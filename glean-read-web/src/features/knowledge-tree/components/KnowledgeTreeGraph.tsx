import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState, type RefObject } from "react";
import ReactFlow, {
  Background,
  BackgroundVariant,
  Controls,
  MiniMap,
  ReactFlowProvider,
  useReactFlow,
  type EdgeTypes,
  type NodeTypes,
  type Viewport,
} from "reactflow";
import { useDndContext } from "@dnd-kit/core";
import { createChildNode, createSiblingNode, deleteNodeSubtree, renameNode } from "@/db/repositories/workspaceRepository";
import { useAuth } from "@/app/providers/AuthProvider";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";
import { buildKnowledgeGraph, VIRTUAL_ROOT_ID } from "@/features/knowledge-tree/treeAdapters";
import {
  getFirstChildNavigationTarget,
  getParentNavigationTarget,
  getSiblingNavigationTarget,
} from "@/features/knowledge-tree/treeInteractions";
import { KnowledgeTreeEdge } from "@/features/knowledge-tree/components/KnowledgeTreeEdge";
import { TreeNodeCard } from "@/features/knowledge-tree/components/TreeNodeCard";
import type { KnowledgeGraphDropPreviewPath } from "@/features/knowledge-tree/treeAdapters";
import type { WorkspaceSnapshot } from "@/shared/models";

const NEW_NODE_TITLE = "新节点";
const KNOWLEDGE_NODE_TYPES: NodeTypes = { knowledgeTreeNode: TreeNodeCard };
const KNOWLEDGE_EDGE_TYPES: EdgeTypes = { knowledgeTreeEdge: KnowledgeTreeEdge };
const DRAG_AUTO_PAN_EDGE_SIZE = 96;
const DRAG_AUTO_PAN_MIN_SPEED = 1.5;
const DRAG_AUTO_PAN_MAX_SPEED = 12;

interface DragPointerPosition {
  x: number;
  y: number;
}

function shouldSkipHotkeys(event: KeyboardEvent): boolean {
  const target = event.target as HTMLElement | null;
  if (!target || !(target instanceof HTMLElement)) {
    return false;
  }
  const tagName = target.tagName.toLowerCase();
  return tagName === "input" || tagName === "textarea" || tagName === "select" || target.isContentEditable;
}

function GraphViewportController({
  onViewportChange,
  visibleNodeIds,
}: {
  onViewportChange: (viewport: Viewport) => void;
  visibleNodeIds: string[];
}) {
  const { fitView, getViewport } = useReactFlow();
  const didInitialFit = useRef(false);

  useEffect(() => {
    if (didInitialFit.current || visibleNodeIds.length === 0) {
      return;
    }
    didInitialFit.current = true;
    let timeoutId: number | null = null;
    const frame = requestAnimationFrame(() => {
      void fitView({ padding: 0.2, duration: 240 });
      timeoutId = window.setTimeout(() => {
        onViewportChange(getViewport());
      }, 260);
    });
    return () => {
      cancelAnimationFrame(frame);
      if (timeoutId) {
        window.clearTimeout(timeoutId);
      }
    };
  }, [fitView, getViewport, onViewportChange, visibleNodeIds.length]);

  return null;
}

function getDragAutoPanSpeed(distance: number): number {
  const intensity = Math.min(1, Math.max(0, (DRAG_AUTO_PAN_EDGE_SIZE - distance) / DRAG_AUTO_PAN_EDGE_SIZE));
  return DRAG_AUTO_PAN_MIN_SPEED + intensity * (DRAG_AUTO_PAN_MAX_SPEED - DRAG_AUTO_PAN_MIN_SPEED);
}

function getDragAutoPanDelta(position: number, start: number, end: number): number {
  const startDistance = position - start;
  if (startDistance < DRAG_AUTO_PAN_EDGE_SIZE) {
    return getDragAutoPanSpeed(startDistance);
  }
  const endDistance = end - position;
  if (endDistance < DRAG_AUTO_PAN_EDGE_SIZE) {
    return -getDragAutoPanSpeed(endDistance);
  }
  return 0;
}

