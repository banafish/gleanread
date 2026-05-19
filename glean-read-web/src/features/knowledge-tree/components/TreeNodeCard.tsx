import type { MouseEvent } from "react";
import { useDroppable } from "@dnd-kit/core";
import { ArrowDown, ArrowUp, ChevronDown, ChevronRight, MoreHorizontal, Plus, Trash2 } from "lucide-react";
import { Handle, Position, type NodeProps } from "reactflow";
import { cx } from "@/shared/utils";
import type { KnowledgeGraphNodeData } from "@/features/knowledge-tree/treeAdapters";

function stop(event: MouseEvent<HTMLButtonElement>) {
  event.stopPropagation();
}

export function TreeNodeCard({ data }: NodeProps<KnowledgeGraphNodeData>) {
  const { viewModel } = data;
  const isVirtualRoot = Boolean(viewModel.isVirtualRoot);
  const canToggleChildren = data.hasChildren && !isVirtualRoot;
  const { setNodeRef, isOver } = useDroppable({
    id: viewModel.id,
    disabled: isVirtualRoot,
    data: {
      type: "tree-node",
      nodeId: viewModel.id,
    },
  });
  const actionButtonClass =
    "inline-flex h-8 w-8 items-center justify-center rounded-full border border-app-border bg-app-surface2 text-app-muted shadow-panel transition hover:border-app-accent/60 hover:text-app-text disabled:cursor-not-allowed disabled:opacity-35 disabled:hover:text-app-muted";
  const handleClass = "!h-4 !w-4 !border-0 !bg-app-accent !shadow-none";
  const isDropTargetActive = data.isHovered || isOver;

  return (
    <div
      ref={setNodeRef}
      className={cx(
        "group relative flex h-[112px] w-[500px] items-center rounded-[28px] border-[3px] bg-app-surface px-11 text-app-text shadow-panel transition",
        "hover:border-app-accent/60 hover:shadow-lg",
        data.isSelected && "border-app-accent ring-[3px] ring-app-accent/20",
        isDropTargetActive && "border-app-success ring-[3px] ring-app-success/25",
        !data.isSelected && !isDropTargetActive && "border-app-border"
      )}
      onClick={() => data.onSelect(viewModel.id)}
      onDoubleClick={() => {
        if (data.hasChildren) {
          data.onToggleExpanded(viewModel.id);
        }
      }}
      role="button"
      tabIndex={0}
    >
      <Handle type="target" position={Position.Left} className={handleClass} style={{ left: -8 }} />
      <div className="min-w-0 flex-1 pr-8">
        <div className="line-clamp-2 text-[32px] font-semibold leading-[1.18] text-current">
          {viewModel.title}
        </div>
      </div>

      <div className="pointer-events-none absolute right-5 top-3 z-20 flex items-center gap-1 opacity-0 transition group-hover:pointer-events-auto group-hover:opacity-100 focus-within:pointer-events-auto focus-within:opacity-100">
        <button
          type="button"
          className={actionButtonClass}
          title="添加子节点"
          aria-label="添加子节点"
          onClick={(event) => {
            stop(event);
            data.onAddChild(isVirtualRoot ? null : viewModel.id);
          }}
        >
          <Plus size={14} />
        </button>
        {!isVirtualRoot ? (
          <>
            <button
              type="button"
              className={actionButtonClass}
              title="重命名"
              aria-label="重命名"
              onClick={(event) => {
                stop(event);
                data.onRename(viewModel.id);
              }}
            >
              <MoreHorizontal size={14} />
            </button>
            <button
              type="button"
              className={actionButtonClass}
              title="上移"
              aria-label="上移节点"
              disabled={!data.canMoveUp}
              onClick={(event) => {
                stop(event);
                data.onMoveSibling(viewModel.id, "up");
              }}
            >
              <ArrowUp size={14} />
            </button>
            <button
              type="button"
              className={actionButtonClass}
              title="下移"
              aria-label="下移节点"
              disabled={!data.canMoveDown}
              onClick={(event) => {
                stop(event);
                data.onMoveSibling(viewModel.id, "down");
              }}
            >
              <ArrowDown size={14} />
            </button>
            <button
              type="button"
              className={cx(actionButtonClass, "hover:text-app-danger")}
              title="删除"
              aria-label="删除"
              onClick={(event) => {
                stop(event);
                data.onDelete(viewModel.id);
              }}
            >
              <Trash2 size={14} />
            </button>
          </>
        ) : null}
      </div>

      {canToggleChildren ? (
        <button
          type="button"
          className="absolute right-[-24px] top-1/2 z-30 flex h-12 w-12 -translate-y-1/2 items-center justify-center rounded-full border-[2px] border-app-border bg-app-surface text-app-muted shadow-panel transition hover:border-app-accent/60 hover:text-app-text"
          title={viewModel.isExpanded ? "折叠" : "展开"}
          aria-label={viewModel.isExpanded ? "折叠节点" : "展开节点"}
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
