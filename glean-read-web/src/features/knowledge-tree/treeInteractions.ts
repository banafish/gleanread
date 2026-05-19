import type { TreeNodeDropIntent } from "@/features/workbench/dnd";
import type { KnowledgeTreeNode } from "../../shared/models.ts";
import { isDescendant, sortByOrderAndTime } from "../../shared/utils.ts";

export interface NodeDropPlacement {
  parentId: string | null;
  index: number;
  expandParentId: string | null;
}

function getActiveNodes(nodes: KnowledgeTreeNode[]): KnowledgeTreeNode[] {
  return nodes.filter((node) => !node.isDeleted);
}

function getOrderedSiblings(nodes: KnowledgeTreeNode[], parentId: string | null): KnowledgeTreeNode[] {
  return sortByOrderAndTime(getActiveNodes(nodes).filter((node) => node.parentId === parentId));
}

export function getParentNavigationTarget(nodes: KnowledgeTreeNode[], nodeId: string): string | null {
  return getActiveNodes(nodes).find((node) => node.id === nodeId)?.parentId ?? null;
}

export function getFirstChildNavigationTarget(nodes: KnowledgeTreeNode[], nodeId: string): string | null {
  return getOrderedSiblings(nodes, nodeId)[0]?.id ?? null;
}

export function getSiblingNavigationTarget(
  nodes: KnowledgeTreeNode[],
  nodeId: string,
  direction: "up" | "down"
): string | null {
  const node = getActiveNodes(nodes).find((item) => item.id === nodeId);
  if (!node) {
    return null;
  }
  const siblings = getOrderedSiblings(nodes, node.parentId);
  const currentIndex = siblings.findIndex((item) => item.id === nodeId);
  const nextIndex = direction === "up" ? currentIndex - 1 : currentIndex + 1;
  return nextIndex >= 0 && nextIndex < siblings.length ? siblings[nextIndex].id : null;
}

export function resolveTreeNodeDropPlacement(
  nodes: KnowledgeTreeNode[],
  activeNodeId: string,
  targetNodeId: string,
  intent: TreeNodeDropIntent,
  virtualRootId: string
): NodeDropPlacement | null {
  const activeNodes = getActiveNodes(nodes);
  const activeNode = activeNodes.find((node) => node.id === activeNodeId);
  if (!activeNode) {
    return null;
  }

  if (targetNodeId === virtualRootId) {
    if (intent !== "inside") {
      return null;
    }
    return {
      parentId: null,
      index: getOrderedSiblings(activeNodes, null).filter((node) => node.id !== activeNodeId).length,
      expandParentId: null,
    };
  }

  const targetNode = activeNodes.find((node) => node.id === targetNodeId);
  if (!targetNode || targetNode.id === activeNode.id || isDescendant(activeNodes, activeNode.id, targetNode.id)) {
    return null;
  }

  if (intent === "inside") {
    return {
      parentId: targetNode.id,
      index: getOrderedSiblings(activeNodes, targetNode.id).filter((node) => node.id !== activeNodeId).length,
      expandParentId: targetNode.id,
    };
  }

  const siblings = getOrderedSiblings(activeNodes, targetNode.parentId).filter((node) => node.id !== activeNodeId);
  const targetIndex = siblings.findIndex((node) => node.id === targetNode.id);
  if (targetIndex < 0) {
    return null;
  }

  return {
    parentId: targetNode.parentId,
    index: intent === "before" ? targetIndex : targetIndex + 1,
    expandParentId: targetNode.parentId,
  };
}