function GraphDragAutoPan({
  canvasRef,
  dragPointer,
  draggedNodeId,
  onViewportChange,
}: {
  canvasRef: RefObject<HTMLDivElement | null>;
  dragPointer: DragPointerPosition | null;
  draggedNodeId: string | null;
  onViewportChange: (viewport: Viewport) => void;
}) {
  const { getViewport, setViewport } = useReactFlow();
  const { measureDroppableContainers } = useDndContext();
  const dragPointerRef = useRef<DragPointerPosition | null>(dragPointer);

  useEffect(() => {
    dragPointerRef.current = dragPointer;
  }, [dragPointer]);

  useEffect(() => {
    if (!draggedNodeId) {
      return;
    }

    let frameId = 0;
    const tick = () => {
      const pointer = dragPointerRef.current;
      const canvas = canvasRef.current;
      if (pointer && canvas) {
        const rect = canvas.getBoundingClientRect();
        const deltaX = getDragAutoPanDelta(pointer.x, rect.left, rect.right);
        const deltaY = getDragAutoPanDelta(pointer.y, rect.top, rect.bottom);
        if (deltaX !== 0 || deltaY !== 0) {
          const currentViewport = getViewport();
          const nextViewport = {
            x: currentViewport.x + deltaX,
            y: currentViewport.y + deltaY,
            zoom: currentViewport.zoom,
          };
          setViewport(nextViewport, { duration: 0 });
          onViewportChange(nextViewport);
          // ReactFlow pans with transforms, so dnd-kit needs fresh screen-space drop rects while auto-panning.
          measureDroppableContainers([]);
        }
      }
      frameId = requestAnimationFrame(tick);
    };

    frameId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frameId);
  }, [canvasRef, draggedNodeId, getViewport, measureDroppableContainers, onViewportChange, setViewport]);

  return null;
}

function NodeDropPreviewPath({
  previewPath,
  viewport,
}: {
  previewPath: KnowledgeGraphDropPreviewPath | null;
  viewport: Viewport;
}) {
  if (!previewPath) {
    return null;
  }

  return (
    <svg
      className="pointer-events-none absolute inset-0 z-20 overflow-visible"
      data-testid="tree-node-drop-preview-path"
      style={{
        transform: `translate(${viewport.x}px, ${viewport.y}px) scale(${viewport.zoom})`,
        transformOrigin: "0 0",
      }}
    >
      <path
        d={previewPath.path}
        fill="none"
        stroke="#f97316"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={8}
        vectorEffect="non-scaling-stroke"
      />
      <foreignObject
        data-testid="tree-node-drop-preview-slot"
        x={previewPath.slot.x}
        y={previewPath.slot.y}
        width={previewPath.slot.width}
        height={previewPath.slot.height}
        overflow="visible"
      >
        <div className="relative flex h-full w-full items-center rounded-[24px] border-[3px] border-orange-500 bg-app-surface shadow-panel ring-[4px] ring-orange-500/15">
          <span className="absolute left-[-8px] top-1/2 h-4 w-4 -translate-y-1/2 rounded-full bg-orange-500 shadow-[0_0_0_5px_rgb(249_115_22_/_0.18)]" />
          <span className="absolute right-[-8px] top-1/2 h-4 w-4 -translate-y-1/2 rounded-full bg-orange-500 shadow-[0_0_0_5px_rgb(249_115_22_/_0.18)]" />
          <div className="mx-8 h-3 flex-1 rounded-full bg-orange-500/20" />
        </div>
      </foreignObject>
    </svg>
  );
}

