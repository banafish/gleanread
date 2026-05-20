import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
import { createClient, type SupabaseClient } from "@supabase/supabase-js";
import type { TestAccount } from "./fixtures";

export interface RemoteClientSession {
  client: SupabaseClient;
  userId: string;
}

export interface RemoteKnowledgeTreeNode {
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

export interface RemoteExcerpt {
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

export interface RemoteTag {
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

export interface RemoteExcerptTag {
  id: string;
  user_id: string;
  excerpt_id: string;
  tag_id: string;
  create_time: number;
  update_time: number;
  is_deleted: boolean;
  device_id: string | null;
}

export type RemoteTableName = "knowledge_tree_node" | "excerpts" | "tags" | "excerpt_tags";

export interface RemoteWorkspaceCounts {
  knowledge_tree_node: number;
  excerpts: number;
  tags: number;
  excerpt_tags: number;
}

export interface RemoteWorkspaceRows {
  node: RemoteKnowledgeTreeNode;
  excerpt: RemoteExcerpt;
  tag: RemoteTag;
  excerptTag: RemoteExcerptTag;
}

export const remoteWorkspaceTables: RemoteTableName[] = ["knowledge_tree_node", "excerpts", "tags", "excerpt_tags"];

function parseDotEnv(): Record<string, string> {
  const envPath = resolve(process.cwd(), ".env");
  if (!existsSync(envPath)) {
    return {};
  }
  return Object.fromEntries(
    readFileSync(envPath, "utf8")
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith("#") && line.includes("="))
      .map((line) => {
        const index = line.indexOf("=");
        return [line.slice(0, index), line.slice(index + 1).replace(/^["']|["']$/g, "")];
      })
  );
}

function getSupabaseEnv(name: "VITE_SUPABASE_URL" | "VITE_SUPABASE_ANON_KEY"): string {
  const dotEnv = parseDotEnv();
  const value = process.env[name] ?? dotEnv[name];
  if (!value) {
    throw new Error(`缺少 ${name}，无法运行 Supabase 远端一致性测试。`);
  }
  return value;
}

export async function createAuthenticatedSupabaseClient(account: TestAccount): Promise<RemoteClientSession> {
  const client = createClient(getSupabaseEnv("VITE_SUPABASE_URL"), getSupabaseEnv("VITE_SUPABASE_ANON_KEY"), {
    auth: {
      persistSession: false,
      autoRefreshToken: false,
      detectSessionInUrl: false,
    },
  });
  const { data, error } = await client.auth.signInWithPassword({
    email: account.email,
    password: account.password,
  });
  if (error) {
    throw new Error(`Supabase 远端登录失败：${error.message}`);
  }
  if (!data.user) {
    throw new Error("Supabase 远端登录后没有返回 user。");
  }
  return { client, userId: data.user.id };
}

export function buildRemoteNode(userId: string, id: string, prefix: string, deviceId: string): RemoteKnowledgeTreeNode {
  const timestamp = Date.now();
  return {
    id,
    user_id: userId,
    parent_id: null,
    node_title: `${prefix} remote realtime node`,
    outline_markdown: `${prefix} remote outline`,
    create_time: timestamp,
    update_time: timestamp,
    is_deleted: false,
    device_id: deviceId,
    sort_order: 1_048_576,
  };
}

export function buildRemoteWorkspace(userId: string, prefix: string, deviceId: string): RemoteWorkspaceRows {
  const timestamp = Date.now();
  const node: RemoteKnowledgeTreeNode = {
    id: `${prefix}-remote-full-node`,
    user_id: userId,
    parent_id: null,
    node_title: `${prefix} remote full node`,
    outline_markdown: `${prefix} remote full outline`,
    create_time: timestamp,
    update_time: timestamp,
    is_deleted: false,
    device_id: deviceId,
    sort_order: 1_572_864,
  };
  const tag: RemoteTag = {
    id: `${prefix}-remote-full-tag`,
    user_id: userId,
    tag_name: `${prefix}-remote-tag`,
    color_icon: "R",
    heat_weight: 9,
    create_time: timestamp + 1,
    update_time: timestamp + 1,
    is_deleted: false,
    device_id: deviceId,
  };
  const excerpt: RemoteExcerpt = {
    id: `${prefix}-remote-full-excerpt`,
    user_id: userId,
    content: `${prefix} remote full excerpt`,
    url: "https://example.com/remote-full-sync",
    source_title: `${prefix} remote source`,
    user_thought: `${prefix} remote thought`,
    tree_node_id: node.id,
    create_time: timestamp + 2,
    update_time: timestamp + 2,
    is_deleted: false,
    device_id: deviceId,
  };
  const excerptTag: RemoteExcerptTag = {
    id: `${prefix}-remote-full-excerpt-tag`,
    user_id: userId,
    excerpt_id: excerpt.id,
    tag_id: tag.id,
    create_time: timestamp + 3,
    update_time: timestamp + 3,
    is_deleted: false,
    device_id: deviceId,
  };
  return { node, excerpt, tag, excerptTag };
}

export async function fetchRemoteNode(
  client: SupabaseClient,
  id: string
): Promise<RemoteKnowledgeTreeNode | null> {
  const { data, error } = await client.from("knowledge_tree_node").select("*").eq("id", id).maybeSingle();
  if (error) {
    throw new Error(`读取 Supabase 节点失败：${error.message}`);
  }
  return (data as RemoteKnowledgeTreeNode | null) ?? null;
}

export async function fetchRemoteExcerpt(client: SupabaseClient, id: string): Promise<RemoteExcerpt | null> {
  const { data, error } = await client.from("excerpts").select("*").eq("id", id).maybeSingle();
  if (error) {
    throw new Error(`读取 Supabase 摘录失败：${error.message}`);
  }
  return (data as RemoteExcerpt | null) ?? null;
}

export async function fetchRemoteTag(client: SupabaseClient, id: string): Promise<RemoteTag | null> {
  const { data, error } = await client.from("tags").select("*").eq("id", id).maybeSingle();
  if (error) {
    throw new Error(`读取 Supabase 标签失败：${error.message}`);
  }
  return (data as RemoteTag | null) ?? null;
}

export async function fetchRemoteExcerptTag(client: SupabaseClient, id: string): Promise<RemoteExcerptTag | null> {
  const { data, error } = await client.from("excerpt_tags").select("*").eq("id", id).maybeSingle();
  if (error) {
    throw new Error(`读取 Supabase 摘录标签关系失败：${error.message}`);
  }
  return (data as RemoteExcerptTag | null) ?? null;
}

export async function fetchRemoteWorkspaceCounts(client: SupabaseClient, userId: string): Promise<RemoteWorkspaceCounts> {
  const entries = await Promise.all(
    remoteWorkspaceTables.map(async (tableName) => {
      const { count, error } = await client
        .from(tableName)
        .select("id", { count: "exact", head: true })
        .eq("user_id", userId)
        .eq("is_deleted", false);
      if (error) {
        throw new Error(`统计 Supabase ${tableName} 失败：${error.message}`);
      }
      return [tableName, count ?? 0] as const;
    })
  );
  return Object.fromEntries(entries) as RemoteWorkspaceCounts;
}

export function sumRemoteWorkspaceCounts(counts: RemoteWorkspaceCounts): number {
  return remoteWorkspaceTables.reduce((total, tableName) => total + counts[tableName], 0);
}

export async function upsertRemoteNode(client: SupabaseClient, node: RemoteKnowledgeTreeNode): Promise<void> {
  const { error } = await client.from("knowledge_tree_node").upsert(node, { onConflict: "id" });
  if (error) {
    throw new Error(`写入 Supabase 节点失败：${error.message}`);
  }
}

export async function upsertRemoteExcerpt(client: SupabaseClient, excerpt: RemoteExcerpt): Promise<void> {
  const { error } = await client.from("excerpts").upsert(excerpt, { onConflict: "id" });
  if (error) {
    throw new Error(`写入 Supabase 摘录失败：${error.message}`);
  }
}

export async function upsertRemoteTag(client: SupabaseClient, tag: RemoteTag): Promise<void> {
  const { error } = await client.from("tags").upsert(tag, { onConflict: "id" });
  if (error) {
    throw new Error(`写入 Supabase 标签失败：${error.message}`);
  }
}

export async function upsertRemoteExcerptTag(client: SupabaseClient, excerptTag: RemoteExcerptTag): Promise<void> {
  const { error } = await client.from("excerpt_tags").upsert(excerptTag, { onConflict: "id" });
  if (error) {
    throw new Error(`写入 Supabase 摘录标签关系失败：${error.message}`);
  }
}

export async function upsertRemoteWorkspace(client: SupabaseClient, rows: RemoteWorkspaceRows): Promise<void> {
  await upsertRemoteNode(client, rows.node);
  await upsertRemoteTag(client, rows.tag);
  await upsertRemoteExcerpt(client, rows.excerpt);
  await upsertRemoteExcerptTag(client, rows.excerptTag);
}

export async function updateRemoteNode(
  client: SupabaseClient,
  id: string,
  values: Partial<RemoteKnowledgeTreeNode>
): Promise<RemoteKnowledgeTreeNode | null> {
  const { data, error } = await client.from("knowledge_tree_node").update(values).eq("id", id).select("*").maybeSingle();
  if (error) {
    throw new Error(`更新 Supabase 节点失败：${error.message}`);
  }
  return (data as RemoteKnowledgeTreeNode | null) ?? null;
}

export async function cleanupRemoteWorkspace(client: SupabaseClient, rows: RemoteWorkspaceRows): Promise<void> {
  await Promise.all([
    softDeleteRemoteRow(client, "excerpt_tags", rows.excerptTag.id).catch(() => undefined),
    softDeleteRemoteRow(client, "excerpts", rows.excerpt.id).catch(() => undefined),
    softDeleteRemoteRow(client, "tags", rows.tag.id).catch(() => undefined),
    softDeleteRemoteRow(client, "knowledge_tree_node", rows.node.id).catch(() => undefined),
  ]);
}

export async function softDeleteRemoteRow(client: SupabaseClient, tableName: RemoteTableName, id: string): Promise<void> {
  const { error } = await client
    .from(tableName)
    .update({
      is_deleted: true,
      update_time: Date.now(),
      device_id: "e2e-cleanup",
    })
    .eq("id", id);
  if (error) {
    throw new Error(`清理 Supabase ${tableName} 失败：${error.message}`);
  }
}

export async function softDeleteRemoteNode(client: SupabaseClient, id: string): Promise<void> {
  await softDeleteRemoteRow(client, "knowledge_tree_node", id);
}
