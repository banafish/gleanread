import { useEffect, useState } from "react";
import { useAuth } from "@/app/providers/AuthProvider";
import { runSyncOnce, subscribeToRemoteChanges } from "@/supabase/sync";

export interface SyncEngineState {
  status: "idle" | "syncing" | "offline" | "error";
  message: string;
}

export function useSyncEngine(): SyncEngineState {
  const { session, refreshWorkspace } = useAuth();
  const [state, setState] = useState<SyncEngineState>({
    status: navigator.onLine ? "idle" : "offline",
    message: navigator.onLine ? "本地优先" : "离线可用",
  });

  useEffect(() => {
    if (!session) {
      setState({ status: "idle", message: "未登录" });
      return undefined;
    }

    let alive = true;

    let running = false;
    let pendingRun: { pullRemote: boolean } | null = null;
    let runTimer: number | null = null;
    let runTimerMode: { pullRemote: boolean } | null = null;
    let refreshTimer: number | null = null;

    const mergeRunMode = (current: { pullRemote: boolean } | null, next: { pullRemote: boolean }) => ({
      pullRemote: Boolean(current?.pullRemote || next.pullRemote),
    });

    const run = async (mode: { pullRemote: boolean }) => {
      if (!navigator.onLine) {
        if (alive) {
          setState({ status: "offline", message: "离线可用" });
        }
        return;
      }
      if (running) {
        pendingRun = mergeRunMode(pendingRun, mode);
        return;
      }
      running = true;
      pendingRun = null;
      if (alive) {
        setState({ status: "syncing", message: "同步中" });
      }
      try {
        const report = await runSyncOnce(session, { pullRemote: mode.pullRemote });
        if (!alive) {
          return;
        }
        setState({ status: "idle", message: report.message });
        await refreshWorkspace();
      } catch (error) {
        if (alive) {
          setState({ status: "error", message: error instanceof Error ? error.message : "同步失败" });
        }
      } finally {
        running = false;
        if (alive && pendingRun) {
          const nextRun = pendingRun;
          pendingRun = null;
          scheduleRun(nextRun, 1_000);
        }
      }
    };

    const scheduleRun = (mode: { pullRemote: boolean }, delay = 0) => {
      runTimerMode = mergeRunMode(runTimerMode, mode);
      if (runTimer !== null) {
        window.clearTimeout(runTimer);
      }
      runTimer = window.setTimeout(() => {
        const nextRun = runTimerMode ?? mode;
        runTimerMode = null;
        runTimer = null;
        void run(nextRun);
      }, delay);
    };

    const scheduleRefresh = (delay = 250) => {
      if (refreshTimer !== null) {
        window.clearTimeout(refreshTimer);
      }
      refreshTimer = window.setTimeout(() => {
        refreshTimer = null;
        void (async () => {
          try {
            await refreshWorkspace();
            if (alive) {
              setState((current) => (current.status === "syncing" ? current : { status: "idle", message: "远端变更已更新" }));
            }
          } catch (error) {
            if (alive) {
              setState({ status: "error", message: error instanceof Error ? error.message : "同步失败" });
            }
          }
        })();
      }, delay);
    };

    scheduleRun({ pullRemote: true });
    const interval = window.setInterval(() => scheduleRun({ pullRemote: false }), 60_000);
    const handleOnline = () => scheduleRun({ pullRemote: true });
    const handleOffline = () => setState({ status: "offline", message: "离线可用" });
    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    const unsubscribeRealtime = subscribeToRemoteChanges(session.userId, () => scheduleRefresh());

    return () => {
      alive = false;
      if (runTimer !== null) {
        window.clearTimeout(runTimer);
      }
      if (refreshTimer !== null) {
        window.clearTimeout(refreshTimer);
      }
      window.clearInterval(interval);
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
      unsubscribeRealtime();
    };
  }, [refreshWorkspace, session]);

  return state;
}