function KnowledgeTreeGraphInner() {
  const { session, refreshWorkspace } = useAuth();
  const userId = session?.userId ?? null;
  const nodes = useWorkbenchStore((state) => state.nodes);
  const excerpts = useWorkbenchStore((state) => state.excerpts);
  const tags = useWorkbenchStore((state) => state.tags);
  const excerptTags = useWorkbenchStore((state) => state.excerptTags);
  const recentSearches = useWorkbenchStore((state) => state.recentSearches);
  const selectedNodeId = useWorkbenchStore((state) => state.selectedNodeId);
  const expandedNodeIds = useWorkbenchStore((state) => state.expandedNodeIds);
  const hoveredNodeId = useWorkbenchStore((state) => state.hoveredNodeId);
  const draggedNodeId = useWorkbenchStore((state) => state.draggedNodeId);
  const nodeDropPreview = useWorkbenchStore((state) => state.nodeDropPreview);
  const dragPointer = useWorkbenchStore((state) => state.dragPointer);
  const viewport = useWorkbenchStore((state) => state.viewport);
  const searchOpen = useWorkbenchStore((state) => state.searchOpen);
  const drawerOpen = useWorkbenchStore((state) => state.drawerOpen);
  const setSelectedNodeId = useWorkbenchStore((state) => state.setSelectedNodeId);
  const setDrawerOpen = useWorkbenchStore((state) => state.setDrawerOpen);
  const toggleNodeExpanded = useWorkbenchStore((state) => state.toggleNodeExpanded);
  const setNodeExpanded = useWorkbenchStore((state) => state.setNodeExpanded);
  const setViewport = useWorkbenchStore((state) => state.setViewport);
  const [editingNodeId, setEditingNodeId] = useState<string | null>(null);
  const canvasRef = useRef<HTMLDivElement | null>(null);
  const createPendingRef = useRef(false);

  const snapshot = useMemo<WorkspaceSnapshot>(
    () => ({ nodes, excerpts, tags, excerptTags, recentSearches }),
    [excerptTags, excerpts, nodes, recentSearches, tags]
  );

  const handleAddChild = useCallback(
    async (parentId: string | null) => {
      if (!userId || createPendingRef.current) {
        return;
      }
      createPendingRef.current = true;
      try {
        const node = await createChildNode(userId, parentId, NEW_NODE_TITLE);
        await refreshWorkspace();
        if (parentId) {
          setNodeExpanded(parentId, true);
        }
        setSelectedNodeId(node.id);
        setEditingNodeId(node.id);
      } finally {
        createPendingRef.current = false;
      }
    },
    [refreshWorkspace, setNodeExpanded, setSelectedNodeId, userId]
  );

  const handleAddSibling = useCallback(
    async (nodeId: string) => {
      if (!userId || createPendingRef.current) {
        return;
      }
      createPendingRef.current = true;
      try {
        const node = await createSiblingNode(userId, nodeId, NEW_NODE_TITLE);
        await refreshWorkspace();
        if (node) {
          setSelectedNodeId(node.id);
          setEditingNodeId(node.id);
        }
      } finally {
        createPendingRef.current = false;
      }
    },
    [refreshWorkspace, setSelectedNodeId, userId]
  );

  const handleCommitTitle = useCallback(
    async (nodeId: string, title: string) => {
      if (!userId) {
        return;
      }
      await renameNode(userId, nodeId, title);
      await refreshWorkspace();
      setEditingNodeId(null);
      setSelectedNodeId(nodeId);
    },
    [refreshWorkspace, setSelectedNodeId, userId]
  );

  const handleDelete = useCallback(
    async (nodeId: string) => {
      if (!userId) {
        return;
      }
      const node = nodes.find((item) => item.id === nodeId);
      await deleteNodeSubtree(userId, nodeId);
      await refreshWorkspace();
      setEditingNodeId(null);
      setSelectedNodeId(node?.parentId ?? null);
    },
    [nodes, refreshWorkspace, setSelectedNodeId, userId]
  );

  const graphData = useMemo(
    () =>
      buildKnowledgeGraph(snapshot, expandedNodeIds, {
        selectedNodeId,
        hoveredNodeId,
        editingNodeId,
        draggedNodeId,
        nodeDropPreview,
        onSelect: (nodeId) => {
          if (nodeId !== VIRTUAL_ROOT_ID) {
            setSelectedNodeId(nodeId);
          }
        },
        onToggleExpanded: (nodeId) => {
          if (nodeId !== VIRTUAL_ROOT_ID) {
            toggleNodeExpanded(nodeId);
          }
        },
        onStartEditing: (nodeId) => setEditingNodeId(nodeId),
        onCancelEditing: () => setEditingNodeId(null),
        onCommitTitle: (nodeId, title) => {
          void handleCommitTitle(nodeId, title);
        },
      }),
    [
      draggedNodeId,
      editingNodeId,
      expandedNodeIds,
      handleCommitTitle,
      hoveredNodeId,
      nodeDropPreview,
      selectedNodeId,
      setSelectedNodeId,
      snapshot,
      toggleNodeExpanded,
    ]
  );

  useLayoutEffect(() => {
    if (!editingNodeId) {
      return;
    }
    let attempts = 0;
    const focusEditor = () => {
      const input = document.querySelector<HTMLInputElement>(`[data-node-edit-input="${editingNodeId}"]`);
      input?.focus();
      input?.select();
      if (input && document.activeElement === input) {
        return true;
      }
      return false;
    };
    const frame = requestAnimationFrame(focusEditor);
    const timer = window.setInterval(() => {
      attempts += 1;
      if (focusEditor() || attempts >= 20) {
        window.clearInterval(timer);
      }
    }, 25);
    void focusEditor();
    return () => {
      cancelAnimationFrame(frame);
      window.clearInterval(timer);
    };
  }, [editingNodeId, graphData.nodes]);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (searchOpen || event.metaKey || event.ctrlKey || event.altKey || shouldSkipHotkeys(event)) {
        return;
      }

      const selected = useWorkbenchStore.getState().selectedNodeId;
      const selectIfPresent = (nextId: string | null) => {
        if (nextId) {
          setSelectedNodeId(nextId);
        }
      };
      const blurActiveElement = () => {
        if (document.activeElement instanceof HTMLElement) {
          document.activeElement.blur();
        }
      };

      if (event.key === "Tab") {
        event.preventDefault();
        blurActiveElement();
        void handleAddChild(selected ?? null);
        return;
      }
      if (event.key === "Enter") {
        event.preventDefault();
        blurActiveElement();
        if (selected) {
          void handleAddSibling(selected);
        } else {
          void handleAddChild(null);
        }
        return;
      }
      if (event.key === "Delete" || event.key === "Backspace") {
        if (selected) {
          event.preventDefault();
          void handleDelete(selected);
        }
        return;
      }
      if (event.key === " ") {
        event.preventDefault();
        setDrawerOpen(!drawerOpen);
        return;
      }
      if (!selected) {
        return;
      }
      if (event.key === "ArrowLeft") {
        event.preventDefault();
        selectIfPresent(getParentNavigationTarget(nodes, selected));
        return;
      }
      if (event.key === "ArrowRight") {
        event.preventDefault();
        const childId = getFirstChildNavigationTarget(nodes, selected);
        if (childId) {
          setNodeExpanded(selected, true);
          setSelectedNodeId(childId);
        }
        return;
      }
      if (event.key === "ArrowUp" || event.key === "ArrowDown") {
        event.preventDefault();
        selectIfPresent(getSiblingNavigationTarget(nodes, selected, event.key === "ArrowUp" ? "up" : "down"));
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [
    drawerOpen,
    handleAddChild,
    handleAddSibling,
    handleDelete,
    nodes,
    searchOpen,
    setDrawerOpen,
    setNodeExpanded,
    setSelectedNodeId,
  ]);

  return (
    <div ref={canvasRef} className="knowledge-tree-canvas h-full min-h-0 overflow-hidden" data-testid="knowledge-tree-canvas">
      <ReactFlow
        className="knowledge-tree-flow"
        nodes={graphData.nodes}
        edges={graphData.edges}
        edgeTypes={KNOWLEDGE_EDGE_TYPES}
        nodeTypes={KNOWLEDGE_NODE_TYPES}
        defaultViewport={viewport}
        minZoom={0.35}
        maxZoom={1.7}
        fitView={false}
        nodesDraggable={false}
        zoomOnScroll
        panOnScroll
        panOnDrag={!draggedNodeId}
        onNodeDoubleClick={(_, node) => {
          if (node.id !== VIRTUAL_ROOT_ID) {
            setSelectedNodeId(node.id);
            setEditingNodeId(node.id);
          }
        }}
        onMoveEnd={(_, nextViewport: Viewport) => setViewport(nextViewport)}
      >
        <Background color="var(--knowledge-dot-color)" gap={56} size={1.5} variant={BackgroundVariant.Dots} />
        <NodeDropPreviewPath previewPath={graphData.dropPreviewPath} viewport={viewport} />
        <MiniMap
          pannable
          zoomable
          className="!rounded-panel !border !border-app-border !bg-app-surface"
          nodeStrokeColor="rgb(var(--app-accent))"
          nodeColor="rgb(var(--app-surface-2))"
          maskColor="rgb(var(--app-bg) / 0.64)"
        />
        <Controls className="!rounded-panel !border !border-app-border !bg-app-surface !shadow-panel" />
        <GraphViewportController onViewportChange={setViewport} visibleNodeIds={graphData.visibleNodeIds} />
        <GraphDragAutoPan
          canvasRef={canvasRef}
          draggedNodeId={draggedNodeId}
          dragPointer={dragPointer}
          onViewportChange={setViewport}
        />
      </ReactFlow>
    </div>
  );
}

export function KnowledgeTreeGraph() {
  return (
    <ReactFlowProvider>
      <KnowledgeTreeGraphInner />
    </ReactFlowProvider>
  );
}
