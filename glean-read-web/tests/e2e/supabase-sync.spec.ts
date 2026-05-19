import { expect, test, type Page } from "@playwright/test";
import { cleanupWorkspace, loginWithPassword, seededUser, testPrefix, waitForWorkbench } from "./fixtures";
import {
  buildRemoteNode,
  createAuthenticatedSupabaseClient,
  fetchRemoteNode,
  softDeleteRemoteNode,
  upsertRemoteNode,
  type RemoteClientSession,
  type RemoteKnowledgeTreeNode,
} from "./supabaseRemote";

interface LocalKnowledgeTreeNode {
  id: string;
  userId: string;
  parentId: string | null;
  nodeTitle: string;
  outlineMarkdown: string;
  createTime: number;
  updateTime: number;
  isDeleted: boolean;
  deviceId: string | null;
  sortOrder: number;
  syncStatus: string;
  syncError: string | null;
  localDirtyTime: number | null;
}

interface LocalSession {
  id: string;
  userId: string;
  email: string;
  provider: "supabase";
}

test.describe.configure({ mode: "serial" });

async function getCurrentLocalSession(page: Page): Promise<LocalSession | null> {
  return page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("glean-read-web");
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
    });
    try {
      return await new Promise<LocalSession | null>((resolve, reject) => {
        const transaction = db.transaction(["sessions"], "readonly");
        const request = transaction.objectStore("sessions").get("current");
        request.onerror = () => reject(request.error);
        request.onsuccess = () => resolve((request.result as LocalSession | undefined) ?? null);
      });
    } finally {
      db.close();
    }
  });
}

async function getLocalNode(page: Page, id: string): Promise<LocalKnowledgeTreeNode | null> {
  return page.evaluate(async (nodeId) => {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("glean-read-web");
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
    });
    try {
      return await new Promise<LocalKnowledgeTreeNode | null>((resolve, reject) => {
        const transaction = db.transaction(["nodes"], "readonly");
        const request = transaction.objectStore("nodes").get(nodeId);
        request.onerror = () => reject(request.error);
        request.onsuccess = () => resolve((request.result as LocalKnowledgeTreeNode | undefined) ?? null);
      });
    } finally {
      db.close();
    }
  }, id);
}

async function insertStaleSupabaseSessionAndPendingNode(page: Page, prefix: string, staleUserId: string): Promise<string> {
  return page.evaluate(
    async ({ seedPrefix, staleUserId }) => {
      const db = await new Promise<IDBDatabase>((resolve, reject) => {
        const request = indexedDB.open("glean-read-web");
        request.onerror = () => reject(request.error);
        request.onsuccess = () => resolve(request.result);
      });
      try {
        const timestamp = Date.now();
        const nodeId = `${seedPrefix}-stale-session-node`;
        await new Promise<void>((resolve, reject) => {
          const transaction = db.transaction(["sessions", "nodes"], "readwrite");
          transaction.onerror = () => reject(transaction.error);
          transaction.oncomplete = () => resolve();
          transaction.objectStore("sessions").put({
            id: "current",
            userId: staleUserId,
            email: "stale@example.com",
            provider: "supabase",
          });
          transaction.objectStore("nodes").put({
            id: nodeId,
            userId: staleUserId,
            parentId: null,
            nodeTitle: `${seedPrefix} stale session node`,
            outlineMarkdown: "",
            createTime: timestamp,
            updateTime: timestamp,
            isDeleted: false,
            deviceId: "e2e-stale-session",
            sortOrder: 1_114_112,
            syncStatus: "pending",
            syncError: null,
            retryCount: 0,
            localDirtyTime: timestamp,
            lastSyncTime: null,
          });
        });
        return nodeId;
      } finally {
        db.close();
      }
    },
    { seedPrefix: prefix, staleUserId }
  );
}

