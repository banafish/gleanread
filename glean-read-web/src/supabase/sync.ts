import { liveQuery } from "dexie";
import { db } from "@/db/dexie";
import { getCurrentSession } from "@/db/repositories/workspaceRepository";
import { hasSupabaseConfig, supabase } from "@/supabase/client";
import { advancePullCursor, isSelfEcho, shouldApplyRemoteChange, toNonSelfDeviceFilter } from "@/supabase/syncPolicy";
import type {
  AuthSession,
  Excerpt,
  ExcerptTag,
  KnowledgeTreeNode,
  RemoteTableName,
  SyncCursor,
  Tag,
} from "@/shared/models";
import { createId, now } from "@/shared/utils";

export interface SyncReport {
  pushed: number;
  pulled: number;
  conflicted: number;
  failed: number;
  skipped: boolean;
  message: string;
}

interface RemoteKnowledgeTreeNode {
  id: string;
  user_id: string;
  parent_id: string | null;
  node_title: string;
  outline_markdown: string | null;
  create_time: number;
  update_time: number;
  is_deleted: boolean;
  device_id: string | null;
  sort_order: number;
}

interface RemoteExcerpt {
  id: string;
  user_id: string;
  content: string;
  url: string | null;
  source_title: string | null;
  user_thought: string | null;
  tree_node_id: string | null;
  create_time: number;
  update_time: number;
  is_deleted: boolean;
  device_id: string | null;
}

interface RemoteTag {
  id: string;
  user_id: string;
  tag_name: string;
  color_icon: string | null;
  heat_weight: number;
  create_time: number;
  update_time: number;
  is_deleted: boolean;
  device_id: string | null;
}

interface RemoteExcerptTag {
  id: string;
  user_id: string;
  excerpt_id: string;
  tag_id: string;
  create_time: number;
  update_time: number;
  is_deleted: boolean;
  device_id: string | null;
}

interface Bridge {
  tableName: RemoteTableName;
  rpcName: string;
  localTable: any;
  toRemote: (item: any) => any;
  fromRemote: (item: any) => any;
}

interface SyncOptions {
  pullRemote?: boolean;
}

interface RealtimeWorkspaceRow {
  id?: string;
  user_id?: string;
  update_time?: number;
  device_id?: string | null;
  [key: string]: unknown;
}

interface PushBridgeReport {
  pushed: number;
  conflicted: number;
  failed: number;
}

interface ConditionalPushResult {
  id: string;
  status: "applied" | "conflict" | "forbidden" | "error";
  remote_update_time?: number | null;
  error?: string | null;
}

const PUSH_BATCH_SIZE = 50;
const DEVICE_ID_PATTERN = /^[A-Za-z0-9_-]+$/;
const rpcFallbackTables = new Set<RemoteTableName>();

function getDeviceId(): string {
  const key = "glean-read-web-device-id";
  const existing = window.localStorage.getItem(key);
  if (existing && DEVICE_ID_PATTERN.test(existing)) {
    return existing;
  }
  const next = createId("device");
  window.localStorage.setItem(key, next);
  return next;
}

function toSyncCursor(tableName: RemoteTableName, userId: string, lastPulledAt: number): SyncCursor {
  return {
    id: `${userId}:${tableName}`,
    userId,
    tableName,
    lastPulledAt,
    updateTime: now(),
  };
}

async function loadSyncCursor(tableName: RemoteTableName, userId: string): Promise<SyncCursor> {
  const id = `${userId}:${tableName}`;
  const existing = await db.syncCursors.get(id);
  if (existing) {
    return existing;
  }
  const cursor = toSyncCursor(tableName, userId, 0);
  await db.syncCursors.put(cursor);
  return cursor;
}

async function saveSyncCursor(cursor: SyncCursor): Promise<void> {
  await db.syncCursors.put(cursor);
}

async function resolveSyncSession(sessionOverride?: AuthSession | null): Promise<AuthSession | SyncReport> {
  const session = sessionOverride === undefined ? await getCurrentSession() : sessionOverride;
  if (!session) {
    return { pushed: 0, pulled: 0, conflicted: 0, failed: 0, skipped: true, message: "未登录，已跳过云同步。" };
  }
  if (!hasSupabaseConfig || !supabase || session.provider !== "supabase") {
    return {
      pushed: 0,
      pulled: 0,
      conflicted: 0,
      failed: 0,
      skipped: true,
      message: "当前运行在本地优先模式。",
    };
  }

  const { data, error } = await supabase.auth.getSession();
  if (error) {
    throw new Error(`读取 Supabase 会话失败：${error.message}`);
  }
  if (!data.session?.user) {
    return {
      pushed: 0,
      pulled: 0,
      conflicted: 0,
      failed: 0,
      skipped: true,
      message: "Supabase 会话已过期，请重新登录。",
    };
  }
  if (data.session.user.id !== session.userId) {
    return {
      pushed: 0,
      pulled: 0,
      conflicted: 0,
      failed: 0,
      skipped: true,
      message: "Supabase 会话已切换，已跳过本次同步。",
    };
  }
  return session;
}

