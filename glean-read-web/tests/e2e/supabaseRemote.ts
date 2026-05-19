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

export async function upsertRemoteNode(client: SupabaseClient, node: RemoteKnowledgeTreeNode): Promise<void> {
  const { error } = await client.from("knowledge_tree_node").upsert(node, { onConflict: "id" });
  if (error) {
    throw new Error(`写入 Supabase 节点失败：${error.message}`);
  }
}

export async function softDeleteRemoteNode(client: SupabaseClient, id: string): Promise<void> {
  const { error } = await client
    .from("knowledge_tree_node")
    .update({
      is_deleted: true,
      update_time: Date.now(),
      device_id: "e2e-cleanup",
    })
    .eq("id", id);
  if (error) {
    throw new Error(`清理 Supabase 节点失败：${error.message}`);
  }
}
