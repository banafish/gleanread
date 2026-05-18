import { useCallback, useEffect, useMemo, useRef } from "react";
import ReactFlow, {
  Background,
  BackgroundVariant,
  Controls,
  MiniMap,
  ReactFlowProvider,
  useReactFlow,
  type NodeTypes,
  type Viewport,
} from "reactflow";
import {
  createChildNode,
  createSiblingNode,
  deleteNodeSubtree,
  renameNode,
  reorderNodeSibling,
} from "@/db/repositories/workspaceRepository";
import { useAuth } from "@/app/providers/AuthProvider";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";
import { buildKnowledgeGraph, VIRTUAL_ROOT_ID } from "@/features/knowledge-tree/treeAdapters";
import { TreeNodeCard } from "@/features/knowledge-tree/components/TreeNodeCard";
import type { WorkspaceSnapshot } from "@/shared/models";

const nodeTypes: NodeTypes = {
  knowledgeTreeNode: TreeNodeCard,
};

function shouldSkipHotkeys(event: KeyboardEvent): boolean {
  const target = event.target as HTMLElement | null;
  if (!target) {
    return false;
  }
  const tagName = target.tagName.toLowerCase();
  return tagName === "input" || tagName === "textarea" || tagName === "select" || target.isContentEditable;
}

