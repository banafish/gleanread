import type { Excerpt, ExcerptTag, KnowledgeTreeNode, Tag } from "@/shared/models";
import { createId, now } from "@/shared/utils";

export function createSeedData(userId: string): {
  nodes: KnowledgeTreeNode[];
  excerpts: Excerpt[];
  tags: Tag[];
  excerptTags: ExcerptTag[];
} {
  const timestamp = now();
  const deviceId = "local-device";

  const root: KnowledgeTreeNode = {
    id: "node-root-frontend",
    userId,
    parentId: null,
    nodeTitle: "前端工程化",
    outlineMarkdown: "# 前端工程化\n\n构建、协作、发布与维护。",
    createTime: timestamp - 120_000,
    updateTime: timestamp - 60_000,
    isDeleted: false,
    deviceId,
    sortOrder: 65_536,
    syncStatus: "synced",
    syncError: null,
    retryCount: 0,
    localDirtyTime: null,
    lastSyncTime: timestamp - 30_000,
  };

  const reactNode: KnowledgeTreeNode = {
    id: "node-react",
    userId,
    parentId: root.id,
    nodeTitle: "React",
    outlineMarkdown: "## React\n\n组件化与状态管理。",
    createTime: timestamp - 110_000,
    updateTime: timestamp - 55_000,
    isDeleted: false,
    deviceId,
    sortOrder: 65_536,
    syncStatus: "synced",
    syncError: null,
    retryCount: 0,
    localDirtyTime: null,
    lastSyncTime: timestamp - 30_000,
  };

  const treeNode: KnowledgeTreeNode = {
    id: "node-tree",
    userId,
    parentId: reactNode.id,
    nodeTitle: "知识树画布",
    outlineMarkdown: "",
    createTime: timestamp - 100_000,
    updateTime: timestamp - 25_000,
    isDeleted: false,
    deviceId,
    sortOrder: 65_536,
    syncStatus: "synced",
    syncError: null,
    retryCount: 0,
    localDirtyTime: null,
    lastSyncTime: timestamp - 20_000,
  };

  const inboxNode: KnowledgeTreeNode = {
    id: "node-inbox",
    userId,
    parentId: null,
    nodeTitle: "收件箱",
    outlineMarkdown: "",
    createTime: timestamp - 90_000,
    updateTime: timestamp - 15_000,
    isDeleted: false,
    deviceId,
    sortOrder: 131_072,
    syncStatus: "synced",
    syncError: null,
    retryCount: 0,
    localDirtyTime: null,
    lastSyncTime: timestamp - 10_000,
  };

  const nodeList = [root, reactNode, treeNode, inboxNode];

  const tags: Tag[] = [
    {
      id: createId("tag"),
      userId,
      tagName: "架构",
      colorIcon: "◉",
      heatWeight: 12,
      createTime: timestamp - 100_000,
      updateTime: timestamp - 90_000,
      isDeleted: false,
      deviceId,
      syncStatus: "synced",
      syncError: null,
      retryCount: 0,
      localDirtyTime: null,
      lastSyncTime: timestamp - 80_000,
    },
    {
      id: createId("tag"),
      userId,
      tagName: "前端",
      colorIcon: "◍",
      heatWeight: 18,
      createTime: timestamp - 98_000,
      updateTime: timestamp - 70_000,
      isDeleted: false,
      deviceId,
      syncStatus: "synced",
      syncError: null,
      retryCount: 0,
      localDirtyTime: null,
      lastSyncTime: timestamp - 60_000,
    },
    {
      id: createId("tag"),
      userId,
      tagName: "同步",
      colorIcon: "●",
      heatWeight: 8,
      createTime: timestamp - 95_000,
      updateTime: timestamp - 60_000,
      isDeleted: false,
      deviceId,
      syncStatus: "synced",
      syncError: null,
      retryCount: 0,
      localDirtyTime: null,
      lastSyncTime: timestamp - 50_000,
    },
  ];

  const excerpts: Excerpt[] = [
    {
      id: createId("excerpt"),
      userId,
      content: "React Flow 适合把关系结构做成可视化画布。",
      url: "https://reactflow.dev/",
      sourceTitle: "React Flow",
      userThought: "适合作为知识树的画布底座。",
      treeNodeId: treeNode.id,
      createTime: timestamp - 80_000,
      updateTime: timestamp - 40_000,
      isDeleted: false,
      deviceId,
      syncStatus: "synced",
      syncError: null,
      retryCount: 0,
      localDirtyTime: null,
      lastSyncTime: timestamp - 30_000,
    },
    {
      id: createId("excerpt"),
      userId,
      content: "Dagre 可以稳定地产出从左到右的 DAG 布局。",
      url: "https://github.com/dagrejs/dagre",
      sourceTitle: "Dagre",
      userThought: "用于展开后的树布局。",
      treeNodeId: reactNode.id,
      createTime: timestamp - 70_000,
      updateTime: timestamp - 35_000,
      isDeleted: false,
      deviceId,
      syncStatus: "synced",
      syncError: null,
      retryCount: 0,
      localDirtyTime: null,
      lastSyncTime: timestamp - 25_000,
    },
    {
      id: createId("excerpt"),
      userId,
      content: "未分类摘录应该始终留在 Inbox 中，等待拖拽整理。",
      url: null,
      sourceTitle: "GleanRead 设计稿",
      userThought: null,
      treeNodeId: null,
      createTime: timestamp - 60_000,
      updateTime: timestamp - 20_000,
      isDeleted: false,
      deviceId,
      syncStatus: "synced",
      syncError: null,
      retryCount: 0,
      localDirtyTime: null,
      lastSyncTime: timestamp - 10_000,
    },
  ];

  const excerptTags: ExcerptTag[] = [
    {
      id: createId("excerpt-tag"),
      userId,
      excerptId: excerpts[0].id,
      tagId: tags[1].id,
      createTime: timestamp - 70_000,
      updateTime: timestamp - 40_000,
      isDeleted: false,
      deviceId,
      syncStatus: "synced",
      syncError: null,
      retryCount: 0,
      localDirtyTime: null,
      lastSyncTime: timestamp - 30_000,
    },
    {
      id: createId("excerpt-tag"),
      userId,
      excerptId: excerpts[1].id,
      tagId: tags[0].id,
      createTime: timestamp - 60_000,
      updateTime: timestamp - 30_000,
      isDeleted: false,
      deviceId,
      syncStatus: "synced",
      syncError: null,
      retryCount: 0,
      localDirtyTime: null,
      lastSyncTime: timestamp - 20_000,
    },
  ];

  return { nodes: nodeList, excerpts, tags, excerptTags };
}