async function insertPendingLocalNode(page: Page, prefix: string): Promise<LocalKnowledgeTreeNode> {
  return page.evaluate(async (seedPrefix) => {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("glean-read-web");
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
    });
    try {
      const session = await new Promise<{ userId: string }>((resolve, reject) => {
        const transaction = db.transaction(["sessions"], "readonly");
        const request = transaction.objectStore("sessions").get("current");
        request.onerror = () => reject(request.error);
        request.onsuccess = () => {
          if (!request.result) {
            reject(new Error("Missing current session"));
            return;
          }
          resolve(request.result as { userId: string });
        };
      });
      const deviceKey = "glean-read-web-device-id";
      let deviceId = window.localStorage.getItem(deviceKey);
      if (!deviceId) {
        deviceId = `e2e-device-${Date.now()}`;
        window.localStorage.setItem(deviceKey, deviceId);
      }
      const timestamp = Date.now();
      const node: LocalKnowledgeTreeNode = {
        id: `${seedPrefix}-local-sync-node`,
        userId: session.userId,
        parentId: null,
        nodeTitle: `${seedPrefix} local sync node`,
        outlineMarkdown: `${seedPrefix} local outline`,
        createTime: timestamp,
        updateTime: timestamp,
        isDeleted: false,
        deviceId,
        sortOrder: 1_114_112,
        syncStatus: "pending",
        syncError: null,
        localDirtyTime: timestamp,
      };
      await new Promise<void>((resolve, reject) => {
        const transaction = db.transaction(["nodes"], "readwrite");
        transaction.onerror = () => reject(transaction.error);
        transaction.oncomplete = () => resolve();
        transaction.objectStore("nodes").put(node);
      });
      return node;
    } finally {
      db.close();
    }
  }, prefix);
}

function expectLocalNodeMatchesRemote(local: LocalKnowledgeTreeNode, remote: RemoteKnowledgeTreeNode) {
  expect(remote.id).toBe(local.id);
  expect(remote.user_id).toBe(local.userId);
  expect(remote.parent_id).toBe(local.parentId);
  expect(remote.node_title).toBe(local.nodeTitle);
  expect(remote.outline_markdown ?? "").toBe(local.outlineMarkdown);
  expect(remote.create_time).toBe(local.createTime);
  expect(remote.update_time).toBe(local.updateTime);
  expect(remote.is_deleted).toBe(local.isDeleted);
  expect(remote.device_id).toBe(local.deviceId);
  expect(remote.sort_order).toBe(local.sortOrder);
}