function isPendingLocalRow(item: any): boolean {
  return item.syncStatus !== "synced" || item.localDirtyTime !== null;
}

function chunkRows<T>(rows: T[], size: number): T[][] {
  const chunks: T[][] = [];
  for (let index = 0; index < rows.length; index += size) {
    chunks.push(rows.slice(index, index + size));
  }
  return chunks;
}

function isMissingRpcError(error: { code?: string; message?: string }): boolean {
  return error.code === "PGRST202" || /could not find.*function|function .* not found/i.test(error.message ?? "");
}

function isDuplicateIdError(error: { code?: string; message?: string }): boolean {
  const message = error.message ?? "";
  return (error.code === "23505" || /duplicate key/i.test(message)) && /\(id\)/i.test(message);
}

async function fetchRemoteBridgeRow(bridge: Bridge, userId: string, id: string): Promise<RealtimeWorkspaceRow | null> {
  const { data, error } = await supabase!
    .from(bridge.tableName)
    .select("*")
    .eq("user_id", userId)
    .eq("id", id)
    .maybeSingle();
  if (error) {
    throw new Error(error.message);
  }
  return (data as RealtimeWorkspaceRow | null) ?? null;
}

async function markRowSynced(bridge: Bridge, row: any, deviceId: string): Promise<void> {
  await bridge.localTable.put({
    ...row,
    deviceId,
    syncStatus: "synced",
    syncError: null,
    retryCount: 0,
    localDirtyTime: null,
    lastSyncTime: now(),
  });
}

async function markRowFailed(bridge: Bridge, row: any, message: string): Promise<void> {
  await bridge.localTable.put({
    ...row,
    syncStatus: "failed",
    syncError: message,
    retryCount: (row.retryCount ?? 0) + 1,
  });
}

async function applyRemoteConflict(bridge: Bridge, row: any, userId: string): Promise<void> {
  const remoteRow = await fetchRemoteBridgeRow(bridge, userId, row.id);
  if (remoteRow) {
    const localRow = bridge.fromRemote(remoteRow);
    if (shouldApplyRemoteChange(row, { updateTime: localRow.updateTime, deviceId: localRow.deviceId })) {
      await bridge.localTable.put({
        ...localRow,
        syncStatus: "synced",
        syncError: null,
        retryCount: 0,
        localDirtyTime: null,
        lastSyncTime: now(),
      });
      return;
    }
  }
  await bridge.localTable.put({
    ...row,
    syncStatus: "conflict",
    syncError: "远端版本更新，已停止覆盖上行。",
    retryCount: (row.retryCount ?? 0) + 1,
  });
}

async function pushBatchViaRpc(bridge: Bridge, rows: any[], deviceId: string): Promise<ConditionalPushResult[]> {
  const client = supabase!;
  const remoteRows = rows.map((row) => ({
    ...bridge.toRemote(row),
    device_id: deviceId,
  }));
  const { data, error } = await client.rpc(bridge.rpcName, { p_rows: remoteRows });
  if (error) {
    if (isMissingRpcError(error)) {
      rpcFallbackTables.add(bridge.tableName);
      return pushBatchViaRest(bridge, rows, deviceId);
    }
    throw new Error(error.message);
  }
  return Array.isArray(data) ? (data as ConditionalPushResult[]) : [];
}

