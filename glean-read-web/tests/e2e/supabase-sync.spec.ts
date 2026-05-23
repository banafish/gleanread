import { expect, test, type Page } from "@playwright/test";
import {
  cleanupWorkspace,
  emptyUser,
  expectNoSeedExampleContent,
  firstRealNode,
  loginWithPassword,
  seededUser,
  testPrefix,
  waitForWorkbench,
} from "./fixtures";
import {
  buildRemoteNode,
  buildRemoteWorkspace,
  cleanupRemoteWorkspace,
  createAuthenticatedSupabaseClient,
  fetchRemoteExcerpt,
  fetchRemoteExcerptTag,
  fetchRemoteNode,
  fetchRemoteTag,
  fetchRemoteWorkspaceCounts,
  softDeleteRemoteNode,
  softDeleteRemoteRow,
  sumRemoteWorkspaceCounts,
  updateRemoteNode,
  upsertRemoteNode,
  upsertRemoteWorkspace,
  type RemoteExcerpt,
  type RemoteExcerptTag,
  type RemoteClientSession,
  type RemoteKnowledgeTreeNode,
  type RemoteWorkspaceRows,
  type RemoteTag,
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
  retryCount: number;
  localDirtyTime: number | null;
  lastSyncTime: number | null;
}

interface LocalExcerpt {
  id: string;
  userId: string;
  content: string;
  url: string | null;
  sourceTitle: string | null;
  userThought: string | null;
  treeNodeId: string | null;
  createTime: number;
  updateTime: number;
  isDeleted: boolean;
  deviceId: string | null;
  syncStatus: string;
  syncError: string | null;
  retryCount: number;
  localDirtyTime: number | null;
  lastSyncTime: number | null;
}

interface LocalTag {
  id: string;
  userId: string;
  tagName: string;
  colorIcon: string | null;
  heatWeight: number;
  createTime: number;
  updateTime: number;
  isDeleted: boolean;
  deviceId: string | null;
  syncStatus: string;
  syncError: string | null;
  retryCount: number;
  localDirtyTime: number | null;
  lastSyncTime: number | null;
}

interface LocalExcerptTag {
  id: string;
  userId: string;
  excerptId: string;
  tagId: string;
  createTime: number;
  updateTime: number;
  isDeleted: boolean;
  deviceId: string | null;
  syncStatus: string;
  syncError: string | null;
  retryCount: number;
  localDirtyTime: number | null;
  lastSyncTime: number | null;
}

interface LocalSession {
  id: string;
  userId: string;
  email: string;
  provider: "supabase";
}

interface LocalWorkspaceCounts {
  nodes: number;
  excerpts: number;
  tags: number;
  excerptTags: number;
  total: number;
}

interface LocalPendingWorkspace {
  node: LocalKnowledgeTreeNode;
  excerpt: LocalExcerpt;
  tag: LocalTag;
  excerptTag: LocalExcerptTag;
}

type LocalStoreName = "nodes" | "excerpts" | "tags" | "excerptTags";
type WorkspaceSyncRpcName =
  | "sync_knowledge_tree_node_conditional"
  | "sync_excerpts_conditional"
  | "sync_tags_conditional"
  | "sync_excerpt_tags_conditional";

interface WorkspaceRpcRequest {
  name: WorkspaceSyncRpcName;
  rowIds: string[];
  rowCount: number;
}

const workspaceRestPath = /\/rest\/v1\/(knowledge_tree_node|excerpts|tags|excerpt_tags)/;
const workspaceRpcPath = /\/rest\/v1\/rpc\/(sync_(knowledge_tree_node|excerpts|tags|excerpt_tags)_conditional)/;

test.describe.configure({ mode: "serial" });

function isWorkspaceRestUrl(url: string): boolean {
  return workspaceRestPath.test(url);
}

function isWorkspaceSyncWriteUrl(url: string): boolean {
  return isWorkspaceRestUrl(url) || workspaceRpcPath.test(url);
}

function getWorkspaceRpcName(url: string): WorkspaceSyncRpcName | null {
  return (workspaceRpcPath.exec(url)?.[1] as WorkspaceSyncRpcName | undefined) ?? null;
}

function isWorkspacePullUrl(url: string): boolean {
  const decodedUrl = decodeURIComponent(url);
  return isWorkspaceRestUrl(decodedUrl) && decodedUrl.includes("select=*") && decodedUrl.includes("update_time=gt.");
}

async function compressSyncInterval(page: Page): Promise<void> {
  await page.addInitScript(() => {
    const originalSetInterval = window.setInterval.bind(window);
    window.setInterval = ((handler: TimerHandler, timeout?: number, ...args: unknown[]) =>
      originalSetInterval(handler, timeout === 60_000 || timeout === 300_000 ? 1_000 : timeout, ...args)) as typeof window.setInterval;
  });
}

async function waitForInitialSync(page: Page): Promise<void> {
  await expect(page.getByText(/同步完成/)).toBeVisible({ timeout: 30_000 });
}

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
  return getLocalRow<LocalKnowledgeTreeNode>(page, "nodes", id);
}

