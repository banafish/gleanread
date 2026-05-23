import { useEffect, useState } from "react";
import { useAuth } from "@/app/providers/AuthProvider";
import { hasPendingLocalChanges, runSyncOnce, subscribeToPendingLocalChanges, subscribeToRemoteChanges } from "@/supabase/sync";

export interface SyncEngineState {
  status: "idle" | "syncing" | "offline" | "error";
  message: string;
}

const DIRTY_SYNC_DELAY_MS = 2_000;
const FOLLOW_UP_SYNC_DELAY_MS = 1_000;
const FALLBACK_SYNC_INTERVAL_MS = 5 * 60_000;
const RETRY_SYNC_DELAY_MS = 5_000;
const MAX_RETRY_SYNC_DELAY_MS = 5 * 60_000;

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
    let retryDelay = RETRY_SYNC_DELAY_MS;

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
        if (report.failed > 0) {
          setState({ status: "error", message: report.message });
          void scheduleRetry();
        } else {
          retryDelay = RETRY_SYNC_DELAY_MS;
          setState({ status: "idle", message: report.message });
        }
        await refreshWorkspace();
      } catch (error) {
        if (alive) {
          setState({ status: "error", message: error instanceof Error ? error.message : "同步失败" });
          void scheduleRetry();
        }
      } finally {
        running = false;
        if (alive && pendingRun) {
          const nextRun = pendingRun;
          pendingRun = null;
          scheduleRun(nextRun, FOLLOW_UP_SYNC_DELAY_MS);
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

    const scheduleRetry = async () => {
      if (!alive || !navigator.onLine) {
        return;
      }
      if (!(await hasPendingLocalChanges(session.userId))) {
        return;
      }
      scheduleRun({ pullRemote: false }, retryDelay);
      retryDelay = Math.min(retryDelay * 2, MAX_RETRY_SYNC_DELAY_MS);
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
    const interval = window.setInterval(() => scheduleRun({ pullRemote: false }), FALLBACK_SYNC_INTERVAL_MS);
    const handleOnline = () => scheduleRun({ pullRemote: true });
    const handleOffline = () => setState({ status: "offline", message: "离线可用" });
    const flushPendingChanges = () => {
      void hasPendingLocalChanges(session.userId).then((hasPending) => {
        if (alive && hasPending) {
          scheduleRun({ pullRemote: false });
        }
      });
    };
    const handleVisibilityChange = () => {
      if (document.visibilityState === "hidden") {
        flushPendingChanges();
      }
    };
    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    window.addEventListener("pagehide", flushPendingChanges);
    document.addEventListener("visibilitychange", handleVisibilityChange);
    const unsubscribePendingChanges = subscribeToPendingLocalChanges(session.userId, () =>
      scheduleRun({ pullRemote: false }, DIRTY_SYNC_DELAY_MS)
    );
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
      window.removeEventListener("pagehide", flushPendingChanges);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      unsubscribePendingChanges();
      unsubscribeRealtime();
    };
  }, [refreshWorkspace, session]);

  return state;
}
