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

    const run = async () => {
      if (!navigator.onLine) {
        if (alive) {
          setState({ status: "offline", message: "离线可用" });
        }
        return;
      }
      if (alive) {
        setState({ status: "syncing", message: "同步中" });
      }
      try {
        const report = await runSyncOnce();
        if (!alive) {
          return;
        }
        setState({ status: "idle", message: report.message });
        await refreshWorkspace();
      } catch (error) {
        if (alive) {
          setState({ status: "error", message: error instanceof Error ? error.message : "同步失败" });
        }
      }
    };

    void run();
    const interval = window.setInterval(() => void run(), 60_000);
    const handleOnline = () => void run();
    const handleOffline = () => setState({ status: "offline", message: "离线可用" });
    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    const unsubscribeRealtime = subscribeToRemoteChanges(() => void run());

    return () => {
      alive = false;
      window.clearInterval(interval);
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
      unsubscribeRealtime();
    };
  }, [refreshWorkspace, session]);

  return state;
}
