import test from "node:test";
import assert from "node:assert/strict";
import { previewSnapshot } from "../app/previews/previewData.ts";
import type { WorkspaceSnapshot } from "../shared/models.ts";
import {
  buildNodeExcerptCountMap,
  getInboxExcerpts,
  getNodeFeed,
  getNodeViewModels,
  getTrashExcerpts,
  getTrashNodes,
  getTrashTags,
  searchWorkspace,
} from "../features/workbench/workbenchSelectors.ts";

test("挂载摘录后会离开收件箱并增加目标节点计数", () => {
  const beforeInbox = getInboxExcerpts(previewSnapshot, "inbox");
  assert.ok(beforeInbox.some((excerpt) => excerpt.id === "excerpt-inbox"));

  const mountedSnapshot = {
    ...previewSnapshot,
    excerpts: previewSnapshot.excerpts.map((excerpt) =>
      excerpt.id === "excerpt-inbox" ? { ...excerpt, treeNodeId: "node-build" } : excerpt
    ),
  };

  const afterInbox = getInboxExcerpts(mountedSnapshot, "inbox");
  const counts = buildNodeExcerptCountMap(mountedSnapshot.excerpts);
  assert.equal(afterInbox.some((excerpt) => excerpt.id === "excerpt-inbox"), false);
  assert.equal(counts.get("node-build"), 2);
});

test("知识树默认展示第一层并按展开状态显示深层节点", () => {
  const collapsed = getNodeViewModels(previewSnapshot, {});
  assert.ok(collapsed.some((node) => node.id === "node-product"));
  assert.ok(collapsed.some((node) => node.id === "node-build"));
  assert.equal(collapsed.some((node) => node.id === "node-offline"), false);

  const expanded = getNodeViewModels(previewSnapshot, { "node-state": true });
  assert.ok(expanded.some((node) => node.id === "node-offline"));
});

test("知识树同级节点按 sort_order 排序", () => {
  const reorderedSnapshot = {
    ...previewSnapshot,
    nodes: previewSnapshot.nodes.map((node) => {
      if (node.id === "node-build") {
        return { ...node, sortOrder: 131_072 };
      }
      if (node.id === "node-state") {
        return { ...node, sortOrder: 65_536 };
      }
      return node;
    }),
  };

  const viewModels = getNodeViewModels(reorderedSnapshot, { "node-product": true });
  const childIds = viewModels.filter((node) => node.parentId === "node-product").map((node) => node.id);
  assert.deepEqual(childIds, ["node-state", "node-build"]);
});

test("标签搜索能够定位到关联摘录和节点", () => {
  const results = searchWorkspace(previewSnapshot, "#架构");
  assert.ok(results.some((result) => result.type === "tag" && result.title === "#架构"));
  assert.ok(results.some((result) => result.type === "excerpt" && result.targetNodeId === "node-offline"));

  const feed = getNodeFeed(previewSnapshot, "node-offline");
  assert.equal(feed.length, 1);
  assert.equal(feed[0].tags[0].tagName, "架构");
});

test("垃圾篓只展示顶层删除内容并按更新时间排序", () => {
  const trashSnapshot: WorkspaceSnapshot = {
    nodes: [
      {
        id: "node-trash-a",
        userId: "preview-user",
        parentId: null,
        nodeTitle: "删除节点 A",
        outlineMarkdown: "",
        createTime: 1,
        updateTime: 30,
        isDeleted: true,
        deviceId: null,
        sortOrder: 65_536,
        syncStatus: "synced",
        syncError: null,
        retryCount: 0,
        localDirtyTime: null,
        lastSyncTime: null,
      },
      {
        id: "node-trash-b",
        userId: "preview-user",
        parentId: null,
        nodeTitle: "删除节点 B",
        outlineMarkdown: "",
        createTime: 2,
        updateTime: 40,
        isDeleted: true,
        deviceId: null,
        sortOrder: 131_072,
        syncStatus: "synced",
        syncError: null,
        retryCount: 0,
        localDirtyTime: null,
        lastSyncTime: null,
      },
      {
        id: "node-trash-child",
        userId: "preview-user",
        parentId: "node-trash-a",
        nodeTitle: "删除子节点",
        outlineMarkdown: "",
        createTime: 3,
        updateTime: 50,
        isDeleted: true,
        deviceId: null,
        sortOrder: 196_608,
        syncStatus: "synced",
        syncError: null,
        retryCount: 0,
        localDirtyTime: null,
        lastSyncTime: null,
      },
    ],
    excerpts: [
      {
        id: "excerpt-trash",
        userId: "preview-user",
        content: "已删除摘录",
        url: null,
        sourceTitle: "草稿来源",
        userThought: null,
        treeNodeId: null,
        createTime: 4,
        updateTime: 20,
        isDeleted: true,
        deviceId: null,
        syncStatus: "synced",
        syncError: null,
        retryCount: 0,
        localDirtyTime: null,
        lastSyncTime: null,
      },
    ],
    tags: [
      {
        id: "tag-trash",
        userId: "preview-user",
        tagName: "归档",
        colorIcon: null,
        heatWeight: 3,
        createTime: 5,
        updateTime: 60,
        isDeleted: true,
        deviceId: null,
        syncStatus: "synced",
        syncError: null,
        retryCount: 0,
        localDirtyTime: null,
        lastSyncTime: null,
      },
    ],
    excerptTags: [],
    recentSearches: [],
  };

  const trashNodes = getTrashNodes(trashSnapshot);
  assert.deepEqual(trashNodes.map((node) => node.id), ["node-trash-b", "node-trash-a"]);

  const trashExcerpts = getTrashExcerpts(trashSnapshot);
  assert.deepEqual(trashExcerpts.map((excerpt) => excerpt.id), ["excerpt-trash"]);

  const trashTags = getTrashTags(trashSnapshot);
  assert.deepEqual(trashTags.map((tag) => tag.id), ["tag-trash"]);
});
