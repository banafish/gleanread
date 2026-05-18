import type { KnowledgeTreeNode, SyncStatus } from "@/shared/models";

export function cx(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(" ");
}

export function now(): number {
  return Date.now();
}

export function createId(prefix: string): string {
  return `${prefix}-${crypto.randomUUID()}`;
}

export function trimOrNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

export function bumpSyncStatus(status: SyncStatus): SyncStatus {
  if (status === "failed" || status === "conflict") {
    return status;
  }
  return "pending";
}

export function sortByOrderAndTime<T extends { sortOrder: number; createTime: number }>(items: T[]): T[] {
  return [...items].sort((a, b) => (a.sortOrder - b.sortOrder) || (a.createTime - b.createTime));
}

export function isDescendant(nodes: KnowledgeTreeNode[], nodeId: string, targetId: string): boolean {
  const map = new Map(nodes.map((node) => [node.id, node]));
  let current = map.get(targetId);
  while (current?.parentId) {
    if (current.parentId === nodeId) {
      return true;
    }
    current = map.get(current.parentId);
  }
  return false;
}

export function getNodeDepth(nodes: KnowledgeTreeNode[], nodeId: string): number {
  const map = new Map(nodes.map((node) => [node.id, node]));
  let depth = 0;
  let current = map.get(nodeId);
  while (current?.parentId) {
    depth += 1;
    current = map.get(current.parentId);
  }
  return depth;
}

export function getAncestors(nodes: KnowledgeTreeNode[], nodeId: string): KnowledgeTreeNode[] {
  const map = new Map(nodes.map((node) => [node.id, node]));
  const ancestors: KnowledgeTreeNode[] = [];
  let current = map.get(nodeId);
  while (current?.parentId) {
    const parent = map.get(current.parentId);
    if (!parent) {
      break;
    }
    ancestors.unshift(parent);
    current = parent;
  }
  return ancestors;
}

export function getSubtreeIds(nodes: KnowledgeTreeNode[], nodeId: string): string[] {
  const children = new Map<string | null, KnowledgeTreeNode[]>();
  for (const node of nodes) {
    const list = children.get(node.parentId) ?? [];
    list.push(node);
    children.set(node.parentId, list);
  }
  const result: string[] = [];
  const stack = [nodeId];
  while (stack.length > 0) {
    const current = stack.pop()!;
    result.push(current);
    for (const child of children.get(current) ?? []) {
      stack.push(child.id);
    }
  }
  return result;
}