async function getLocalNodeByTitle(page: Page, title: string): Promise<LocalKnowledgeTreeNode | null> {
  return page.evaluate(async (nodeTitle) => {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("glean-read-web");
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
    });
    try {
      return await new Promise<LocalKnowledgeTreeNode | null>((resolve, reject) => {
        const transaction = db.transaction(["nodes"], "readonly");
        const request = transaction.objectStore("nodes").openCursor();
        request.onerror = () => reject(request.error);
        request.onsuccess = () => {
          const cursor = request.result;
          if (!cursor) {
            resolve(null);
            return;
          }
          const node = cursor.value as LocalKnowledgeTreeNode;
          if (node.nodeTitle === nodeTitle && !node.isDeleted) {
            resolve(node);
            return;
          }
          cursor.continue();
        };
      });
    } finally {
      db.close();
    }
  }, title);
}

async function getLocalRow<T>(page: Page, storeName: LocalStoreName, id: string): Promise<T | null> {
  return page.evaluate(async ({ rowId, storeName }) => {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("glean-read-web");
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
    });
    try {
      return await new Promise<T | null>((resolve, reject) => {
        const transaction = db.transaction([storeName], "readonly");
        const request = transaction.objectStore(storeName).get(rowId);
        request.onerror = () => reject(request.error);
        request.onsuccess = () => resolve((request.result as T | undefined) ?? null);
      });
    } finally {
      db.close();
    }
  }, { rowId: id, storeName });
}

async function deleteLocalRow(page: Page, storeName: LocalStoreName, id: string): Promise<void> {
  await page.evaluate(async ({ rowId, storeName }) => {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("glean-read-web");
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
    });
    try {
      await new Promise<void>((resolve, reject) => {
        const transaction = db.transaction([storeName], "readwrite");
        transaction.onerror = () => reject(transaction.error);
        transaction.oncomplete = () => resolve();
        transaction.objectStore(storeName).delete(rowId);
      });
    } finally {
      db.close();
    }
  }, { rowId: id, storeName });
}

async function getLocalWorkspaceCounts(page: Page): Promise<LocalWorkspaceCounts> {
  return page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("glean-read-web");
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
    });
    try {
      const countActive = (storeName: LocalStoreName) =>
        new Promise<number>((resolve, reject) => {
          let count = 0;
          const transaction = db.transaction([storeName], "readonly");
          const request = transaction.objectStore(storeName).openCursor();
          request.onerror = () => reject(request.error);
          request.onsuccess = () => {
            const cursor = request.result;
            if (!cursor) {
              resolve(count);
              return;
            }
            if (!(cursor.value as { isDeleted?: boolean }).isDeleted) {
              count += 1;
            }
            cursor.continue();
          };
        });
      const [nodes, excerpts, tags, excerptTags] = await Promise.all([
        countActive("nodes"),
        countActive("excerpts"),
        countActive("tags"),
        countActive("excerptTags"),
      ]);
      return { nodes, excerpts, tags, excerptTags, total: nodes + excerpts + tags + excerptTags };
    } finally {
      db.close();
    }
  });
}

async function putLocalNode(page: Page, node: LocalKnowledgeTreeNode): Promise<void> {
  await page.evaluate(async (localNode) => {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("glean-read-web");
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
    });
    try {
      await new Promise<void>((resolve, reject) => {
        const transaction = db.transaction(["nodes"], "readwrite");
        transaction.onerror = () => reject(transaction.error);
        transaction.oncomplete = () => resolve();
        transaction.objectStore("nodes").put(localNode);
      });
    } finally {
      db.close();
    }
  }, node);
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
        retryCount: 0,
        localDirtyTime: timestamp,
        lastSyncTime: null,
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

async function insertPendingLocalWorkspace(page: Page, prefix: string): Promise<LocalPendingWorkspace> {
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
      const syncFields = {
        userId: session.userId,
        isDeleted: false,
        deviceId,
        syncStatus: "pending",
        syncError: null,
        retryCount: 0,
        localDirtyTime: timestamp,
        lastSyncTime: null,
      };
      const node: LocalKnowledgeTreeNode = {
        ...syncFields,
        id: `${seedPrefix}-full-node`,
        parentId: null,
        nodeTitle: `${seedPrefix} full sync node`,
        outlineMarkdown: `${seedPrefix} full sync outline`,
        createTime: timestamp,
        updateTime: timestamp,
        sortOrder: 1_310_720,
      };
      const tag: LocalTag = {
        ...syncFields,
        id: `${seedPrefix}-full-tag`,
        tagName: `${seedPrefix}-tag`,
        colorIcon: "T",
        heatWeight: 7,
        createTime: timestamp + 1,
        updateTime: timestamp + 1,
      };
      const excerpt: LocalExcerpt = {
        ...syncFields,
        id: `${seedPrefix}-full-excerpt`,
        content: `${seedPrefix} full sync excerpt`,
        url: "https://example.com/full-sync",
        sourceTitle: `${seedPrefix} full source`,
        userThought: `${seedPrefix} full thought`,
        treeNodeId: node.id,
        createTime: timestamp + 2,
        updateTime: timestamp + 2,
      };
      const excerptTag: LocalExcerptTag = {
        ...syncFields,
        id: `${seedPrefix}-full-excerpt-tag`,
        excerptId: excerpt.id,
        tagId: tag.id,
        createTime: timestamp + 3,
        updateTime: timestamp + 3,
      };
      await new Promise<void>((resolve, reject) => {
        const transaction = db.transaction(["nodes", "excerpts", "tags", "excerptTags"], "readwrite");
        transaction.onerror = () => reject(transaction.error);
        transaction.oncomplete = () => resolve();
        transaction.objectStore("nodes").put(node);
        transaction.objectStore("tags").put(tag);
        transaction.objectStore("excerpts").put(excerpt);
        transaction.objectStore("excerptTags").put(excerptTag);
      });
      return { node, excerpt, tag, excerptTag };
    } finally {
      db.close();
    }
  }, prefix);
}

