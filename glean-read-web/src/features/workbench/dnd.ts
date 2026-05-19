export const WORKBENCH_DND_TYPES = {
  excerpt: "excerpt",
  treeNode: "tree-node",
  treeNodeDropZone: "tree-node-drop-zone",
} as const;

export type TreeNodeDropIntent = "before" | "inside" | "after";

export interface ExcerptDragData {
  type: typeof WORKBENCH_DND_TYPES.excerpt;
  excerptId: string;
}

export interface TreeNodeDragData {
  type: typeof WORKBENCH_DND_TYPES.treeNode;
  nodeId: string;
}

export interface TreeNodeDropData {
  type: typeof WORKBENCH_DND_TYPES.treeNodeDropZone;
  nodeId: string;
  intent: TreeNodeDropIntent;
}

export function isExcerptDragData(value: unknown): value is ExcerptDragData {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const candidate = value as Partial<ExcerptDragData>;
  return candidate.type === WORKBENCH_DND_TYPES.excerpt && typeof candidate.excerptId === "string";
}

export function isTreeNodeDragData(value: unknown): value is TreeNodeDragData {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const candidate = value as Partial<TreeNodeDragData>;
  return candidate.type === WORKBENCH_DND_TYPES.treeNode && typeof candidate.nodeId === "string";
}

export function isTreeNodeDropData(value: unknown): value is TreeNodeDropData {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const candidate = value as Partial<TreeNodeDropData>;
  return (
    candidate.type === WORKBENCH_DND_TYPES.treeNodeDropZone &&
    typeof candidate.nodeId === "string" &&
    (candidate.intent === "before" || candidate.intent === "inside" || candidate.intent === "after")
  );
}
