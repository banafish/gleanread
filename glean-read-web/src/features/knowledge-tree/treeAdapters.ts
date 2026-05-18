import dagre from "dagre";
import type { Edge, Node } from "reactflow";
import { MarkerType } from "reactflow";
import type { TreeNodeViewModel, WorkspaceSnapshot } from "@/shared/models";
import { getNodeViewModels } from "@/features/workbench/workbenchSelectors";
import { sortByOrderAndTime } from "@/shared/utils";

export const VIRTUAL_ROOT_ID = "__virtual_root__";

export interface KnowledgeGraphNodeData {
  viewModel: TreeNodeViewModel;
  hasChildren: boolean;
  isSelected: boolean;
  isHovered: boolean;
  canMoveUp: boolean;
  canMoveDown: boolean;
  onSelect: (nodeId: string) => void;
  onToggleExpanded: (nodeId: string) => void;
  onAddChild: (nodeId: string | null) => void;
  onAddSibling: (nodeId: string) => void;
  onMoveSibling: (nodeId: string, direction: "up" | "down") => void;
  onRename: (nodeId: string) => void;
  onDelete: (nodeId: string) => void;
  onDropExcerpt: (excerptId: string, nodeId: string) => void;
  onHoverNode: (nodeId: string | null) => void;
}

export interface KnowledgeGraphData {
  nodes: Node<KnowledgeGraphNodeData>[];
  edges: Edge[];
  visibleNodeIds: string[];
}

const NODE_WIDTH = 236;
const NODE_HEIGHT = 92;
const ROOT_WIDTH = 184;
const ROOT_HEIGHT = 72;

export function buildKnowledgeGraph(
  snapshot: WorkspaceSnapshot,
  expandedNodeIds: Record<string, boolean>,
  options: Omit<KnowledgeGraphNodeData, "viewModel" | "hasChildren" | "isSelected" | "isHovered" | "canMoveUp" | "canMoveDown"> & {
    selectedNodeId: string | null;
    hoveredNodeId: string | null;
  }
): KnowledgeGraphData {
  const viewModels = getNodeViewModels(snapshot, expandedNodeIds);
  const visibleIds = new Set(viewModels.map((node) => node.id));
  const childCountMap = new Map<string, number>();
  const siblingPositionMap = new Map<string, { canMoveUp: boolean; canMoveDown: boolean }>();
  const siblingsByParent = new Map<string | null, typeof snapshot.nodes>();
  for (const node of snapshot.nodes) {
    const siblings = siblingsByParent.get(node.parentId) ?? [];
    siblings.push(node);
    siblingsByParent.set(node.parentId, siblings);
    if (!node.parentId) {
      childCountMap.set(VIRTUAL_ROOT_ID, (childCountMap.get(VIRTUAL_ROOT_ID) ?? 0) + 1);
      continue;
    }
    childCountMap.set(node.parentId, (childCountMap.get(node.parentId) ?? 0) + 1);
  }
  for (const siblings of siblingsByParent.values()) {
    const ordered = sortByOrderAndTime(siblings);
    ordered.forEach((node, index) => {
      siblingPositionMap.set(node.id, {
        canMoveUp: index > 0,
        canMoveDown: index < ordered.length - 1,
      });
    });
  }

  const graph = new dagre.graphlib.Graph();
  graph.setDefaultEdgeLabel(() => ({}));
  graph.setGraph({
    rankdir: "LR",
    nodesep: 42,
    ranksep: 92,
    marginx: 32,
    marginy: 32,
  });

  for (const viewModel of viewModels) {
    const width = viewModel.isVirtualRoot ? ROOT_WIDTH : NODE_WIDTH;
    const height = viewModel.isVirtualRoot ? ROOT_HEIGHT : NODE_HEIGHT;
    graph.setNode(viewModel.id, { width, height });
  }

  const edges: Edge[] = [];
  for (const viewModel of viewModels) {
    if (viewModel.id === VIRTUAL_ROOT_ID) {
      continue;
    }
    const source = viewModel.parentId ?? VIRTUAL_ROOT_ID;
    if (!visibleIds.has(source)) {
      continue;
    }
    graph.setEdge(source, viewModel.id);
    edges.push({
      id: `${source}->${viewModel.id}`,
      source,
      target: viewModel.id,
      type: "smoothstep",
      animated: false,
      markerEnd: {
        type: MarkerType.ArrowClosed,
        width: 16,
        height: 16,
      },
      style: {
        stroke: "rgb(var(--app-accent) / 0.38)",
        strokeWidth: 2,
      },
    });
  }

  dagre.layout(graph);

  const nodes: Node<KnowledgeGraphNodeData>[] = viewModels.map((viewModel) => {
    const layout = graph.node(viewModel.id);
    const width = viewModel.isVirtualRoot ? ROOT_WIDTH : NODE_WIDTH;
    const height = viewModel.isVirtualRoot ? ROOT_HEIGHT : NODE_HEIGHT;
    return {
      id: viewModel.id,
      type: "knowledgeTreeNode",
      position: {
        x: layout.x - width / 2,
        y: layout.y - height / 2,
      },
      data: {
        viewModel,
        hasChildren: (childCountMap.get(viewModel.id) ?? 0) > 0,
        isSelected: options.selectedNodeId === viewModel.id,
        isHovered: options.hoveredNodeId === viewModel.id,
        canMoveUp: siblingPositionMap.get(viewModel.id)?.canMoveUp ?? false,
        canMoveDown: siblingPositionMap.get(viewModel.id)?.canMoveDown ?? false,
        onSelect: options.onSelect,
        onToggleExpanded: options.onToggleExpanded,
        onAddChild: options.onAddChild,
        onAddSibling: options.onAddSibling,
        onMoveSibling: options.onMoveSibling,
        onRename: options.onRename,
        onDelete: options.onDelete,
        onDropExcerpt: options.onDropExcerpt,
        onHoverNode: options.onHoverNode,
      },
      draggable: false,
    };
  });

  return {
    nodes,
    edges,
    visibleNodeIds: viewModels.filter((node) => !node.isVirtualRoot).map((node) => node.id),
  };
}
