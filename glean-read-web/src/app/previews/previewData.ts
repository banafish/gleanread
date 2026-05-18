import type { Excerpt, ExcerptTag, KnowledgeTreeNode, RecentSearch, Tag, WorkspaceSnapshot } from "@/shared/models";

const userId = "preview-user";
const deviceId = "preview-device";
const baseTime = 1_720_000_000_000;

function syncFields(offset: number) {
  return {
    userId,
    createTime: baseTime + offset,
    updateTime: baseTime + offset,
    isDeleted: false,
    deviceId,
    syncStatus: "synced" as const,
    syncError: null,
    retryCount: 0,
    localDirtyTime: null,
    lastSyncTime: baseTime + offset,
  };
}

export const previewNodes: KnowledgeTreeNode[] = [
  {
    id: "node-product",
    parentId: null,
    nodeTitle: "前端工程化",
    outlineMarkdown: "## 核心问题\n\n构建从需求到交付的稳定路径。",
    sortOrder: 65_536,
    ...syncFields(1),
  },
  {
    id: "node-build",
    parentId: "node-product",
    nodeTitle: "构建系统",
    outlineMarkdown: "- Vite dev server\n- Bundle 分析\n- 代码分割",
    sortOrder: 65_536,
    ...syncFields(2),
  },
  {
    id: "node-state",
    parentId: "node-product",
    nodeTitle: "状态管理",
    outlineMarkdown: "",
    sortOrder: 131_072,
    ...syncFields(3),
  },
  {
    id: "node-offline",
    parentId: "node-state",
    nodeTitle: "Local First",
    outlineMarkdown: "Dexie 负责秒开与离线写入，Supabase 作为最终同步目标。",
    sortOrder: 65_536,
    ...syncFields(4),
  },
];

export const previewTags: Tag[] = [
  {
    id: "tag-architecture",
    tagName: "架构",
    colorIcon: "●",
    heatWeight: 12,
    ...syncFields(10),
  },
  {
    id: "tag-build",
    tagName: "构建",
    colorIcon: "◆",
    heatWeight: 8,
    ...syncFields(11),
  },
];

export const previewExcerpts: Excerpt[] = [
  {
    id: "excerpt-inbox",
    content: "把零散摘录先放进收件箱，再在知识树里找到合适的位置，可以减少分类时的认知摩擦。",
    url: "https://example.com/local-first-notes",
    sourceTitle: "Local-first notes",
    userThought: "先收集，再结构化，顺序很重要。",
    treeNodeId: null,
    ...syncFields(20),
  },
  {
    id: "excerpt-build",
    content: "构建系统需要同时关注开发体验和生产包体，单纯追求首屏炫技会让长期维护变重。",
    url: "https://example.com/build-pipeline",
    sourceTitle: "Build pipeline guide",
    userThought: "这条适合放在构建系统下面。",
    treeNodeId: "node-build",
    ...syncFields(21),
  },
  {
    id: "excerpt-offline",
    content: "本地优先应用应该把 UI 状态、IndexedDB 持久化和远端同步拆成不同层次。",
    url: "https://example.com/offline-sync",
    sourceTitle: "Offline sync patterns",
    userThought: "可以作为同步设计的总纲。",
    treeNodeId: "node-offline",
    ...syncFields(22),
  },
];

export const previewExcerptTags: ExcerptTag[] = [
  {
    id: "relation-build",
    excerptId: "excerpt-build",
    tagId: "tag-build",
    ...syncFields(30),
  },
  {
    id: "relation-offline",
    excerptId: "excerpt-offline",
    tagId: "tag-architecture",
    ...syncFields(31),
  },
  {
    id: "relation-inbox",
    excerptId: "excerpt-inbox",
    tagId: "tag-architecture",
    ...syncFields(32),
  },
];

export const previewRecentSearches: RecentSearch[] = [
  {
    id: "recent-local-first",
    userId,
    query: "#架构",
    createTime: baseTime + 40,
  },
  {
    id: "recent-build",
    userId,
    query: "构建系统",
    createTime: baseTime + 41,
  },
];

export const previewSnapshot: WorkspaceSnapshot = {
  nodes: previewNodes,
  excerpts: previewExcerpts,
  tags: previewTags,
  excerptTags: previewExcerptTags,
  recentSearches: previewRecentSearches,
};