async function pushRowViaRest(bridge: Bridge, row: any, deviceId: string, retryDuplicate = true): Promise<ConditionalPushResult> {
  const client = supabase!;
  const remoteRow = {
    ...bridge.toRemote(row),
    device_id: deviceId,
  };
  const { data: updatedRows, error: updateError } = await client
    .from(bridge.tableName)
    .update(remoteRow)
    .eq("id", row.id)
    .eq("user_id", row.userId)
    .lte("update_time", row.updateTime)
    .select("id");
  if (updateError) {
    throw new Error(updateError.message);
  }
  if ((updatedRows as Array<{ id: string }> | null)?.length) {
    return { id: row.id, status: "applied" };
  }

  const { data: existing, error: selectError } = await client
    .from(bridge.tableName)
    .select("id, update_time")
    .eq("id", row.id)
    .eq("user_id", row.userId)
    .maybeSingle();
  if (selectError) {
    throw new Error(selectError.message);
  }
  if (existing) {
    return {
      id: row.id,
      status: "conflict",
      remote_update_time: (existing as { update_time: number }).update_time,
    };
  }

  const { error: insertError } = await client.from(bridge.tableName).insert(remoteRow).select("id").single();
  if (!insertError) {
    return { id: row.id, status: "applied" };
  }
  if (isDuplicateIdError(insertError) && retryDuplicate) {
    return pushRowViaRest(bridge, row, deviceId, false);
  }
  if (isDuplicateIdError(insertError)) {
    return { id: row.id, status: "conflict" };
  }
  throw new Error(insertError.message);
}

async function pushBatchViaRest(bridge: Bridge, rows: any[], deviceId: string): Promise<ConditionalPushResult[]> {
  const results: ConditionalPushResult[] = [];
  for (const row of rows) {
    try {
      results.push(await pushRowViaRest(bridge, row, deviceId));
    } catch (error) {
      results.push({
        id: row.id,
        status: "error",
        error: error instanceof Error ? error.message : "同步失败",
      });
    }
  }
  return results;
}

async function pushBatchConditionally(bridge: Bridge, rows: any[], deviceId: string): Promise<ConditionalPushResult[]> {
  if (rpcFallbackTables.has(bridge.tableName)) {
    return pushBatchViaRest(bridge, rows, deviceId);
  }
  return pushBatchViaRpc(bridge, rows, deviceId);
}

async function pushBridge(bridge: Bridge, userId: string, deviceId: string): Promise<PushBridgeReport> {
  const localRows = await bridge.localTable.where("userId").equals(userId).toArray();
  const pending = (localRows as any[]).filter(isPendingLocalRow);
  let pushed = 0;
  let conflicted = 0;
  let failed = 0;
  for (const batch of chunkRows(pending, PUSH_BATCH_SIZE)) {
    let results: ConditionalPushResult[];
    try {
      results = await pushBatchConditionally(bridge, batch, deviceId);
    } catch (error) {
      results = batch.map((row) => ({
        id: row.id,
        status: "error",
        error: error instanceof Error ? error.message : "同步失败",
      }));
    }
    const resultsById = new Map(results.map((result) => [result.id, result]));
    for (const row of batch) {
      const result = resultsById.get(row.id) ?? {
        id: row.id,
        status: "error" as const,
        error: "同步结果缺失",
      };
      if (result.status === "applied") {
        pushed += 1;
        await markRowSynced(bridge, row, deviceId);
        continue;
      }
      if (result.status === "conflict") {
        conflicted += 1;
        await applyRemoteConflict(bridge, row, userId);
        continue;
      }
      failed += 1;
      await markRowFailed(bridge, row, result.error ?? "同步失败");
    }
  }
  return { pushed, conflicted, failed };
}

async function pullBridge(
  bridge: Bridge,
  userId: string,
  deviceId: string
): Promise<number> {
  const client = supabase!;
  const cursor = await loadSyncCursor(bridge.tableName, userId);
  const { data, error } = await client
    .from(bridge.tableName)
    .select("*")
    .eq("user_id", userId)
    .gt("update_time", cursor.lastPulledAt)
    .or(toNonSelfDeviceFilter(deviceId))
    .order("update_time", { ascending: true });
  if (error) {
    throw new Error(error.message);
  }
  let pulled = 0;
  let nextCursor = cursor.lastPulledAt;
  for (const remoteRow of (data ?? []) as Array<{ update_time: number; device_id: string | null }>) {
    nextCursor = advancePullCursor(nextCursor, remoteRow.update_time);
    if (isSelfEcho(remoteRow.device_id, deviceId)) {
      continue;
    }
    const localRow = bridge.fromRemote(remoteRow);
    const existing = await bridge.localTable.get(localRow.id);
    if (shouldApplyRemoteChange(existing, { updateTime: localRow.updateTime, deviceId: localRow.deviceId })) {
      await bridge.localTable.put({
        ...localRow,
        syncStatus: "synced",
        syncError: null,
        retryCount: 0,
        localDirtyTime: null,
        lastSyncTime: now(),
      });
      pulled += 1;
    }
  }
  if (nextCursor !== cursor.lastPulledAt) {
    await saveSyncCursor({ ...cursor, lastPulledAt: nextCursor, updateTime: now() });
  }
  return pulled;
}

