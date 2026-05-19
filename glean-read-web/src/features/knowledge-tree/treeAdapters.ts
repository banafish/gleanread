import type { Edge, Node } from "reactflow";
import type { TreeNodeDropIntent } from "@/features/workbench/dnd";
import type { TreeNodeViewModel, WorkspaceSnapshot } from "@/shared/models";
import { getNodeViewModels } from "@/features/workbench/workbenchSelectors";

export const VIRTUAL_ROOT_ID = "__virtual_root__";

export interface KnowledgeGraphNodeData {
  viewModel: TreeNodeViewModel;
  hasChildren: boolean;
  isSelected: boolean;
  isHovered: boolean;
  isEditing: boolean;
  isDraggingSource: boolean;
  dropIntent: TreeNodeDropIntent | null;
  onSelect: (nodeId: string) => void;
  onToggleExpanded: (nodeId: string) => void;
  onStartEditing: (nodeId: string) => void;
  onCancelEditing: () => void;
  onCommitTitle: (nodeId: string, title: string) => void;
}

export interface KnowledgeGraphData {
  nodes: Node<KnowledgeGraphNodeData>[];
  edges: Edge[];
  visibleNodeIds: string[];
}

const NODE_WIDTH = 500;
const NODE_HEIGHT = 112;
const ROOT_WIDTH = NODE_WIDTH;
const ROOT_HEIGHT = NODE_HEIGHT;
const HORIZONTAL_STEP = NODE_WIDTH + 196;
const ROW_STEP = NODE_HEIGHT + 132;
const MARGIN_X = 64;
const MARGIN_Y = 64;

interface LayoutCenter {
  x: number;
  y: number;
}

function buildVisibleChildrenMap(viewModels: TreeNodeViewModel[]): Map<string, TreeNodeViewModel[]> {
  const childrenByParent = new Map<string, TreeNodeViewModel[]>();
  for (const viewModel of viewModels) {
    if (viewModel.id === VIRTUAL_ROOT_ID) {
      continue;
    }
    const parentId = viewModel.parentId ?? VIRTUAL_ROOT_ID;
    const siblings = childrenByParent.get(parentId) ?? [];
    siblings.push(viewModel);
    childrenByParent.set(parentId, siblings);
  }
  return childrenByParent;
}

function buildAutoLayoutCenters(viewModels: TreeNodeViewModel[]): Map<string, LayoutCenter> {
  const viewModelById = new Map(viewModels.map((viewModel) => [viewModel.id, viewModel]));
  const childrenByParent = buildVisibleChildrenMap(viewModels);
  const centers = new Map<string, LayoutCenter>();
  let nextLeafY = MARGIN_Y + ROOT_HEIGHT / 2;

  const layoutSubtree = (viewModel: TreeNodeViewModel, depth: number): number => {
    const width = viewModel.isVirtualRoot ? ROOT_WIDTH : NODE_WIDTH;
    const children = childrenByParent.get(viewModel.id) ?? [];
    const x = MARGIN_X + depth * HORIZONTAL_STEP + width / 2;
    if (children.length === 0) {
      const y = nextLeafY;
      nextLeafY += ROW_STEP;
      centers.set(viewModel.id, { x, y });
      return y;
    }

    const firstChildY = layoutSubtree(children[0], depth + 1);
    let lastChildY = firstChildY;
    for (const child of children.slice(1)) {
      lastChildY = layoutSubtree(child, depth + 1);
    }

    const y = (firstChildY + lastChildY) / 2;
    centers.set(viewModel.id, { x, y });
    return y;
  };

  const root = viewModelById.get(VIRTUAL_ROOT_ID);
  if (root) {
    layoutSubtree(root, 0);
  }

  for (const viewModel of viewModels) {
    if (!centers.has(viewModel.id)) {
      const width = viewModel.isVirtualRoot ? ROOT_WIDTH : NODE_WIDTH;
      const y = nextLeafY;
      nextLeafY += ROW_STEP;
      centers.set(viewModel.id, {
        x: MARGIN_X + viewModel.depth * HORIZONTAL_STEP + width / 2,
        y,
      });
    }
  }

  return centers;
}

export function buildKnowledgeGraph(
  snapshot: WorkspaceSnapshot,
  expandedNodeIds: Record<string, boolean>,
  options: Pick<
    KnowledgeGraphNodeData,
    "onSelect" | "onToggleExpanded" | "onStartEditing" | "onCancelEditing" | "onCommitTitle"
  > & {
    selectedNodeId: string | null;
    hoveredNodeId: string | null;
    editingNodeId: string | null;
    draggedNodeId: string | null;
    nodeDropPreview: {
      nodeId: string;
      intent: TreeNodeDropIntent;
    } | null;
  }
): KnowledgeGraphData {
  const viewModels = getNodeViewModels(snapshot, expandedNodeIds);
  const visibleIds = new Set(viewModels.map((node) => node.id));
  const childCountMap = new Map<string, number>();
  for (const node of snapshot.nodes) {
    if (!node.parentId) {
      childCountMap.set(VIRTUAL_ROOT_ID, (childCountMap.get(VIRTUAL_ROOT_ID) ?? 0) + 1);
      continue;
    }
    childCountMap.set(node.parentId, (childCountMap.get(node.parentId) ?? 0) + 1);
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
    edges.push({
      id: `${source}->${viewModel.id}`,
      source,
      target: viewModel.id,
      type: "knowledgeTreeEdge",
      animated: false,
      focusable: false,
      interactionWidth: 24,
      style: {
        stroke: "rgb(var(--app-accent) / 0.38)",
        strokeWidth: 5,
      },
    });
  }

  const layoutCenters = buildAutoLayoutCenters(viewModels);

  const nodes: Node<KnowledgeGraphNodeData>[] = viewModels.map((viewModel) => {
    const width = viewModel.isVirtualRoot ? ROOT_WIDTH : NODE_WIDTH;
    const height = viewModel.isVirtualRoot ? ROOT_HEIGHT : NODE_HEIGHT;
    const layout = layoutCenters.get(viewModel.id) ?? {
      x: MARGIN_X + viewModel.depth * HORIZONTAL_STEP + width / 2,
      y: MARGIN_Y + height / 2,
    };
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
        isEditing: options.editingNodeId === viewModel.id,
        isDraggingSource: options.draggedNodeId === viewModel.id,
        dropIntent: options.nodeDropPreview?.nodeId === viewModel.id ? options.nodeDropPreview.intent : null,
        onSelect: options.onSelect,
        onToggleExpanded: options.onToggleExpanded,
        onStartEditing: options.onStartEditing,
        onCancelEditing: options.onCancelEditing,
        onCommitTitle: options.onCommitTitle,
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
