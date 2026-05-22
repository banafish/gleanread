export type ThemeMode = "light" | "dark" | "system";

export type AuthMode = "login" | "signup";

export type InboxFilter = "inbox" | "all";

export interface AuthSession {
  userId: string;
  email: string;
  avatarUrl?: string | null;
  accessToken?: string;
  refreshToken?: string;
  provider: "local" | "supabase";
}

export interface KnowledgeTreeNode {
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
  syncStatus: SyncStatus;
  syncError: string | null;
  retryCount: number;
  localDirtyTime: number | null;
  lastSyncTime: number | null;
}

export interface Excerpt {
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
  syncStatus: SyncStatus;
  syncError: string | null;
  retryCount: number;
  localDirtyTime: number | null;
  lastSyncTime: number | null;
}

export interface Tag {
  id: string;
  userId: string;
  tagName: string;
  colorIcon: string | null;
  heatWeight: number;
  createTime: number;
  updateTime: number;
  isDeleted: boolean;
  deviceId: string | null;
  syncStatus: SyncStatus;
  syncError: string | null;
  retryCount: number;
  localDirtyTime: number | null;
  lastSyncTime: number | null;
}

export interface ExcerptTag {
  id: string;
  userId: string;
  excerptId: string;
  tagId: string;
  createTime: number;
  updateTime: number;
  isDeleted: boolean;
  deviceId: string | null;
  syncStatus: SyncStatus;
  syncError: string | null;
  retryCount: number;
  localDirtyTime: number | null;
  lastSyncTime: number | null;
}

export interface RecentSearch {
  id: string;
  userId: string;
  query: string;
  createTime: number;
}

export interface WorkspacePreference {
  id: string;
  userId: string;
  key: string;
  value: string;
  updateTime: number;
}

export type SyncStatus = "synced" | "pending" | "syncing" | "failed" | "conflict";

export type RemoteTableName = "knowledge_tree_node" | "excerpts" | "tags" | "excerpt_tags";

export interface SyncCursor {
  id: string;
  userId: string;
  tableName: RemoteTableName;
  lastPulledAt: number;
  updateTime: number;
}

export interface WorkspaceSnapshot {
  nodes: KnowledgeTreeNode[];
  excerpts: Excerpt[];
  tags: Tag[];
  excerptTags: ExcerptTag[];
  recentSearches: RecentSearch[];
}

export interface SearchResult {
  type: "node" | "excerpt" | "tag";
  id: string;
  title: string;
  subtitle: string;
  targetNodeId?: string | null;
  excerptId?: string | null;
  tagId?: string | null;
}

export interface TreeNodeViewModel {
  id: string;
  parentId: string | null;
  title: string;
  outlineMarkdown: string;
  sortOrder: number;
  excerptCount: number;
  hasOutline: boolean;
  isExpanded: boolean;
  depth: number;
  isVirtualRoot?: boolean;
}

export interface ExcerptViewModel {
  id: string;
  content: string;
  sourceTitle: string | null;
  url: string | null;
  userThought: string | null;
  treeNodeId: string | null;
  tags: Tag[];
}

export interface WorkbenchViewState {
  selectedNodeId: string | null;
  selectedExcerptId: string | null;
  drawerOpen: boolean;
  drawerFullscreen: boolean;
  inboxFilter: InboxFilter;
  searchOpen: boolean;
  searchQuery: string;
  trashOpen: boolean;
  themeMode: ThemeMode;
  leftPanelWidth: number;
  rightPanelWidth: number;
  expandedNodeIds: Record<string, boolean>;
  hoveredNodeId: string | null;
  viewport: { x: number; y: number; zoom: number };
  recentSearches: RecentSearch[];
}
