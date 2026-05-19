import "fake-indexeddb/auto";
import test, { afterEach, beforeEach } from "node:test";
import assert from "node:assert/strict";
import { db } from "../db/dexie.ts";
import {
  createChildNode,
  deleteExcerpt,
  deleteNodeSubtree,
  ensureSessionSeed,
  ensureTag,
  getPreference,
  getWorkspaceSnapshot,
  moveExcerptToNode,
  renameNode,
  restoreExcerpt,
  restoreNodeSubtree,
  savePreference,
  updateExcerpt,
} from "../db/repositories/workspaceRepository.ts";
import type { Excerpt } from "../shared/models.ts";

async function resetDatabase() {
  if (db.isOpen()) {
    db.close();
  }
  await db.delete();
  await db.open();
}

beforeEach(resetDatabase);

afterEach(async () => {
  if (db.isOpen()) {
    db.close();
  }
});

test("Supabase 会话不会为空账号自动写入示例数据", async () => {
  await ensureSessionSeed({
    userId: "remote-empty-user",
    email: "test1@qq.com",
    provider: "supabase",
  });

  const snapshot = await getWorkspaceSnapshot("remote-empty-user");
  assert.equal(snapshot.nodes.length, 0);
  assert.equal(snapshot.excerpts.length, 0);
  assert.equal(snapshot.tags.length, 0);
});

test("本地 fallback 会话仍可显式初始化示例数据", async () => {
  await ensureSessionSeed({
    userId: "local-user",
    email: "local@example.com",
    provider: "local",
  });

  const snapshot = await getWorkspaceSnapshot("local-user");
  assert.ok(snapshot.nodes.length > 0);
  assert.ok(snapshot.excerpts.length > 0);
});

test("Repository 支持节点创建、改名、软删除与恢复", async () => {
  const userId = "repo-user";
  const node = await createChildNode(userId, null, "临时节点");
  await renameNode(userId, node.id, "已改名节点");

  let snapshot = await getWorkspaceSnapshot(userId);
  assert.equal(snapshot.nodes[0]?.nodeTitle, "已改名节点");
  assert.equal(snapshot.nodes[0]?.syncStatus, "pending");

  await deleteNodeSubtree(userId, node.id);
  snapshot = await getWorkspaceSnapshot(userId);
  assert.equal(snapshot.nodes[0]?.isDeleted, true);

  await restoreNodeSubtree(userId, node.id);
  snapshot = await getWorkspaceSnapshot(userId);
  assert.equal(snapshot.nodes[0]?.nodeTitle, "已改名节点");
});

test("Repository 支持摘录挂载、移回、标签更新和恢复", async () => {
  const userId = "repo-user";
  const node = await createChildNode(userId, null, "挂载目标");
  const excerpt: Excerpt = {
    id: "excerpt-repo",
    userId,
    content: "可挂载摘录",
    url: null,
    sourceTitle: "测试来源",
    userThought: null,
    treeNodeId: null,
    createTime: 1,
    updateTime: 1,
    isDeleted: false,
    deviceId: null,
    syncStatus: "synced",
    syncError: null,
    retryCount: 0,
    localDirtyTime: null,
    lastSyncTime: null,
  };
  await db.excerpts.add(excerpt);

  await moveExcerptToNode(userId, excerpt.id, node.id);
  assert.equal((await db.excerpts.get(excerpt.id))?.treeNodeId, node.id);

  const tag = await ensureTag(userId, "自动化标签", "●");
  assert.ok(tag);
  await updateExcerpt(userId, excerpt.id, {
    content: excerpt.content,
    userThought: "更新后的思考",
    sourceTitle: excerpt.sourceTitle,
    url: excerpt.url,
    treeNodeId: node.id,
    tagIds: [tag!.id],
  });
  assert.equal((await db.excerpts.get(excerpt.id))?.userThought, "更新后的思考");
  assert.equal(await db.excerptTags.where({ excerptId: excerpt.id }).count(), 1);

  await moveExcerptToNode(userId, excerpt.id, null);
  assert.equal((await db.excerpts.get(excerpt.id))?.treeNodeId, null);

  await deleteExcerpt(userId, excerpt.id);
  assert.equal((await getWorkspaceSnapshot(userId)).excerpts[0]?.isDeleted, true);
  await restoreExcerpt(userId, excerpt.id);
  assert.equal((await getWorkspaceSnapshot(userId)).excerpts[0]?.isDeleted, false);
});

test("偏好按 userId 隔离保存", async () => {
  await savePreference("user-a", "theme-mode", "dark");
  await savePreference("user-b", "theme-mode", "light");

  assert.equal(await getPreference("user-a", "theme-mode"), "dark");
  assert.equal(await getPreference("user-b", "theme-mode"), "light");
});