async function applyRemoteBridgeRow(
  bridge: Bridge,
  remoteRow: RealtimeWorkspaceRow,
  userId: string,
  deviceId: string
): Promise<boolean> {
  if (
    remoteRow.user_id !== userId ||
    typeof remoteRow.id !== "string" ||
    typeof remoteRow.update_time !== "number" ||
    isSelfEcho(remoteRow.device_id ?? null, deviceId)
  ) {
    return false;
  }

  const localRow = bridge.fromRemote(remoteRow);
  const existing = await bridge.localTable.get(localRow.id);
  if (!shouldApplyRemoteChange(existing, { updateTime: localRow.updateTime, deviceId: localRow.deviceId })) {
    return false;
  }

  await bridge.localTable.put({
    ...localRow,
    syncStatus: "synced",
    syncError: null,
    retryCount: 0,
    localDirtyTime: null,
    lastSyncTime: now(),
  });
  return true;
}

const bridges: Bridge[] = [
  {
    tableName: "knowledge_tree_node",
    rpcName: "sync_knowledge_tree_node_conditional",
    localTable: db.nodes,
    toRemote: (item: KnowledgeTreeNode): RemoteKnowledgeTreeNode => ({
      id: item.id,
      user_id: item.userId,
      parent_id: item.parentId,
      node_title: item.nodeTitle,
      outline_markdown: item.outlineMarkdown || null,
      create_time: item.createTime,
      update_time: item.updateTime,
      is_deleted: item.isDeleted,
      device_id: item.deviceId,
      sort_order: item.sortOrder,
    }),
    fromRemote: (item: RemoteKnowledgeTreeNode): KnowledgeTreeNode => ({
      id: item.id,
      userId: item.user_id,
      parentId: item.parent_id,
      nodeTitle: item.node_title,
      outlineMarkdown: item.outline_markdown ?? "",
      createTime: item.create_time,
      updateTime: item.update_time,
      isDeleted: item.is_deleted,
      deviceId: item.device_id,
      sortOrder: item.sort_order,
      syncStatus: "synced",
      syncError: null,
      retryCount: 0,
      localDirtyTime: null,
      lastSyncTime: item.update_time,
    }),
  },
  {
    tableName: "excerpts",
    rpcName: "sync_excerpts_conditional",
    localTable: db.excerpts,
    toRemote: (item: Excerpt): RemoteExcerpt => ({
      id: item.id,
      user_id: item.userId,
      content: item.content,
      url: item.url,
      source_title: item.sourceTitle,
      user_thought: item.userThought,
      tree_node_id: item.treeNodeId,
      create_time: item.createTime,
      update_time: item.updateTime,
      is_deleted: item.isDeleted,
      device_id: item.deviceId,
    }),
    fromRemote: (item: RemoteExcerpt): Excerpt => ({
      id: item.id,
      userId: item.user_id,
      content: item.content,
      url: item.url,
      sourceTitle: item.source_title,
      userThought: item.user_thought,
      treeNodeId: item.tree_node_id,
      createTime: item.create_time,
      updateTime: item.update_time,
      isDeleted: item.is_deleted,
      deviceId: item.device_id,
      syncStatus: "synced",
      syncError: null,
      retryCount: 0,
      localDirtyTime: null,
      lastSyncTime: item.update_time,
    }),
  },
  {
    tableName: "tags",
    rpcName: "sync_tags_conditional",
    localTable: db.tags,
    toRemote: (item: Tag): RemoteTag => ({
      id: item.id,
      user_id: item.userId,
      tag_name: item.tagName,
      color_icon: item.colorIcon,
      heat_weight: item.heatWeight,
      create_time: item.createTime,
      update_time: item.updateTime,
      is_deleted: item.isDeleted,
      device_id: item.deviceId,
    }),
    fromRemote: (item: RemoteTag): Tag => ({
      id: item.id,
      userId: item.user_id,
      tagName: item.tag_name,
      colorIcon: item.color_icon,
      heatWeight: item.heat_weight,
      createTime: item.create_time,
      updateTime: item.update_time,
      isDeleted: item.is_deleted,
      deviceId: item.device_id,
      syncStatus: "synced",
      syncError: null,
      retryCount: 0,
      localDirtyTime: null,
      lastSyncTime: item.update_time,
    }),
  },
  {
    tableName: "excerpt_tags",
    rpcName: "sync_excerpt_tags_conditional",
    localTable: db.excerptTags,
    toRemote: (item: ExcerptTag): RemoteExcerptTag => ({
      id: item.id,
      user_id: item.userId,
      excerpt_id: item.excerptId,
      tag_id: item.tagId,
      create_time: item.createTime,
      update_time: item.updateTime,
      is_deleted: item.isDeleted,
      device_id: item.deviceId,
    }),
    fromRemote: (item: RemoteExcerptTag): ExcerptTag => ({
      id: item.id,
      userId: item.user_id,
      excerptId: item.excerpt_id,
      tagId: item.tag_id,
      createTime: item.create_time,
      updateTime: item.update_time,
      isDeleted: item.is_deleted,
      deviceId: item.device_id,
      syncStatus: "synced",
      syncError: null,
      retryCount: 0,
      localDirtyTime: null,
      lastSyncTime: item.update_time,
    }),
  },
];

