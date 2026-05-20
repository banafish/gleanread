import { type ReactNode, useCallback, useMemo, useRef, useState } from "react";
import {
  closestCenter,
  DndContext,
  DragOverlay,
  MeasuringStrategy,
  pointerWithin,
  PointerSensor,
  TouchSensor,
  useSensor,
  useSensors,
  type CollisionDetection,
  type DragCancelEvent,
  type DragEndEvent,
  type DragMoveEvent,
  type DragOverEvent,
  type DragStartEvent,
} from "@dnd-kit/core";
import { useAuth } from "@/app/providers/AuthProvider";
import { moveExcerptToNode, moveNodeToPosition } from "@/db/repositories/workspaceRepository";
import { VIRTUAL_ROOT_ID } from "@/features/knowledge-tree/treeAdapters";
import { resolveTreeNodeDropPlacement } from "@/features/knowledge-tree/treeInteractions";
import { getInboxExcerpts } from "@/features/workbench/workbenchSelectors";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";
import { isExcerptDragData, isTreeNodeDragData, isTreeNodeDropData } from "@/features/workbench/dnd";
import { Badge } from "@/shared/components";
import type { ExcerptViewModel, WorkspaceSnapshot } from "@/shared/models";

interface PointerPosition {
  x: number;
  y: number;
}

function getPointerPosition(event: Event): PointerPosition | null {
  if (event instanceof MouseEvent) {
    return { x: event.clientX, y: event.clientY };
  }
  if (typeof TouchEvent !== "undefined" && event instanceof TouchEvent) {
    const touch = event.touches[0] ?? event.changedTouches[0];
    return touch ? { x: touch.clientX, y: touch.clientY } : null;
  }
  return null;
}

function ExcerptDragPreview({ excerpt }: { excerpt: ExcerptViewModel }) {
  return (
    <article className="w-[320px] rounded-panel border border-app-border bg-app-bg p-3 shadow-2xl ring-1 ring-app-accent/10">
      <p className="line-clamp-4 text-sm leading-6 text-app-text">{excerpt.content}</p>
      <div className="mt-3 text-xs text-app-muted">{excerpt.sourceTitle ?? excerpt.url ?? "未记录来源"}</div>
      {excerpt.userThought ? (
        <div className="mt-3 rounded-lg bg-app-surface2 px-3 py-2 text-xs leading-5 text-app-muted">{excerpt.userThought}</div>
      ) : null}
      <div className="mt-3 flex flex-wrap items-center gap-1.5">
        {excerpt.tags.map((tag) => (
          <Badge key={tag.id} className="gap-1">
            <span>{tag.colorIcon ?? "●"}</span>
            {tag.tagName}
          </Badge>
        ))}
        {excerpt.treeNodeId ? <Badge>已挂载</Badge> : null}
      </div>
    </article>
  );
}

function TreeNodeDragPreview({ title, zoom }: { title: string; zoom: number }) {
  return (
    <article
      className="flex h-[112px] w-[500px] origin-top-left items-center rounded-[28px] border-[3px] border-app-border bg-app-surface px-11 text-app-text opacity-95 shadow-panel"
      style={{ transform: `scale(${zoom})` }}
    >
      <div className="line-clamp-2 text-[32px] font-semibold leading-[1.18]">{title}</div>
    </article>
  );
}

const collisionDetection: CollisionDetection = (args) => {
  const pointerCollisions = pointerWithin(args);
  return pointerCollisions.length > 0 ? pointerCollisions : closestCenter(args);
};

