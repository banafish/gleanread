import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
  type MouseEvent,
  type PointerEvent as ReactPointerEvent,
} from "react";
import { useDraggable, useDroppable } from "@dnd-kit/core";
import { ChevronDown, ChevronRight } from "lucide-react";
import { Handle, Position, type NodeProps } from "reactflow";
import { WORKBENCH_DND_TYPES, type TreeNodeDropIntent } from "@/features/workbench/dnd";
import { cx } from "@/shared/utils";
import type { KnowledgeGraphNodeData } from "@/features/knowledge-tree/treeAdapters";

function stop(event: MouseEvent<HTMLButtonElement>) {
  event.stopPropagation();
}

function stopPointer(event: ReactPointerEvent<HTMLButtonElement>) {
  event.stopPropagation();
}

function stopInputKey(event: ReactKeyboardEvent<HTMLInputElement>) {
  event.stopPropagation();
}

function DropZone({
  disabled,
  intent,
  nodeId,
}: {
  disabled: boolean;
  intent: TreeNodeDropIntent;
  nodeId: string;
}) {
  const { setNodeRef } = useDroppable({
    id: `tree-node:${nodeId}:${intent}`,
    disabled,
    data: {
      type: WORKBENCH_DND_TYPES.treeNodeDropZone,
      nodeId,
      intent,
    },
  });

  return (
    <div
      ref={setNodeRef}
      className={cx(
        "pointer-events-none absolute inset-x-0 z-10",
        intent === "before" && "top-0 h-1/3",
        intent === "inside" && "top-1/3 h-1/3",
        intent === "after" && "bottom-0 h-1/3"
      )}
    />
  );
}

