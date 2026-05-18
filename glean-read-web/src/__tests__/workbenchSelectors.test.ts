import test from "node:test";
import assert from "node:assert/strict";
import { previewSnapshot } from "../app/previews/previewData.ts";
import {
  buildNodeExcerptCountMap,
  getInboxExcerpts,
  getNodeFeed,
  getNodeViewModels,
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

test("标签搜索能定位到关联摘录和节点", () => {
  const results = searchWorkspace(previewSnapshot, "#架构");
  assert.ok(results.some((result) => result.type === "tag" && result.title === "#架构"));
  assert.ok(results.some((result) => result.type === "excerpt" && result.targetNodeId === "node-offline"));

  const feed = getNodeFeed(previewSnapshot, "node-offline");
  assert.equal(feed.length, 1);
  assert.equal(feed[0].tags[0].tagName, "架构");
});