export async function runSyncOnce(sessionOverride?: AuthSession | null, options: SyncOptions = {}): Promise<SyncReport> {
  const session = await resolveSyncSession(sessionOverride);
  if ("skipped" in session) {
    return session;
  }

  const pullRemote = options.pullRemote ?? true;
  const deviceId = getDeviceId();
  let pushed = 0;
  let pulled = 0;
  let conflicted = 0;
  let failed = 0;
  for (const bridge of bridges) {
    const report = await pushBridge(bridge, session.userId, deviceId);
    pushed += report.pushed;
    conflicted += report.conflicted;
    failed += report.failed;
  }
  if (pullRemote) {
    for (const bridge of bridges) {
      pulled += await pullBridge(bridge, session.userId, deviceId);
    }
  }
  const conflictText = conflicted > 0 ? `，冲突 ${conflicted} 条` : "";
  const failedText = failed > 0 ? `，失败 ${failed} 条` : "";
  return {
    pushed,
    pulled,
    conflicted,
    failed,
    skipped: false,
    message: pullRemote
      ? `同步完成：上行 ${pushed} 条，下行 ${pulled} 条${conflictText}${failedText}。`
      : `同步完成：上行 ${pushed} 条${conflictText}${failedText}。`,
  };
}

async function getPendingLocalChangeSignature(userId: string): Promise<string> {
  const tableSignatures = await Promise.all(
    bridges.map(async (bridge) => {
      const rows = ((await bridge.localTable.where("userId").equals(userId).toArray()) as any[]).filter(isPendingLocalRow);
      return rows
        .map((row) => `${bridge.tableName}:${row.id}:${row.localDirtyTime ?? row.updateTime}`)
        .sort()
        .join(",");
    })
  );
  return tableSignatures.filter(Boolean).join("|");
}

export async function hasPendingLocalChanges(userId: string): Promise<boolean> {
  return (await getPendingLocalChangeSignature(userId)).length > 0;
}

export function subscribeToPendingLocalChanges(userId: string, onPendingChange: () => void): () => void {
  let lastSignature = "";
  const subscription = liveQuery(() => getPendingLocalChangeSignature(userId)).subscribe({
    next: (signature) => {
      if (signature && signature !== lastSignature) {
        onPendingChange();
      }
      lastSignature = signature;
    },
    error: () => undefined,
  });
  return () => subscription.unsubscribe();
}

export function subscribeToRemoteChanges(userId: string, onRemoteChange: () => void): () => void {
  if (!hasSupabaseConfig || !supabase) {
    return () => undefined;
  }
  const client = supabase;
  const deviceId = getDeviceId();
  const channels = bridges.map((bridge) =>
    client
      .channel(`glean-read:${bridge.tableName}:${userId}`)
      .on(
        "postgres_changes",
        {
          event: "*",
          schema: "public",
          table: bridge.tableName,
          filter: `user_id=eq.${userId}`,
        },
        (payload) => {
          const row = (payload.new && Object.keys(payload.new).length > 0 ? payload.new : payload.old) as RealtimeWorkspaceRow | null;
          if (!row) {
            return;
          }
          void applyRemoteBridgeRow(bridge, row, userId, deviceId).then((applied) => {
            if (applied) {
              onRemoteChange();
            }
          }).catch(() => undefined);
        }
      )
      .subscribe()
  );

  return () => {
    for (const channel of channels) {
      void client.removeChannel(channel);
    }
  };
}