export function TreeNodeCard({ data }: NodeProps<KnowledgeGraphNodeData>) {
  const { viewModel } = data;
  const isVirtualRoot = Boolean(viewModel.isVirtualRoot);
  const canToggleChildren = data.hasChildren && !isVirtualRoot;
  const inputRef = useRef<HTMLInputElement | null>(null);
  const skipNextBlurRef = useRef(false);
  const lastClickTimeRef = useRef(0);
  const [draftTitle, setDraftTitle] = useState(viewModel.title);
  const { attributes, isDragging, listeners, setNodeRef } = useDraggable({
    id: `tree-node:${viewModel.id}`,
    disabled: isVirtualRoot || data.isEditing,
    data: {
      type: WORKBENCH_DND_TYPES.treeNode,
      nodeId: viewModel.id,
    },
  });
  const canDrop = !data.isDraggingSource && !data.isEditing;
  const draggableAttributes = !isVirtualRoot && !data.isEditing ? attributes : {};
  const draggableListeners = !isVirtualRoot && !data.isEditing ? listeners : {};
  const handleClass = "!h-4 !w-4 !border-0 !bg-app-accent !shadow-none";
  const isDropTargetActive = data.isHovered || data.dropIntent === "inside";
  const isDragSource = data.isDraggingSource || isDragging;
  const showNodeMeta = !isVirtualRoot;
  const showExcerptCount = showNodeMeta && viewModel.excerptCount > 0;

  useEffect(() => {
    if (!data.isEditing) {
      setDraftTitle(viewModel.title);
    }
  }, [data.isEditing, viewModel.title]);

  useLayoutEffect(() => {
    if (!data.isEditing) {
      return;
    }
    setDraftTitle(viewModel.title);
    const focusTitleInput = () => {
      inputRef.current?.focus();
      inputRef.current?.select();
    };
    focusTitleInput();
    const frame = requestAnimationFrame(focusTitleInput);
    return () => {
      cancelAnimationFrame(frame);
    };
  }, [data.isEditing, viewModel.title]);

  const commitTitle = () => {
    const title = draftTitle.trim();
    if (!title || title === viewModel.title.trim()) {
      data.onCancelEditing();
      return;
    }
    data.onCommitTitle(viewModel.id, title);
  };

  const setEditingInputRef = useCallback((node: HTMLInputElement | null) => {
    inputRef.current = node;
    if (node && data.isEditing) {
      queueMicrotask(() => {
        node.focus();
        node.select();
      });
    }
  }, [data.isEditing]);

  return (
    <div
      ref={setNodeRef}
      className={cx(
        "group relative flex h-[112px] w-[500px] items-center rounded-[28px] border-[3px] bg-app-surface px-11 text-app-text shadow-panel transition",
        "hover:border-app-accent/60 hover:shadow-lg",
        !isVirtualRoot && !data.isEditing && "nopan cursor-grab active:cursor-grabbing",
        data.isSelected && "border-app-accent ring-[3px] ring-app-accent/20",
        isDropTargetActive && "border-app-success ring-[3px] ring-app-success/25",
        isDragSource && "opacity-40",
        !data.isSelected && !isDropTargetActive && "border-app-border"
      )}
      data-testid={`tree-node-${viewModel.id}`}
      {...draggableListeners}
      {...draggableAttributes}
      onPointerDown={(event) => {
        if (!isVirtualRoot && !data.isEditing) {
          listeners?.onPointerDown?.(event);
        }
        event.stopPropagation();
        if (!isVirtualRoot && event.detail >= 2) {
          data.onSelect(viewModel.id);
          data.onStartEditing(viewModel.id);
        }
      }}
      onClick={(event) => {
        const timestamp = Date.now();
        data.onSelect(viewModel.id);
        if (!isVirtualRoot && (event.detail >= 2 || timestamp - lastClickTimeRef.current < 640)) {
          data.onStartEditing(viewModel.id);
        }
        lastClickTimeRef.current = timestamp;
      }}
      onDoubleClick={(event) => {
        if (!isVirtualRoot) {
          event.currentTarget.blur();
          data.onSelect(viewModel.id);
          data.onStartEditing(viewModel.id);
        }
      }}
      style={{ touchAction: "none" }}
    >
      <DropZone disabled={isVirtualRoot || !canDrop} intent="before" nodeId={viewModel.id} />
      <DropZone disabled={!canDrop} intent="inside" nodeId={viewModel.id} />
      <DropZone disabled={isVirtualRoot || !canDrop} intent="after" nodeId={viewModel.id} />

      <Handle type="target" position={Position.Left} className={handleClass} style={{ left: -8 }} />
      {data.dropIntent === "before" ? (
        <div className="pointer-events-none absolute -top-3 left-8 right-8 z-30 h-1.5 rounded-full bg-app-success shadow-[0_0_0_6px_rgb(var(--app-success)_/_0.16)]" />
      ) : null}
      {data.dropIntent === "after" ? (
        <div className="pointer-events-none absolute -bottom-3 left-8 right-8 z-30 h-1.5 rounded-full bg-app-success shadow-[0_0_0_6px_rgb(var(--app-success)_/_0.16)]" />
      ) : null}
      {data.dropIntent === "inside" ? (
        <div className="pointer-events-none absolute -right-16 top-1/2 z-30 h-1.5 w-16 -translate-y-1/2 rounded-full bg-app-success shadow-[0_0_0_6px_rgb(var(--app-success)_/_0.16)]">
          <span className="absolute -right-1.5 top-1/2 h-4 w-4 -translate-y-1/2 rounded-full border-[3px] border-app-success bg-app-surface" />
        </div>
      ) : null}

      <div className="relative z-20 min-w-0 flex-1 pr-6">
        {data.isEditing ? (
          <input
            ref={setEditingInputRef}
            autoFocus
            data-node-edit-input={viewModel.id}
            className="w-full rounded-xl border border-app-accent/60 bg-app-bg px-3 py-2 text-[30px] font-semibold leading-[1.18] text-app-text outline-none ring-4 ring-app-accent/15"
            value={draftTitle}
            aria-label="编辑节点名称"
            onChange={(event) => setDraftTitle(event.target.value)}
            onFocus={(event) => event.currentTarget.select()}
            onBlur={() => {
              if (skipNextBlurRef.current) {
                skipNextBlurRef.current = false;
                return;
              }
              commitTitle();
            }}
            onKeyDown={(event) => {
              stopInputKey(event);
              if (event.key === "Enter") {
                event.preventDefault();
                inputRef.current?.blur();
                return;
              }
              if (event.key === "Escape") {
                event.preventDefault();
                skipNextBlurRef.current = true;
                setDraftTitle(viewModel.title);
                data.onCancelEditing();
              }
            }}
          />
        ) : (
          <div className="line-clamp-2 text-[32px] font-semibold leading-[1.18] text-current">{viewModel.title}</div>
        )}
      </div>

      {showNodeMeta ? (
        <div className="relative z-20 flex shrink-0 items-center gap-2 text-[22px]" aria-hidden={false}>
          <span
            data-testid={`tree-node-outline-${viewModel.id}`}
            className={cx(
              "inline-flex h-9 w-9 items-center justify-center rounded-full border transition",
              viewModel.hasOutline
                ? "border-app-accent/35 bg-app-accent/15 opacity-100 shadow-[0_0_0_5px_rgb(var(--app-accent)_/_0.10)]"
                : "border-app-border bg-app-surface2 opacity-35 grayscale"
            )}
            title={viewModel.hasOutline ? "Has note outline" : "No note outline"}
            aria-label={viewModel.hasOutline ? "Has note outline" : "No note outline"}
          >
            📝
          </span>
          {showExcerptCount ? (
            <span
              data-testid={`tree-node-excerpt-count-${viewModel.id}`}
              className="inline-flex h-9 items-center gap-1 rounded-full border border-app-accent/30 bg-app-accent/10 px-3 text-[17px] font-semibold leading-none text-app-text shadow-[0_0_0_4px_rgb(var(--app-accent)_/_0.08)]"
              title={`Mounted excerpts: ${viewModel.excerptCount}`}
              aria-label={`Mounted excerpts: ${viewModel.excerptCount}`}
            >
              <span className="text-[18px]" aria-hidden="true">
                📌
              </span>
              <span>{viewModel.excerptCount}</span>
            </span>
          ) : null}
        </div>
      ) : null}

      {canToggleChildren ? (
        <button
          type="button"
          data-testid={`tree-node-toggle-${viewModel.id}`}
          className="absolute right-[-24px] top-1/2 z-30 flex h-12 w-12 -translate-y-1/2 items-center justify-center rounded-full border-[2px] border-app-border bg-app-surface text-app-muted shadow-panel transition hover:border-app-accent/60 hover:text-app-text"
          title={viewModel.isExpanded ? "折叠" : "展开"}
          aria-label={viewModel.isExpanded ? "折叠节点" : "展开节点"}
          onPointerDown={stopPointer}
          onMouseDown={stop}
          onDoubleClick={stop}
          onClick={(event) => {
            stop(event);
            data.onToggleExpanded(viewModel.id);
          }}
        >
          {viewModel.isExpanded ? <ChevronDown size={24} strokeWidth={2.5} /> : <ChevronRight size={24} strokeWidth={2.5} />}
        </button>
      ) : null}
      <Handle type="source" position={Position.Right} className={handleClass} style={{ right: -8 }} />
    </div>
  );
}