async function buildCleanLocalNodeForCurrentUser(page: Page, id: string, prefix: string, updateTime: number): Promise<LocalKnowledgeTreeNode> {
  return page.evaluate(
    async ({ id, prefix, updateTime }) => {
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
        return {
          id,
          userId: session.userId,
          parentId: null,
          nodeTitle: `${prefix} local newer node`,
          outlineMarkdown: `${prefix} local newer outline`,
          createTime: updateTime,
          updateTime,
          isDeleted: false,
          deviceId: "e2e-local-newer",
          sortOrder: 2_097_152,
          syncStatus: "synced",
          syncError: null,
          retryCount: 0,
          localDirtyTime: null,
          lastSyncTime: updateTime,
        } satisfies LocalKnowledgeTreeNode;
      } finally {
        db.close();
      }
    },
    { id, prefix, updateTime }
  );
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

function expectLocalExcerptMatchesRemote(local: LocalExcerpt, remote: RemoteExcerpt) {
  expect(remote.id).toBe(local.id);
  expect(remote.user_id).toBe(local.userId);
  expect(remote.content).toBe(local.content);
  expect(remote.url).toBe(local.url);
  expect(remote.source_title).toBe(local.sourceTitle);
  expect(remote.user_thought).toBe(local.userThought);
  expect(remote.tree_node_id).toBe(local.treeNodeId);
  expect(remote.create_time).toBe(local.createTime);
  expect(remote.update_time).toBe(local.updateTime);
  expect(remote.is_deleted).toBe(local.isDeleted);
  expect(remote.device_id).toBe(local.deviceId);
}

function expectLocalTagMatchesRemote(local: LocalTag, remote: RemoteTag) {
  expect(remote.id).toBe(local.id);
  expect(remote.user_id).toBe(local.userId);
  expect(remote.tag_name).toBe(local.tagName);
  expect(remote.color_icon).toBe(local.colorIcon);
  expect(remote.heat_weight).toBe(local.heatWeight);
  expect(remote.create_time).toBe(local.createTime);
  expect(remote.update_time).toBe(local.updateTime);
  expect(remote.is_deleted).toBe(local.isDeleted);
  expect(remote.device_id).toBe(local.deviceId);
}

function expectLocalExcerptTagMatchesRemote(local: LocalExcerptTag, remote: RemoteExcerptTag) {
  expect(remote.id).toBe(local.id);
  expect(remote.user_id).toBe(local.userId);
  expect(remote.excerpt_id).toBe(local.excerptId);
  expect(remote.tag_id).toBe(local.tagId);
  expect(remote.create_time).toBe(local.createTime);
  expect(remote.update_time).toBe(local.updateTime);
  expect(remote.is_deleted).toBe(local.isDeleted);
  expect(remote.device_id).toBe(local.deviceId);
}

async function expectLocalWorkspaceMatchesRemote(page: Page, rows: RemoteWorkspaceRows) {
  const [localNode, localExcerpt, localTag, localExcerptTag] = await Promise.all([
    getLocalRow<LocalKnowledgeTreeNode>(page, "nodes", rows.node.id),
    getLocalRow<LocalExcerpt>(page, "excerpts", rows.excerpt.id),
    getLocalRow<LocalTag>(page, "tags", rows.tag.id),
    getLocalRow<LocalExcerptTag>(page, "excerptTags", rows.excerptTag.id),
  ]);
  expect(localNode).not.toBeNull();
  expect(localExcerpt).not.toBeNull();
  expect(localTag).not.toBeNull();
  expect(localExcerptTag).not.toBeNull();
  expectLocalNodeMatchesRemote(localNode!, rows.node);
  expectLocalExcerptMatchesRemote(localExcerpt!, rows.excerpt);
  expectLocalTagMatchesRemote(localTag!, rows.tag);
  expectLocalExcerptTagMatchesRemote(localExcerptTag!, rows.excerptTag);
  expectSyncedClean(localNode!);
  expectSyncedClean(localExcerpt!);
  expectSyncedClean(localTag!);
  expectSyncedClean(localExcerptTag!);
}

function expectSyncedClean(row: { syncStatus: string; syncError: string | null; localDirtyTime: number | null }) {
  expect(row.syncStatus).toBe("synced");
  expect(row.syncError).toBeNull();
  expect(row.localDirtyTime).toBeNull();
}

