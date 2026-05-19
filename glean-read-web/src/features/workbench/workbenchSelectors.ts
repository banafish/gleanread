import type {
  Excerpt,
  ExcerptTag,
  ExcerptViewModel,
  KnowledgeTreeNode,
  SearchResult,
  Tag,
  WorkspaceSnapshot,
  InboxFilter,
  TreeNodeViewModel,
} from "@/shared/models";

function isActiveTag(tag: Tag | undefined): tag is Tag {
  return Boolean(tag && !tag.isDeleted);
}

export function buildNodeMap(nodes: KnowledgeTreeNode[]): Map<string, KnowledgeTreeNode> {
  return new Map(nodes.map((node) => [node.id, node]));
}

export function buildTagMap(tags: Tag[]): Map<string, Tag> {
  return new Map(tags.map((tag) => [tag.id, tag]));
}

export function buildNodeExcerptCountMap(excerpts: Excerpt[]): Map<string, number> {
  const counts = new Map<string, number>();
  for (const excerpt of excerpts) {
    if (!excerpt.treeNodeId || excerpt.isDeleted) {
      continue;
    }
    counts.set(excerpt.treeNodeId, (counts.get(excerpt.treeNodeId) ?? 0) + 1);
  }
  return counts;
}

export function getNodeViewModels(snapshot: WorkspaceSnapshot, expandedNodeIds: Record<string, boolean>): TreeNodeViewModel[] {
  const activeNodes = snapshot.nodes.filter((node) => !node.isDeleted);
  const nodeMap = buildNodeMap(activeNodes);
  const excerptCountMap = buildNodeExcerptCountMap(snapshot.excerpts);
  const childrenByParent = new Map<string | null, KnowledgeTreeNode[]>();
  for (const node of activeNodes) {
    const list = childrenByParent.get(node.parentId) ?? [];
    list.push(node);
    childrenByParent.set(node.parentId, list);
  }
  const sortNodes = (items: KnowledgeTreeNode[]) => [...items].sort((a, b) => a.sortOrder - b.sortOrder || a.createTime - b.createTime);
  for (const [key, items] of childrenByParent) {
    childrenByParent.set(key, sortNodes(items));
  }

  const result: TreeNodeViewModel[] = [
    {
      id: "__virtual_root__",
      parentId: null,
      title: "知识体系",
      outlineMarkdown: "",
      sortOrder: 0,
      excerptCount: snapshot.excerpts.filter((excerpt) => !excerpt.isDeleted && excerpt.treeNodeId === null).length,
      hasOutline: false,
      isExpanded: true,
      depth: 0,
      isVirtualRoot: true,
    },
  ];

  const visit = (node: KnowledgeTreeNode, depth: number): void => {
    const childNodes = childrenByParent.get(node.id) ?? [];
    const isExpanded = Object.prototype.hasOwnProperty.call(expandedNodeIds, node.id)
      ? Boolean(expandedNodeIds[node.id])
      : depth <= 1;
    result.push({
      id: node.id,
      parentId: node.parentId,
      title: node.nodeTitle,
      outlineMarkdown: node.outlineMarkdown,
      sortOrder: node.sortOrder,
      excerptCount: excerptCountMap.get(node.id) ?? 0,
      hasOutline: node.outlineMarkdown.trim().length > 0,
      isExpanded,
      depth,
    });
    if (childNodes.length > 0 && isExpanded) {
      for (const child of childNodes) {
        visit(child, depth + 1);
      }
    }
  };

  for (const node of childrenByParent.get(null) ?? []) {
    visit(node, 1);
  }

  return result;
}

export function getNodeBreadthFirstOrder(snapshot: WorkspaceSnapshot, expandedNodeIds: Record<string, boolean>): string[] {
  return getNodeViewModels(snapshot, expandedNodeIds)
    .filter((node) => !node.isVirtualRoot)
    .map((node) => node.id);
}

export function getTrashNodes(snapshot: WorkspaceSnapshot): KnowledgeTreeNode[] {
  const deletedNodeIds = new Set(snapshot.nodes.filter((node) => node.isDeleted).map((node) => node.id));
  return snapshot.nodes
    .filter((node) => node.isDeleted && (!node.parentId || !deletedNodeIds.has(node.parentId)))
    .sort((a, b) => b.updateTime - a.updateTime || a.nodeTitle.localeCompare(b.nodeTitle));
}

export function getTrashExcerpts(snapshot: WorkspaceSnapshot): Excerpt[] {
  return snapshot.excerpts
    .filter((excerpt) => excerpt.isDeleted)
    .sort((a, b) => b.updateTime - a.updateTime || a.createTime - b.createTime);
}

export function getTrashTags(snapshot: WorkspaceSnapshot): Tag[] {
  return snapshot.tags
    .filter((tag) => tag.isDeleted)
    .sort((a, b) => b.updateTime - a.updateTime || a.tagName.localeCompare(b.tagName));
}

