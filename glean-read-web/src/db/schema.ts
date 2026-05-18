import type {
  AuthSession,
  Excerpt,
  ExcerptTag,
  KnowledgeTreeNode,
  RecentSearch,
  Tag,
  WorkspacePreference,
} from "@/shared/models";

export interface DexieTables {
  nodes: KnowledgeTreeNode;
  excerpts: Excerpt;
  tags: Tag;
  excerptTags: ExcerptTag;
  recentSearches: RecentSearch;
  preferences: WorkspacePreference;
  sessions: AuthSession & { id: string };
}