export function WorkbenchDragDropLayer({ children }: { children: ReactNode }) {
  const { session, refreshWorkspace } = useAuth();
  const nodes = useWorkbenchStore((state) => state.nodes);
  const excerpts = useWorkbenchStore((state) => state.excerpts);
  const tags = useWorkbenchStore((state) => state.tags);
  const excerptTags = useWorkbenchStore((state) => state.excerptTags);
  const recentSearches = useWorkbenchStore((state) => state.recentSearches);
  const setHoveredNodeId = useWorkbenchStore((state) => state.setHoveredNodeId);
  const setSelectedNodeId = useWorkbenchStore((state) => state.setSelectedNodeId);
  const setDraggedNodeId = useWorkbenchStore((state) => state.setDraggedNodeId);
  const setDragPointer = useWorkbenchStore((state) => state.setDragPointer);
  const setNodeDropPreview = useWorkbenchStore((state) => state.setNodeDropPreview);
  const setNodeExpanded = useWorkbenchStore((state) => state.setNodeExpanded);
  const viewportZoom = useWorkbenchStore((state) => state.viewport.zoom);
  const [activeExcerptId, setActiveExcerptId] = useState<string | null>(null);
  const [activeTreeNodeId, setActiveTreeNodeId] = useState<string | null>(null);
  const dragStartPointerRef = useRef<PointerPosition | null>(null);

  const snapshot = useMemo<WorkspaceSnapshot>(
    () => ({ nodes, excerpts, tags, excerptTags, recentSearches }),
    [excerptTags, excerpts, nodes, recentSearches, tags]
  );

  const activeExcerpt = useMemo(() => {
    if (!activeExcerptId) {
      return null;
    }
    return getInboxExcerpts(snapshot, "all").find((item) => item.id === activeExcerptId) ?? null;
  }, [activeExcerptId, snapshot]);

  const activeTreeNode = useMemo(() => {
    if (!activeTreeNodeId) {
      return null;
    }
    return nodes.find((node) => node.id === activeTreeNodeId && !node.isDeleted) ?? null;
  }, [activeTreeNodeId, nodes]);

  const clearDragState = useCallback(() => {
    setActiveExcerptId(null);
    setActiveTreeNodeId(null);
    dragStartPointerRef.current = null;
    setDraggedNodeId(null);
    setDragPointer(null);
    setHoveredNodeId(null);
    setNodeDropPreview(null);
  }, [setDragPointer, setDraggedNodeId, setHoveredNodeId, setNodeDropPreview]);

  const handleDragStart = useCallback(
    (event: DragStartEvent) => {
      const activeData = event.active.data.current;
      if (isExcerptDragData(activeData)) {
        setActiveExcerptId(activeData.excerptId);
        setActiveTreeNodeId(null);
        dragStartPointerRef.current = null;
        setDragPointer(null);
        setDraggedNodeId(null);
        setHoveredNodeId(null);
        setNodeDropPreview(null);
        return;
      }
      if (isTreeNodeDragData(activeData)) {
        const pointer = getPointerPosition(event.activatorEvent);
        setActiveExcerptId(null);
        setActiveTreeNodeId(activeData.nodeId);
        dragStartPointerRef.current = pointer;
        setDragPointer(pointer);
        setDraggedNodeId(activeData.nodeId);
        setHoveredNodeId(null);
        setNodeDropPreview(null);
        setSelectedNodeId(activeData.nodeId);
      }
    },
    [setDragPointer, setDraggedNodeId, setHoveredNodeId, setNodeDropPreview, setSelectedNodeId]
  );

  const handleDragMove = useCallback(
    (event: DragMoveEvent) => {
      const activeData = event.active.data.current;
      if (!isTreeNodeDragData(activeData)) {
        setDragPointer(null);
        return;
      }
      const startPointer = dragStartPointerRef.current ?? getPointerPosition(event.activatorEvent);
      dragStartPointerRef.current = startPointer;
      setDragPointer(startPointer ? { x: startPointer.x + event.delta.x, y: startPointer.y + event.delta.y } : null);
    },
    [setDragPointer]
  );

  const handleDragOver = useCallback(
    (event: DragOverEvent) => {
      const activeData = event.active.data.current;
      const overData = event.over?.data.current;
      if (isExcerptDragData(activeData)) {
        const nextNodeId = isTreeNodeDropData(overData) && overData.nodeId !== VIRTUAL_ROOT_ID ? overData.nodeId : null;
        setHoveredNodeId(nextNodeId);
        setNodeDropPreview(null);
        return;
      }
      if (isTreeNodeDragData(activeData) && isTreeNodeDropData(overData)) {
        const placement = resolveTreeNodeDropPlacement(
          nodes,
          activeData.nodeId,
          overData.nodeId,
          overData.intent,
          VIRTUAL_ROOT_ID
        );
        setNodeDropPreview(placement ? { nodeId: overData.nodeId, intent: overData.intent, placement } : null);
        setHoveredNodeId(null);
        return;
      }
      setHoveredNodeId(null);
      setNodeDropPreview(null);
    },
    [nodes, setHoveredNodeId, setNodeDropPreview]
  );

  const handleDragCancel = useCallback(
    (_event: DragCancelEvent) => {
      clearDragState();
    },
    [clearDragState]
  );

  const handleDragEnd = useCallback(
    async (event: DragEndEvent) => {
      const activeData = event.active.data.current;
      const overData = event.over?.data.current;
      const excerptId = isExcerptDragData(activeData) ? activeData.excerptId : null;
      const dropData = isTreeNodeDropData(overData) ? overData : null;
      const droppedNodeId = dropData?.nodeId === VIRTUAL_ROOT_ID ? null : dropData?.nodeId ?? null;

      if (excerptId && dropData) {
        const userId = session?.userId ?? null;
        if (userId) {
          const excerpt = excerpts.find((item) => item.id === excerptId && !item.isDeleted);
          if (excerpt?.treeNodeId !== droppedNodeId) {
            await moveExcerptToNode(userId, excerptId, droppedNodeId);
            await refreshWorkspace();
          }
        }
        setSelectedNodeId(droppedNodeId);
      }

      if (isTreeNodeDragData(activeData) && dropData) {
        const userId = session?.userId ?? null;
        const placement = resolveTreeNodeDropPlacement(nodes, activeData.nodeId, dropData.nodeId, dropData.intent, VIRTUAL_ROOT_ID);
        if (userId && placement) {
          const didMove = await moveNodeToPosition(userId, activeData.nodeId, placement.parentId, placement.index);
          if (didMove) {
            await refreshWorkspace();
            if (placement.expandParentId) {
              setNodeExpanded(placement.expandParentId, true);
            }
            setSelectedNodeId(activeData.nodeId);
          }
        }
      }

      clearDragState();
    },
    [clearDragState, excerpts, nodes, refreshWorkspace, session?.userId, setNodeExpanded, setSelectedNodeId]
  );

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
    useSensor(TouchSensor, { activationConstraint: { delay: 120, tolerance: 8 } })
  );
  const measuring = useMemo(
    () => ({
      droppable: {
        strategy: MeasuringStrategy.Always,
      },
    }),
    []
  );

  return (
    <DndContext
      collisionDetection={collisionDetection}
      measuring={measuring}
      sensors={sensors}
      onDragStart={handleDragStart}
      onDragMove={handleDragMove}
      onDragOver={handleDragOver}
      onDragCancel={handleDragCancel}
      onDragEnd={handleDragEnd}
    >
      {children}
      <DragOverlay dropAnimation={null}>
        {activeExcerpt ? <ExcerptDragPreview excerpt={activeExcerpt} /> : null}
        {!activeExcerpt && activeTreeNode ? (
          <TreeNodeDragPreview title={activeTreeNode.nodeTitle} zoom={Math.max(0.35, Math.min(1.7, viewportZoom))} />
        ) : null}
      </DragOverlay>
    </DndContext>
  );
}
