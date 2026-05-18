import Dexie, { type Table } from "dexie";
import type {
  AuthSession,
  Excerpt,
  ExcerptTag,
  KnowledgeTreeNode,
  RecentSearch,
  Tag,
  SyncCursor,
  WorkspacePreference,
} from "@/shared/models";

export class GleanReadDexie extends Dexie {
  nodes!: Table<KnowledgeTreeNode, string>;
  excerpts!: Table<Excerpt, string>;
  tags!: Table<Tag, string>;
  excerptTags!: Table<ExcerptTag, string>;
  recentSearches!: Table<RecentSearch, string>;
  preferences!: Table<WorkspacePreference, string>;
  syncCursors!: Table<SyncCursor, string>;
  sessions!: Table<AuthSession & { id: string }, string>;

  constructor() {
    super("glean-read-web");
    this.version(1).stores({
      nodes: "id, userId, parentId, sortOrder, updateTime, isDeleted, syncStatus",
      excerpts: "id, userId, treeNodeId, updateTime, isDeleted, syncStatus",
      tags: "id, userId, tagName, updateTime, isDeleted, syncStatus",
      excerptTags: "id, userId, excerptId, tagId, updateTime, isDeleted, syncStatus",
      recentSearches: "id, userId, query, createTime",
      preferences: "id, userId, key, updateTime",
      syncCursors: "id, userId, tableName, lastPulledAt, updateTime",
      sessions: "id, userId, email",
    });
  }
}

export const db = new GleanReadDexie();
