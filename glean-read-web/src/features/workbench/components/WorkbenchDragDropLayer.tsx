import { type ReactNode, useCallback, useMemo, useState } from "react";
import {
  closestCenter,
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  TouchSensor,
  useSensor,
  useSensors,
  type DragCancelEvent,
  type DragEndEvent,
  type DragOverEvent,
  type DragStartEvent,
} from "@dnd-kit/core";
import { useAuth } from "@/app/providers/AuthProvider";
import { moveExcerptToNode } from "@/db/repositories/workspaceRepository";
import { getInboxExcerpts } from "@/features/workbench/workbenchSelectors";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";
import { isExcerptDragData, isTreeNodeDropData } from "@/features/workbench/dnd";
import { Badge } from "@/shared/components";
import type { ExcerptViewModel, WorkspaceSnapshot } from "@/shared/models";

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

export function WorkbenchDragDropLayer({ children }: { children: ReactNode }) {
  const { session, refreshWorkspace } = useAuth();
  const nodes = useWorkbenchStore((state) => state.nodes);
  const excerpts = useWorkbenchStore((state) => state.excerpts);
  const tags = useWorkbenchStore((state) => state.tags);
  const excerptTags = useWorkbenchStore((state) => state.excerptTags);
  const recentSearches = useWorkbenchStore((state) => state.recentSearches);
  const setHoveredNodeId = useWorkbenchStore((state) => state.setHoveredNodeId);
  const setSelectedNodeId = useWorkbenchStore((state) => state.setSelectedNodeId);
  const [activeExcerptId, setActiveExcerptId] = useState<string | null>(null);

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

  const clearDragState = useCallback(() => {
    setActiveExcerptId(null);
    setHoveredNodeId(null);
  }, [setHoveredNodeId]);

  const handleDragStart = useCallback(
    (event: DragStartEvent) => {
      if (!isExcerptDragData(event.active.data.current)) {
        return;
      }
      setActiveExcerptId(event.active.data.current.excerptId);
      setHoveredNodeId(null);
    },
    [setHoveredNodeId]
  );

  const handleDragOver = useCallback(
    (event: DragOverEvent) => {
      if (!isExcerptDragData(event.active.data.current)) {
        return;
      }
      const nextNodeId = isTreeNodeDropData(event.over?.data.current) ? event.over.data.current.nodeId : null;
      setHoveredNodeId(nextNodeId);
    },
    [setHoveredNodeId]
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
      const nodeId = isTreeNodeDropData(overData) ? overData.nodeId : null;

      if (excerptId && nodeId) {
        const userId = session?.userId ?? null;
        if (userId) {
          const excerpt = excerpts.find((item) => item.id === excerptId && !item.isDeleted);
          if (excerpt?.treeNodeId !== nodeId) {
            await moveExcerptToNode(userId, excerptId, nodeId);
            await refreshWorkspace();
          }
        }
        setSelectedNodeId(nodeId);
      }

      clearDragState();
    },
    [clearDragState, excerpts, refreshWorkspace, session?.userId, setSelectedNodeId]
  );

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
    useSensor(TouchSensor, { activationConstraint: { delay: 120, tolerance: 8 } }),
    useSensor(KeyboardSensor)
  );

  return (
    <DndContext
      collisionDetection={closestCenter}
      sensors={sensors}
      onDragStart={handleDragStart}
      onDragOver={handleDragOver}
      onDragCancel={handleDragCancel}
      onDragEnd={handleDragEnd}
    >
      {children}
      <DragOverlay dropAnimation={null}>{activeExcerpt ? <ExcerptDragPreview excerpt={activeExcerpt} /> : null}</DragOverlay>
    </DndContext>
  );
}
