import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
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
import type { WorkspaceSnapshot } from "@/shared/models";

const NEW_NODE_TITLE = "新节点";

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
  const viewport = useWorkbenchStore((state) => state.viewport);
  const searchOpen = useWorkbenchStore((state) => state.searchOpen);
  const drawerOpen = useWorkbenchStore((state) => state.drawerOpen);
  const setSelectedNodeId = useWorkbenchStore((state) => state.setSelectedNodeId);
  const setDrawerOpen = useWorkbenchStore((state) => state.setDrawerOpen);
  const toggleNodeExpanded = useWorkbenchStore((state) => state.toggleNodeExpanded);
  const setNodeExpanded = useWorkbenchStore((state) => state.setNodeExpanded);
  const setViewport = useWorkbenchStore((state) => state.setViewport);
  const [editingNodeId, setEditingNodeId] = useState<string | null>(null);
  const createPendingRef = useRef(false);
  const nodeTypes = useMemo<NodeTypes>(() => ({ knowledgeTreeNode: TreeNodeCard }), []);
  const edgeTypes = useMemo<EdgeTypes>(() => ({ knowledgeTreeEdge: KnowledgeTreeEdge }), []);

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
    <div className="knowledge-tree-canvas h-full min-h-0 overflow-hidden" data-testid="knowledge-tree-canvas">
      <ReactFlow
        className="knowledge-tree-flow"
        nodes={graphData.nodes}
        edges={graphData.edges}
        edgeTypes={edgeTypes}
        nodeTypes={nodeTypes}
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