function GraphViewportController({
  selectedNodeId,
  visibleNodeIds,
}: {
  selectedNodeId: string | null;
  visibleNodeIds: string[];
}) {
  const { fitView, getNode, setCenter } = useReactFlow();
  const didInitialFit = useRef(false);

  useEffect(() => {
    if (didInitialFit.current || visibleNodeIds.length === 0) {
      return;
    }
    didInitialFit.current = true;
    const frame = requestAnimationFrame(() => {
      void fitView({ padding: 0.2, duration: 240 });
    });
    return () => cancelAnimationFrame(frame);
  }, [fitView, visibleNodeIds.length]);

  useEffect(() => {
    if (!selectedNodeId) {
      return;
    }
    const frame = requestAnimationFrame(() => {
      const node = getNode(selectedNodeId);
      if (!node) {
        return;
      }
      const width = node.width ?? 236;
      const height = node.height ?? 92;
      setCenter(node.position.x + width / 2, node.position.y + height / 2, {
        zoom: 1.06,
        duration: 240,
      });
    });
    return () => cancelAnimationFrame(frame);
  }, [getNode, selectedNodeId, setCenter, visibleNodeIds]);

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
  const viewport = useWorkbenchStore((state) => state.viewport);
  const searchOpen = useWorkbenchStore((state) => state.searchOpen);
  const drawerOpen = useWorkbenchStore((state) => state.drawerOpen);
  const setSelectedNodeId = useWorkbenchStore((state) => state.setSelectedNodeId);
  const setDrawerOpen = useWorkbenchStore((state) => state.setDrawerOpen);
  const toggleNodeExpanded = useWorkbenchStore((state) => state.toggleNodeExpanded);
  const setViewport = useWorkbenchStore((state) => state.setViewport);

  const snapshot = useMemo<WorkspaceSnapshot>(
    () => ({ nodes, excerpts, tags, excerptTags, recentSearches }),
    [excerptTags, excerpts, nodes, recentSearches, tags]
  );

  const promptTitle = useCallback((fallback: string): string | null => {
    const value = window.prompt("节点标题", fallback);
    const title = value?.trim();
    return title ? title : null;
  }, []);

  const handleAddChild = useCallback(
    async (parentId: string | null) => {
      if (!userId) {
        return;
      }
      const title = promptTitle("新节点");
      if (!title) {
        return;
      }
      const node = await createChildNode(userId, parentId, title);
      if (parentId && !useWorkbenchStore.getState().expandedNodeIds[parentId]) {
        useWorkbenchStore.getState().toggleNodeExpanded(parentId);
      }
      await refreshWorkspace();
      setSelectedNodeId(node.id);
    },
    [promptTitle, refreshWorkspace, setSelectedNodeId, userId]
  );

  const handleAddSibling = useCallback(
    async (nodeId: string) => {
      if (!userId) {
        return;
      }
      const title = promptTitle("同级节点");
      if (!title) {
        return;
      }
      const node = await createSiblingNode(userId, nodeId, title);
      await refreshWorkspace();
      if (node) {
        setSelectedNodeId(node.id);
      }
    },
    [promptTitle, refreshWorkspace, setSelectedNodeId, userId]
  );

  const handleRename = useCallback(
    async (nodeId: string) => {
      if (!userId) {
        return;
      }
      const node = nodes.find((item) => item.id === nodeId);
      const title = promptTitle(node?.nodeTitle ?? "节点");
      if (!title) {
        return;
      }
      await renameNode(userId, nodeId, title);
      await refreshWorkspace();
    },
    [nodes, promptTitle, refreshWorkspace, userId]
  );

  const handleDelete = useCallback(
    async (nodeId: string) => {
      if (!userId) {
        return;
      }
      const node = nodes.find((item) => item.id === nodeId);
      const confirmed = window.confirm(`确认删除「${node?.nodeTitle ?? "该节点"}」及其子节点吗？`);
      if (!confirmed) {
        return;
      }
      await deleteNodeSubtree(userId, nodeId);
      await refreshWorkspace();
      setSelectedNodeId(node?.parentId ?? null);
    },
    [nodes, refreshWorkspace, setSelectedNodeId, userId]
  );

  const handleMoveSibling = useCallback(
    async (nodeId: string, direction: "up" | "down") => {
      if (!userId) {
        return;
      }
      await reorderNodeSibling(userId, nodeId, direction);
      await refreshWorkspace();
      setSelectedNodeId(nodeId);
    },
    [refreshWorkspace, setSelectedNodeId, userId]
  );

  const graphData = useMemo(
    () =>
      buildKnowledgeGraph(snapshot, expandedNodeIds, {
        selectedNodeId,
        hoveredNodeId,
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
        onAddChild: handleAddChild,
        onAddSibling: handleAddSibling,
        onMoveSibling: handleMoveSibling,
        onRename: handleRename,
        onDelete: handleDelete,
      }),
    [
      expandedNodeIds,
      handleAddChild,
      handleAddSibling,
      handleDelete,
      handleMoveSibling,
      handleRename,
      hoveredNodeId,
      selectedNodeId,
      setSelectedNodeId,
      snapshot,
      toggleNodeExpanded,
    ]
  );

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (searchOpen || event.metaKey || event.ctrlKey || event.altKey || shouldSkipHotkeys(event)) {
        return;
      }

      const selected = useWorkbenchStore.getState().selectedNodeId;
      const currentIndex = selected ? graphData.visibleNodeIds.indexOf(selected) : -1;
      const selectByIndex = (index: number) => {
        const nextId = graphData.visibleNodeIds[index];
        if (nextId) {
          setSelectedNodeId(nextId);
        }
      };

      if (event.key === "Tab") {
        event.preventDefault();
        void handleAddChild(selected ?? null);
        return;
      }
      if (event.key === "Enter") {
        event.preventDefault();
        if (selected) {
          void handleAddSibling(selected);
        } else {
          void handleAddChild(null);
        }
        return;
      }
      if (event.shiftKey && selected && (event.key === "ArrowUp" || event.key === "ArrowDown")) {
        event.preventDefault();
        void handleMoveSibling(selected, event.key === "ArrowUp" ? "up" : "down");
        return;
      }
      if (event.key === " ") {
        event.preventDefault();
        setDrawerOpen(!drawerOpen);
        return;
      }
      if (event.key === "ArrowUp" || event.key === "ArrowLeft") {
        event.preventDefault();
        selectByIndex(Math.max(0, currentIndex - 1));
        return;
      }
      if (event.key === "ArrowDown" || event.key === "ArrowRight") {
        event.preventDefault();
        selectByIndex(Math.min(graphData.visibleNodeIds.length - 1, currentIndex + 1));
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [
    drawerOpen,
    graphData.visibleNodeIds,
    handleAddChild,
    handleAddSibling,
    handleMoveSibling,
    searchOpen,
    setDrawerOpen,
    setSelectedNodeId,
  ]);

  return (
    <div className="h-full min-h-0 overflow-hidden bg-app-bg">
      <ReactFlow
        nodes={graphData.nodes}
        edges={graphData.edges}
        nodeTypes={nodeTypes}
        defaultViewport={viewport}
        minZoom={0.35}
        maxZoom={1.7}
        fitView={false}
        nodesDraggable={false}
        zoomOnScroll
        panOnScroll
        panOnDrag
        onMoveEnd={(_, nextViewport: Viewport) => setViewport(nextViewport)}
      >
        <Background color="rgb(var(--app-border))" gap={24} variant={BackgroundVariant.Dots} />
        <MiniMap
          pannable
          zoomable
          className="!rounded-panel !border !border-app-border !bg-app-surface"
          nodeStrokeColor="rgb(var(--app-accent))"
          nodeColor="rgb(var(--app-surface-2))"
          maskColor="rgb(var(--app-bg) / 0.64)"
        />
        <Controls className="!rounded-panel !border !border-app-border !bg-app-surface !shadow-panel" />
        <GraphViewportController selectedNodeId={selectedNodeId} visibleNodeIds={graphData.visibleNodeIds} />
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