function expectLocalCountsMatchRemote(local: LocalWorkspaceCounts, remote: Awaited<ReturnType<typeof fetchRemoteWorkspaceCounts>>) {
  expect(local.nodes).toBe(remote.knowledge_tree_node);
  expect(local.excerpts).toBe(remote.excerpts);
  expect(local.tags).toBe(remote.tags);
  expect(local.excerptTags).toBe(remote.excerpt_tags);
  expect(local.total).toBe(sumRemoteWorkspaceCounts(remote));
}

function expectRpcBatch(
  rpcRequests: WorkspaceRpcRequest[],
  rpcName: WorkspaceSyncRpcName,
  expectedIds: string[]
) {
  const request = rpcRequests.find((item) => item.name === rpcName && expectedIds.every((id) => item.rowIds.includes(id)));
  expect(request, `${rpcName} should batch ${expectedIds.join(", ")}`).toBeTruthy();
  expect(request!.rowCount).toBe(expectedIds.length);
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

  page.on("request", (request) => {
    if (isWorkspaceSyncWriteUrl(request.url())) {
      workspaceRequests.push(request.url());
    }
  });
  page.on("response", (response) => {
    if (isWorkspaceRestUrl(response.url()) && response.status() >= 400) {
      failedWorkspaceRequests.push(`${response.status()} ${response.url()}`);
    }
  });
  page.on("requestfailed", (request) => {
    if (isWorkspaceRestUrl(request.url())) {
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

test("E2E-40 test@qq.com 云端已有数据会下行到空 IndexedDB", async ({ page }) => {
  test.setTimeout(75_000);
  let remote: RemoteClientSession | null = null;

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    const remoteCounts = await fetchRemoteWorkspaceCounts(remote.client, remote.userId);
    expect(sumRemoteWorkspaceCounts(remoteCounts)).toBeGreaterThan(0);

    await loginWithPassword(page, seededUser);

    await expect
      .poll(async () => (await getLocalWorkspaceCounts(page)).total, { timeout: 45_000 })
      .toBe(sumRemoteWorkspaceCounts(remoteCounts));
    const localCounts = await getLocalWorkspaceCounts(page);
    expectLocalCountsMatchRemote(localCounts, remoteCounts);
    expect((await getCurrentLocalSession(page))?.email).toBe(seededUser.email);
  } finally {
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-41 test2@qq.com 空云端登录后不会产生本地种子数据", async ({ page }) => {
  test.setTimeout(75_000);
  let remote: RemoteClientSession | null = null;

  try {
    remote = await createAuthenticatedSupabaseClient(emptyUser);
    const remoteCounts = await fetchRemoteWorkspaceCounts(remote.client, remote.userId);
    expect(sumRemoteWorkspaceCounts(remoteCounts)).toBe(0);

    await loginWithPassword(page, emptyUser);
    await expect.poll(async () => (await getLocalWorkspaceCounts(page)).total, { timeout: 15_000 }).toBe(0);
    await expectNoSeedExampleContent(page);
    await expect(page.getByTestId("inbox-empty")).toBeVisible();
    expect((await getCurrentLocalSession(page))?.email).toBe(emptyUser.email);
  } finally {
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-42 test2@qq.com 不会读取 test@qq.com 的云端节点", async ({ page }) => {
  test.setTimeout(75_000);
  const prefix = testPrefix("supabase-account-isolation");
  const nodeId = `${prefix}-seeded-only-node`;
  let seededRemote: RemoteClientSession | null = null;
  let emptyRemote: RemoteClientSession | null = null;

  try {
    seededRemote = await createAuthenticatedSupabaseClient(seededUser);
    emptyRemote = await createAuthenticatedSupabaseClient(emptyUser);
    const remoteNode = buildRemoteNode(seededRemote.userId, nodeId, prefix, "e2e-account-isolation");
    await upsertRemoteNode(seededRemote.client, remoteNode);

    expect(await fetchRemoteNode(seededRemote.client, nodeId)).not.toBeNull();
    expect(await fetchRemoteNode(emptyRemote.client, nodeId)).toBeNull();

    await loginWithPassword(page, emptyUser);
    await page.waitForTimeout(2_000);
    expect(await getLocalNode(page, nodeId)).toBeNull();
  } finally {
    if (seededRemote) {
      await softDeleteRemoteNode(seededRemote.client, nodeId).catch(() => undefined);
    }
    await seededRemote?.client.auth.signOut().catch(() => undefined);
    await emptyRemote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-43 本地节点、摘录、标签和关系会一起同步到 Supabase", async ({ page }) => {
  test.setTimeout(75_000);
  const prefix = testPrefix("supabase-full-push");
  let remote: RemoteClientSession | null = null;
  const localWorkspaces: LocalPendingWorkspace[] = [];
  const rpcRequests: WorkspaceRpcRequest[] = [];
  const tableWriteRequests: string[] = [];

  page.on("request", (request) => {
    const url = request.url();
    const rpcName = getWorkspaceRpcName(url);
    if (rpcName) {
      const body = request.postDataJSON() as { p_rows?: Array<{ id?: string }> } | null;
      const rows = Array.isArray(body?.p_rows) ? body.p_rows : [];
      rpcRequests.push({
        name: rpcName,
        rowIds: rows.map((row) => row.id).filter((id): id is string => Boolean(id)),
        rowCount: rows.length,
      });
      return;
    }
    if (isWorkspaceRestUrl(url) && ["POST", "PATCH", "PUT"].includes(request.method())) {
      tableWriteRequests.push(`${request.method()} ${url}`);
    }
  });

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    await loginWithPassword(page, seededUser);
    localWorkspaces.push(await insertPendingLocalWorkspace(page, `${prefix}-a`));
    localWorkspaces.push(await insertPendingLocalWorkspace(page, `${prefix}-b`));

    await page.reload();
    await waitForWorkbench(page);
    await expect
      .poll(async () => {
        const rows = await Promise.all(
          localWorkspaces.flatMap((workspace) => [
            fetchRemoteNode(remote!.client, workspace.node.id),
            fetchRemoteExcerpt(remote!.client, workspace.excerpt.id),
            fetchRemoteTag(remote!.client, workspace.tag.id),
            fetchRemoteExcerptTag(remote!.client, workspace.excerptTag.id),
          ])
        );
        return rows.every(Boolean);
      }, { timeout: 45_000 })
      .toBe(true);

    expect(tableWriteRequests).toEqual([]);
    expectRpcBatch(rpcRequests, "sync_knowledge_tree_node_conditional", localWorkspaces.map((workspace) => workspace.node.id));
    expectRpcBatch(rpcRequests, "sync_excerpts_conditional", localWorkspaces.map((workspace) => workspace.excerpt.id));
    expectRpcBatch(rpcRequests, "sync_tags_conditional", localWorkspaces.map((workspace) => workspace.tag.id));
    expectRpcBatch(rpcRequests, "sync_excerpt_tags_conditional", localWorkspaces.map((workspace) => workspace.excerptTag.id));

    for (const localBefore of localWorkspaces) {
      const [remoteNode, remoteExcerpt, remoteTag, remoteExcerptTag] = await Promise.all([
        fetchRemoteNode(remote.client, localBefore.node.id),
        fetchRemoteExcerpt(remote.client, localBefore.excerpt.id),
        fetchRemoteTag(remote.client, localBefore.tag.id),
        fetchRemoteExcerptTag(remote.client, localBefore.excerptTag.id),
      ]);
      const [localNode, localExcerpt, localTag, localExcerptTag] = await Promise.all([
        getLocalRow<LocalKnowledgeTreeNode>(page, "nodes", localBefore.node.id),
        getLocalRow<LocalExcerpt>(page, "excerpts", localBefore.excerpt.id),
        getLocalRow<LocalTag>(page, "tags", localBefore.tag.id),
        getLocalRow<LocalExcerptTag>(page, "excerptTags", localBefore.excerptTag.id),
      ]);

      expect(remoteNode).not.toBeNull();
      expect(remoteExcerpt).not.toBeNull();
      expect(remoteTag).not.toBeNull();
      expect(remoteExcerptTag).not.toBeNull();
      expectLocalNodeMatchesRemote(localBefore.node, remoteNode!);
      expectLocalExcerptMatchesRemote(localBefore.excerpt, remoteExcerpt!);
      expectLocalTagMatchesRemote(localBefore.tag, remoteTag!);
      expectLocalExcerptTagMatchesRemote(localBefore.excerptTag, remoteExcerptTag!);

      expect(localNode).not.toBeNull();
      expect(localExcerpt).not.toBeNull();
      expect(localTag).not.toBeNull();
      expect(localExcerptTag).not.toBeNull();
      expectSyncedClean(localNode!);
      expectSyncedClean(localExcerpt!);
      expectSyncedClean(localTag!);
      expectSyncedClean(localExcerptTag!);
      expectLocalNodeMatchesRemote(localNode!, remoteNode!);
      expectLocalExcerptMatchesRemote(localExcerpt!, remoteExcerpt!);
      expectLocalTagMatchesRemote(localTag!, remoteTag!);
      expectLocalExcerptTagMatchesRemote(localExcerptTag!, remoteExcerptTag!);
    }
  } finally {
    if (remote) {
      await Promise.all(
        localWorkspaces.flatMap((workspace) => [
          softDeleteRemoteRow(remote!.client, "excerpt_tags", workspace.excerptTag.id).catch(() => undefined),
          softDeleteRemoteRow(remote!.client, "excerpts", workspace.excerpt.id).catch(() => undefined),
          softDeleteRemoteRow(remote!.client, "tags", workspace.tag.id).catch(() => undefined),
          softDeleteRemoteRow(remote!.client, "knowledge_tree_node", workspace.node.id).catch(() => undefined),
        ])
      );
    }
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-44 远端节点、摘录、标签和关系会一起下行到本地", async ({ page }) => {
  test.setTimeout(75_000);
  const prefix = testPrefix("supabase-full-pull");
  let remote: RemoteClientSession | null = null;
  let rows: RemoteWorkspaceRows | null = null;

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    rows = buildRemoteWorkspace(remote.userId, prefix, "e2e-remote-full-pull");
    await upsertRemoteWorkspace(remote.client, rows);

    await loginWithPassword(page, seededUser);
    await expect
      .poll(async () => Boolean(await getLocalRow<LocalExcerptTag>(page, "excerptTags", rows!.excerptTag.id)), { timeout: 45_000 })
      .toBe(true);
    await expectLocalWorkspaceMatchesRemote(page, rows);
  } finally {
    if (remote && rows) {
      await cleanupRemoteWorkspace(remote.client, rows);
    }
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-45 远端软删除和恢复会下行到本地节点", async ({ page }) => {
  test.setTimeout(75_000);
  const prefix = testPrefix("supabase-remote-delete-restore");
  const nodeId = `${prefix}-node`;
  let remote: RemoteClientSession | null = null;

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    const remoteNode = buildRemoteNode(remote.userId, nodeId, prefix, "e2e-remote-delete-restore");
    await upsertRemoteNode(remote.client, remoteNode);

    await loginWithPassword(page, seededUser);
    await expect.poll(async () => (await getLocalNode(page, nodeId))?.isDeleted ?? null, { timeout: 30_000 }).toBe(false);

    const deletedTime = Date.now();
    await updateRemoteNode(remote.client, nodeId, {
      is_deleted: true,
      update_time: deletedTime,
      device_id: "e2e-remote-delete",
    });
    await expect.poll(async () => (await getLocalNode(page, nodeId))?.isDeleted ?? null, { timeout: 30_000 }).toBe(true);

    const restoredTime = Date.now() + 1;
    await updateRemoteNode(remote.client, nodeId, {
      is_deleted: false,
      update_time: restoredTime,
      device_id: "e2e-remote-restore",
    });
    await expect.poll(async () => (await getLocalNode(page, nodeId))?.isDeleted ?? null, { timeout: 30_000 }).toBe(false);
    expect((await getLocalNode(page, nodeId))?.updateTime).toBe(restoredTime);
  } finally {
    if (remote) {
      await softDeleteRemoteNode(remote.client, nodeId).catch(() => undefined);
    }
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-46 远端旧版本不会覆盖本地较新的 clean 节点", async ({ page }) => {
  test.setTimeout(75_000);
  const prefix = testPrefix("supabase-remote-stale");
  const nodeId = `${prefix}-node`;
  let remote: RemoteClientSession | null = null;

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    await loginWithPassword(page, seededUser);
    const localUpdateTime = Date.now() + 120_000;
    const localNode = await buildCleanLocalNodeForCurrentUser(page, nodeId, prefix, localUpdateTime);
    await putLocalNode(page, localNode);

    const staleRemoteNode = {
      ...buildRemoteNode(remote.userId, nodeId, prefix, "e2e-remote-stale"),
      node_title: `${prefix} stale remote node`,
      outline_markdown: `${prefix} stale remote outline`,
      create_time: localUpdateTime - 60_000,
      update_time: localUpdateTime - 60_000,
    };
    await upsertRemoteNode(remote.client, staleRemoteNode);

    await page.reload();
    await waitForWorkbench(page);
    await page.waitForTimeout(2_000);

    const localAfter = await getLocalNode(page, nodeId);
    expect(localAfter).not.toBeNull();
    expect(localAfter!.nodeTitle).toBe(localNode.nodeTitle);
    expect(localAfter!.outlineMarkdown).toBe(localNode.outlineMarkdown);
    expect(localAfter!.updateTime).toBe(localUpdateTime);
    expectSyncedClean(localAfter!);
  } finally {
    if (remote) {
      await softDeleteRemoteNode(remote.client, nodeId).catch(() => undefined);
    }
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-47 本设备上行回音会在拉取查询中被服务端过滤", async ({ page }) => {
  test.setTimeout(75_000);
  const prefix = testPrefix("supabase-self-echo-cursor");
  let remote: RemoteClientSession | null = null;
  let nodeId = "";
  const pullRequests: string[] = [];

  page.on("request", (request) => {
    const url = decodeURIComponent(request.url());
    if (url.includes("/rest/v1/knowledge_tree_node") && url.includes("select=*") && url.includes("device_id.neq.")) {
      pullRequests.push(url);
    }
  });

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    await loginWithPassword(page, seededUser);
    const localBefore = await insertPendingLocalNode(page, prefix);
    nodeId = localBefore.id;

    await page.reload();
    await waitForWorkbench(page);
    await expect.poll(async () => Boolean(await fetchRemoteNode(remote!.client, nodeId)), { timeout: 30_000 }).toBe(true);
    const localAfter = await getLocalNode(page, nodeId);

    expect(pullRequests.some((url) => url.includes("device_id.is.null") && url.includes("device_id.neq."))).toBe(true);
    expect(localAfter).not.toBeNull();
    expectSyncedClean(localAfter!);
    expect(localAfter!.nodeTitle).toBe(localBefore.nodeTitle);
  } finally {
    if (remote && nodeId) {
      await softDeleteRemoteNode(remote.client, nodeId).catch(() => undefined);
    }
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-48 RLS 会阻止 test2@qq.com 写入或更新 test@qq.com 的节点", async () => {
  test.setTimeout(75_000);
  const prefix = testPrefix("supabase-rls-write");
  const nodeId = `${prefix}-node`;
  let seededRemote: RemoteClientSession | null = null;
  let emptyRemote: RemoteClientSession | null = null;

  try {
    seededRemote = await createAuthenticatedSupabaseClient(seededUser);
    emptyRemote = await createAuthenticatedSupabaseClient(emptyUser);
    const remoteNode = buildRemoteNode(seededRemote.userId, nodeId, prefix, "e2e-rls-write-owner");
    await upsertRemoteNode(seededRemote.client, remoteNode);

    const forbiddenNode = buildRemoteNode(seededRemote.userId, `${prefix}-forbidden-insert`, prefix, "e2e-rls-forbidden");
    await expect(upsertRemoteNode(emptyRemote.client, forbiddenNode)).rejects.toThrow();

    const { data, error } = await emptyRemote.client
      .from("knowledge_tree_node")
      .update({
        node_title: `${prefix} forbidden update`,
        update_time: Date.now(),
        device_id: "e2e-rls-forbidden-update",
      })
      .eq("id", nodeId)
      .select("*");
    expect(error).toBeNull();
    expect(data).toEqual([]);
    expect((await fetchRemoteNode(seededRemote.client, nodeId))?.node_title).toBe(remoteNode.node_title);
  } finally {
    if (seededRemote) {
      await softDeleteRemoteNode(seededRemote.client, nodeId).catch(() => undefined);
      await softDeleteRemoteNode(seededRemote.client, `${prefix}-forbidden-insert`).catch(() => undefined);
    }
    await seededRemote?.client.auth.signOut().catch(() => undefined);
    await emptyRemote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-49 离线期间保留本地 pending，恢复在线后再上行", async ({ page }) => {
  test.setTimeout(90_000);
  const prefix = testPrefix("supabase-offline-online");
  let remote: RemoteClientSession | null = null;
  let nodeId = "";

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    await loginWithPassword(page, seededUser);
    await page.context().setOffline(true);
    await page.evaluate(() => window.dispatchEvent(new Event("offline")));
    await expect(page.getByText("离线可用")).toBeVisible();

    const localBefore = await insertPendingLocalNode(page, prefix);
    nodeId = localBefore.id;
    await page.waitForTimeout(1_500);
    expect(await fetchRemoteNode(remote.client, nodeId)).toBeNull();

    await page.context().setOffline(false);
    await page.evaluate(() => window.dispatchEvent(new Event("online")));
    await expect.poll(async () => Boolean(await fetchRemoteNode(remote!.client, nodeId)), { timeout: 45_000 }).toBe(true);
    const localAfter = await getLocalNode(page, nodeId);
    expect(localAfter).not.toBeNull();
    expectSyncedClean(localAfter!);
  } finally {
    await page.context().setOffline(false).catch(() => undefined);
    if (remote && nodeId) {
      await softDeleteRemoteNode(remote.client, nodeId).catch(() => undefined);
    }
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-50 Realtime payload 会直接落本地且不会额外触发 REST 下行", async ({ page }) => {
  test.setTimeout(75_000);
  const prefix = testPrefix("supabase-realtime-payload");
  let remote: RemoteClientSession | null = null;
  const nodeId = `${prefix}-remote-payload-node`;
  const pullRequests: string[] = [];

  page.on("request", (request) => {
    if (isWorkspacePullUrl(request.url())) {
      pullRequests.push(request.url());
    }
  });

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    await loginWithPassword(page, seededUser);
    await waitForInitialSync(page);
    pullRequests.length = 0;
    await page.route("**/rest/v1/**", async (route) => {
      if (isWorkspacePullUrl(route.request().url())) {
        pullRequests.push(route.request().url());
        await route.abort();
        return;
      }
      await route.continue();
    });

    const remoteNode = buildRemoteNode(remote.userId, nodeId, prefix, "e2e-remote-payload");
    await upsertRemoteNode(remote.client, remoteNode);

    await expect.poll(async () => (await getLocalNode(page, nodeId))?.nodeTitle ?? null, { timeout: 30_000 }).toBe(remoteNode.node_title);
    await page.waitForTimeout(2_500);

    const localNode = await getLocalNode(page, nodeId);
    expect(localNode).not.toBeNull();
    expectLocalNodeMatchesRemote(localNode!, remoteNode);
    expect(pullRequests).toEqual([]);
  } finally {
    if (remote) {
      await softDeleteRemoteNode(remote.client, nodeId).catch(() => undefined);
    }
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-51 兜底调度只上行 pending 数据且不会触发下行查询", async ({ page }) => {
  test.setTimeout(75_000);
  const prefix = testPrefix("supabase-interval-push-only");
  let remote: RemoteClientSession | null = null;
  let nodeId = "";
  const workspaceRequests: string[] = [];
  const pullRequests: string[] = [];

  await compressSyncInterval(page);
  page.on("request", (request) => {
    const url = request.url();
    if (isWorkspaceSyncWriteUrl(url)) {
      workspaceRequests.push(url);
    }
    if (isWorkspacePullUrl(url)) {
      pullRequests.push(url);
    }
  });

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    await loginWithPassword(page, seededUser);
    await waitForInitialSync(page);
    workspaceRequests.length = 0;
    pullRequests.length = 0;

    const localBefore = await insertPendingLocalNode(page, prefix);
    nodeId = localBefore.id;

    await expect.poll(async () => Boolean(await fetchRemoteNode(remote!.client, nodeId)), { timeout: 15_000 }).toBe(true);
    await page.waitForTimeout(1_500);

    expect(workspaceRequests.length).toBeGreaterThan(0);
    expect(pullRequests).toEqual([]);
    const localAfter = await getLocalNode(page, nodeId);
    expect(localAfter).not.toBeNull();
    expectSyncedClean(localAfter!);
  } finally {
    if (remote && nodeId) {
      await softDeleteRemoteNode(remote.client, nodeId).catch(() => undefined);
    }
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-52 条件上行不会用本地旧版本覆盖 Supabase 新版本", async ({ page }) => {
  test.setTimeout(90_000);
  const prefix = testPrefix("supabase-conditional-push");
  const nodeId = `${prefix}-node`;
  let remote: RemoteClientSession | null = null;

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    await loginWithPassword(page, seededUser);

    const staleTime = Date.now();
    const remoteNode = {
      ...buildRemoteNode(remote.userId, nodeId, prefix, "e2e-remote-newer"),
      node_title: `${prefix} remote newer node`,
      outline_markdown: `${prefix} remote newer outline`,
      create_time: staleTime - 1_000,
      update_time: staleTime + 120_000,
    };
    await upsertRemoteNode(remote.client, remoteNode);
    await putLocalNode(page, {
      id: nodeId,
      userId: remote.userId,
      parentId: null,
      nodeTitle: `${prefix} local stale node`,
      outlineMarkdown: `${prefix} local stale outline`,
      createTime: staleTime - 2_000,
      updateTime: staleTime,
      isDeleted: false,
      deviceId: "e2e-local-stale",
      sortOrder: 2_031_616,
      syncStatus: "pending",
      syncError: null,
      retryCount: 0,
      localDirtyTime: staleTime,
      lastSyncTime: null,
    });

    await page.reload();
    await waitForWorkbench(page);

    await expect
      .poll(async () => (await fetchRemoteNode(remote!.client, nodeId))?.node_title ?? null, { timeout: 45_000 })
      .toBe(remoteNode.node_title);
    await expect
      .poll(async () => (await getLocalNode(page, nodeId))?.nodeTitle ?? null, { timeout: 45_000 })
      .toBe(remoteNode.node_title);

    const remoteAfter = await fetchRemoteNode(remote.client, nodeId);
    const localAfter = await getLocalNode(page, nodeId);
    expect(remoteAfter).not.toBeNull();
    expect(localAfter).not.toBeNull();
    expect(remoteAfter!.update_time).toBe(remoteNode.update_time);
    expectLocalNodeMatchesRemote(localAfter!, remoteAfter!);
    expectSyncedClean(localAfter!);
  } finally {
    if (remote) {
      await softDeleteRemoteNode(remote.client, nodeId).catch(() => undefined);
    }
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});

test("E2E-53 本地变更会通过 dirty 调度及时上行，不等待兜底间隔", async ({ page }) => {
  test.setTimeout(90_000);
  const prefix = testPrefix("supabase-dirty-schedule");
  const title = `${prefix} dirty scheduled node`;
  let remote: RemoteClientSession | null = null;
  let nodeId = "";

  try {
    remote = await createAuthenticatedSupabaseClient(seededUser);
    await loginWithPassword(page, seededUser);
    await waitForInitialSync(page);

    await firstRealNode(page).click();
    await expect(page.getByTestId("knowledge-tree-canvas")).toBeFocused();
    await page.keyboard.press("Tab");
    await expect(page.locator("[data-node-edit-input]")).toBeVisible();
    await page.locator("[data-node-edit-input]").fill(title);
    await page.keyboard.press("Enter");

    await expect.poll(async () => (await getLocalNodeByTitle(page, title))?.id ?? null, { timeout: 10_000 }).not.toBeNull();
    nodeId = (await getLocalNodeByTitle(page, title))!.id;

    await expect.poll(async () => Boolean(await fetchRemoteNode(remote!.client, nodeId)), { timeout: 30_000 }).toBe(true);
    const remoteNode = await fetchRemoteNode(remote.client, nodeId);
    const localAfter = await getLocalNode(page, nodeId);
    expect(remoteNode).not.toBeNull();
    expect(localAfter).not.toBeNull();
    expectLocalNodeMatchesRemote(localAfter!, remoteNode!);
    expectSyncedClean(localAfter!);
  } finally {
    if (remote && nodeId) {
      await softDeleteRemoteNode(remote.client, nodeId).catch(() => undefined);
    }
    if (nodeId) {
      await deleteLocalRow(page, "nodes", nodeId).catch(() => undefined);
    }
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await remote?.client.auth.signOut().catch(() => undefined);
  }
});
