import type { SyncStatus } from "@/shared/models";

export interface LocalSyncProbe {
  updateTime: number;
  syncStatus: SyncStatus;
}

export interface RemoteSyncProbe {
  updateTime: number;
  deviceId: string | null;
}

export function isSelfEcho(remoteDeviceId: string | null, currentDeviceId: string): boolean {
  return remoteDeviceId === currentDeviceId;
}

export function shouldApplyRemoteChange(existing: LocalSyncProbe | undefined, remote: RemoteSyncProbe): boolean {
  if (!existing) {
    return true;
  }
  return existing.updateTime <= remote.updateTime || existing.syncStatus === "synced";
}

export function advancePullCursor(currentCursor: number, remoteUpdateTime: number): number {
  return Math.max(currentCursor, remoteUpdateTime);
}