export function getInboxExcerpts(snapshot: WorkspaceSnapshot, filter: InboxFilter): ExcerptViewModel[] {
  const tagMap = buildTagMap(snapshot.tags);
  const excerptTagMap = new Map<string, ExcerptTag[]>();
  for (const relation of snapshot.excerptTags) {
    if (relation.isDeleted) {
      continue;
    }
    const list = excerptTagMap.get(relation.excerptId) ?? [];
    list.push(relation);
    excerptTagMap.set(relation.excerptId, list);
  }

  return snapshot.excerpts
    .filter((excerpt) => !excerpt.isDeleted && (filter === "all" || excerpt.treeNodeId === null))
    .map((excerpt) => ({
      id: excerpt.id,
      content: excerpt.content,
      sourceTitle: excerpt.sourceTitle,
      url: excerpt.url,
      userThought: excerpt.userThought,
      treeNodeId: excerpt.treeNodeId,
      tags: (excerptTagMap.get(excerpt.id) ?? [])
        .map((relation) => tagMap.get(relation.tagId))
        .filter(isActiveTag)
        .sort((a, b) => b.heatWeight - a.heatWeight),
    }));
}

export function getNodeFeed(snapshot: WorkspaceSnapshot, nodeId: string | null): ExcerptViewModel[] {
  if (!nodeId) {
    return [];
  }
  const tagMap = buildTagMap(snapshot.tags);
  const excerptTagMap = new Map<string, ExcerptTag[]>();
  for (const relation of snapshot.excerptTags) {
    if (relation.isDeleted) {
      continue;
    }
    const list = excerptTagMap.get(relation.excerptId) ?? [];
    list.push(relation);
    excerptTagMap.set(relation.excerptId, list);
  }

  return snapshot.excerpts
    .filter((excerpt) => !excerpt.isDeleted && excerpt.treeNodeId === nodeId)
    .map((excerpt) => ({
      id: excerpt.id,
      content: excerpt.content,
      sourceTitle: excerpt.sourceTitle,
      url: excerpt.url,
      userThought: excerpt.userThought,
      treeNodeId: excerpt.treeNodeId,
      tags: (excerptTagMap.get(excerpt.id) ?? [])
        .map((relation) => tagMap.get(relation.tagId))
        .filter(isActiveTag)
        .sort((a, b) => b.heatWeight - a.heatWeight),
    }));
}

export function getSelectedNode(snapshot: WorkspaceSnapshot, nodeId: string | null): KnowledgeTreeNode | null {
  if (!nodeId) {
    return null;
  }
  return snapshot.nodes.find((node) => node.id === nodeId && !node.isDeleted) ?? null;
}

export function getSelectedExcerpt(snapshot: WorkspaceSnapshot, excerptId: string | null): Excerpt | null {
  if (!excerptId) {
    return null;
  }
  return snapshot.excerpts.find((excerpt) => excerpt.id === excerptId && !excerpt.isDeleted) ?? null;
}

export function searchWorkspace(snapshot: WorkspaceSnapshot, query: string): SearchResult[] {
  const trimmed = query.trim();
  if (!trimmed) {
    return [];
  }
  const lowered = trimmed.toLowerCase();
  const tagQuery = lowered.startsWith("#") ? lowered.slice(1) : null;
  const tagMap = buildTagMap(snapshot.tags);
  const excerptTagMap = new Map<string, ExcerptTag[]>();
  for (const relation of snapshot.excerptTags) {
    if (relation.isDeleted) {
      continue;
    }
    const list = excerptTagMap.get(relation.excerptId) ?? [];
    list.push(relation);
    excerptTagMap.set(relation.excerptId, list);
  }

  const tagMatches = snapshot.tags.filter((tag) => {
    if (tag.isDeleted) {
      return false;
    }
    return tag.tagName.toLowerCase().includes(tagQuery ?? lowered);
  });
  const nodeMatches = snapshot.nodes.filter((node) => !node.isDeleted && node.nodeTitle.toLowerCase().includes(lowered));
  const excerptMatches = snapshot.excerpts.filter((excerpt) => {
    if (excerpt.isDeleted) {
      return false;
    }
    const text = `${excerpt.content} ${excerpt.sourceTitle ?? ""} ${excerpt.userThought ?? ""}`.toLowerCase();
    if (text.includes(lowered)) {
      return true;
    }
    if (tagQuery) {
      const relations = excerptTagMap.get(excerpt.id) ?? [];
      return relations.some((relation) => tagMap.get(relation.tagId)?.tagName.toLowerCase().includes(tagQuery));
    }
    return false;
  });

  const results: SearchResult[] = [
    ...nodeMatches.map((node) => ({
      type: "node" as const,
      id: node.id,
      title: node.nodeTitle,
      subtitle: node.outlineMarkdown.trim() ? "含有大纲" : "知识节点",
      targetNodeId: node.id,
    })),
    ...excerptMatches.map((excerpt) => ({
      type: "excerpt" as const,
      id: excerpt.id,
      title: excerpt.sourceTitle ?? excerpt.url ?? excerpt.content.slice(0, 32),
      subtitle: excerpt.treeNodeId ? "已挂载摘录" : "收件箱摘录",
      targetNodeId: excerpt.treeNodeId,
      excerptId: excerpt.id,
    })),
    ...tagMatches.map((tag) => ({
      type: "tag" as const,
      id: tag.id,
      title: `#${tag.tagName}`,
      subtitle: `热度 ${tag.heatWeight}`,
      tagId: tag.id,
      targetNodeId:
        snapshot.excerpts.find((excerpt) =>
          (excerptTagMap.get(excerpt.id) ?? []).some((relation) => relation.tagId === tag.id)
        )?.treeNodeId ?? null,
    })),
  ];

  return results.slice(0, 12);
}
