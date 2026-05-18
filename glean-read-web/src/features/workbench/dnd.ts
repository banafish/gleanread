export const WORKBENCH_DND_TYPES = {
  excerpt: "excerpt",
  treeNode: "tree-node",
} as const;

export interface ExcerptDragData {
  type: typeof WORKBENCH_DND_TYPES.excerpt;
  excerptId: string;
}

export interface TreeNodeDropData {
  type: typeof WORKBENCH_DND_TYPES.treeNode;
  nodeId: string;
}

export function isExcerptDragData(value: unknown): value is ExcerptDragData {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const candidate = value as Partial<ExcerptDragData>;
  return candidate.type === WORKBENCH_DND_TYPES.excerpt && typeof candidate.excerptId === "string";
}

export function isTreeNodeDropData(value: unknown): value is TreeNodeDropData {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const candidate = value as Partial<TreeNodeDropData>;
  return candidate.type === WORKBENCH_DND_TYPES.treeNode && typeof candidate.nodeId === "string";
}