test("E2E-36 Supabase 会话恢复会覆盖 IndexedDB 中的旧 current 用户", async ({ page }) => {
  test.setTimeout(75_000);
  const prefix = testPrefix("supabase-stale-session");
  const staleUserId = "11111111-1111-4111-8111-111111111111";
  let remote: RemoteClientSession | null = null;
  const failedKnowledgeUpserts: number[] = [];

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    await loginWithPassword(page, seededUser);
    await insertStaleSupabaseSessionAndPendingNode(page, prefix, staleUserId);
    page.on("response", (response) => {
      if (
        response.url().includes("/rest/v1/knowledge_tree_node") &&
        response.url().includes("on_conflict=id") &&
        response.status() >= 400
      ) {
        failedKnowledgeUpserts.push(response.status());
      }
    });

    await page.reload();
    await waitForWorkbench(page);

    await expect.poll(async () => (await getCurrentLocalSession(page))?.userId ?? null, { timeout: 15_000 }).toBe(remote.userId);
    await page.waitForTimeout(1_000);
    expect(failedKnowledgeUpserts).toEqual([]);
  } finally {
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-37 本地节点变更会同步到 Supabase 且远端字段一致", async ({ page }) => {
  test.setTimeout(75_000);
  const prefix = testPrefix("supabase-push");
  let remote: RemoteClientSession | null = null;
  let nodeId = "";

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    await loginWithPassword(page, seededUser);
    const localBefore = await insertPendingLocalNode(page, prefix);
    nodeId = localBefore.id;

    await page.reload();
    await waitForWorkbench(page);
    await expect.poll(async () => Boolean(await fetchRemoteNode(remote!.client, nodeId)), { timeout: 30_000 }).toBe(true);

    const remoteNode = await fetchRemoteNode(remote.client, nodeId);
    const localAfter = await getLocalNode(page, nodeId);
    expect(remoteNode).not.toBeNull();
    expect(localAfter).not.toBeNull();
    expect(localAfter!.syncStatus).toBe("synced");
    expect(localAfter!.syncError).toBeNull();
    expect(localAfter!.localDirtyTime).toBeNull();
    expectLocalNodeMatchesRemote(localAfter!, remoteNode!);
  } finally {
    if (remote && nodeId) {
      await softDeleteRemoteNode(remote.client, nodeId).catch(() => undefined);
    }
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-39 本地同步不会在短时间内重复刷 Supabase 请求", async ({ page }) => {
  test.setTimeout(75_000);
  const prefix = testPrefix("supabase-request-storm");
  let remote: RemoteClientSession | null = null;
  let nodeId = "";
  const workspaceRequests: string[] = [];
  const failedWorkspaceRequests: string[] = [];
  const workspaceRestPath = /\/rest\/v1\/(knowledge_tree_node|excerpts|tags|excerpt_tags)/;

  page.on("request", (request) => {
    if (workspaceRestPath.test(request.url())) {
      workspaceRequests.push(request.url());
    }
  });
  page.on("response", (response) => {
    if (workspaceRestPath.test(response.url()) && response.status() >= 400) {
      failedWorkspaceRequests.push(`${response.status()} ${response.url()}`);
    }
  });
  page.on("requestfailed", (request) => {
    if (workspaceRestPath.test(request.url())) {
      const errorText = request.failure()?.errorText ?? "";
      if (!errorText.includes("ERR_ABORTED")) {
        failedWorkspaceRequests.push(`${errorText} ${request.url()}`);
      }
    }
  });

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    await loginWithPassword(page, seededUser);
    workspaceRequests.length = 0;
    failedWorkspaceRequests.length = 0;
    const localBefore = await insertPendingLocalNode(page, prefix);
    nodeId = localBefore.id;

    await page.reload();
    await waitForWorkbench(page);
    await expect.poll(async () => Boolean(await fetchRemoteNode(remote!.client, nodeId)), { timeout: 30_000 }).toBe(true);
    await page.waitForTimeout(6_000);

    expect(failedWorkspaceRequests).toEqual([]);
    expect(workspaceRequests.length).toBeLessThan(40);
  } finally {
    if (remote && nodeId) {
      await softDeleteRemoteNode(remote.client, nodeId).catch(() => undefined);
    }
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-38 Supabase Realtime 远端节点变更会拉到本地且字段一致", async ({ page }) => {
  test.setTimeout(75_000);
  const prefix = testPrefix("supabase-realtime");
  let remote: RemoteClientSession | null = null;
  const nodeId = `${prefix}-remote-realtime-node`;

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    await loginWithPassword(page, seededUser);
    await waitForWorkbench(page);

    const remoteNode = buildRemoteNode(remote.userId, nodeId, prefix, "e2e-remote-realtime");
    await upsertRemoteNode(remote.client, remoteNode);

    await expect.poll(async () => (await getLocalNode(page, nodeId))?.nodeTitle ?? null, { timeout: 30_000 }).toBe(remoteNode.node_title);

    const localNode = await getLocalNode(page, nodeId);
    expect(localNode).not.toBeNull();
    expect(localNode!.syncStatus).toBe("synced");
    expectLocalNodeMatchesRemote(localNode!, remoteNode);
  } finally {
    if (remote) {
      await softDeleteRemoteNode(remote.client, nodeId).catch(() => undefined);
    }
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});
