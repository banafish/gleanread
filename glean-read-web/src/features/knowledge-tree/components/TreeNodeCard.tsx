import type { MouseEvent } from "react";
import { useDroppable } from "@dnd-kit/core";
import { ArrowDown, ArrowUp, ChevronDown, ChevronRight, FileText, MoreHorizontal, Pin, Plus, Trash2 } from "lucide-react";
import { Handle, Position, type NodeProps } from "reactflow";
import { cx } from "@/shared/utils";
import type { KnowledgeGraphNodeData } from "@/features/knowledge-tree/treeAdapters";

function stop(event: MouseEvent<HTMLButtonElement>) {
  event.stopPropagation();
}

export function TreeNodeCard({ data }: NodeProps<KnowledgeGraphNodeData>) {
  const { viewModel } = data;
  const isVirtualRoot = Boolean(viewModel.isVirtualRoot);
  const { setNodeRef, isOver } = useDroppable({
    id: viewModel.id,
    disabled: isVirtualRoot,
    data: {
      type: "tree-node",
      nodeId: viewModel.id,
    },
  });
  const actionButtonClass =
    "inline-flex h-7 w-7 items-center justify-center rounded-lg bg-app-surface2 text-app-muted transition hover:text-app-text disabled:cursor-not-allowed disabled:opacity-35 disabled:hover:text-app-muted";
  const isDropTargetActive = data.isHovered || isOver;

  return (
    <div
      ref={setNodeRef}
      className={cx(
        "group relative w-[236px] rounded-panel border bg-app-surface px-3 py-3 shadow-panel transition",
        "hover:border-app-accent/60 hover:shadow-lg",
        isVirtualRoot && "w-[184px] bg-app-accent text-white",
        data.isSelected && "border-app-accent ring-2 ring-app-accent/20",
        isDropTargetActive && "border-app-success ring-2 ring-app-success/25",
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
      <Handle type="target" position={Position.Left} className="!h-2 !w-2 !border-0 !bg-app-accent" />
      <div className="flex min-h-[48px] items-start gap-2">
        <button
          type="button"
          className={cx(
            "mt-0.5 inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-lg transition",
            isVirtualRoot ? "bg-white/15 text-white hover:bg-white/25" : "bg-app-surface2 text-app-muted hover:text-app-text"
          )}
          title={viewModel.isExpanded ? "折叠" : "展开"}
          aria-label={viewModel.isExpanded ? "折叠节点" : "展开节点"}
          onClick={(event) => {
            stop(event);
            data.onToggleExpanded(viewModel.id);
          }}
          disabled={!data.hasChildren && !isVirtualRoot}
        >
          {viewModel.isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
        </button>

        <div className="min-w-0 flex-1">
          <div className={cx("line-clamp-2 text-sm font-semibold leading-snug", isVirtualRoot ? "text-white" : "text-app-text")}>
            {viewModel.title}
          </div>
          <div className="mt-2 flex flex-wrap items-center gap-1.5">
            {viewModel.hasOutline ? (
              <span className={cx("inline-flex items-center gap-1 text-[11px]", isVirtualRoot ? "text-white/85" : "text-app-muted")}>
                <FileText size={12} />
                大纲
              </span>
            ) : null}
            {viewModel.excerptCount > 0 ? (
              <span className={cx("inline-flex items-center gap-1 text-[11px]", isVirtualRoot ? "text-white/85" : "text-app-muted")}>
                <Pin size={12} />
                {viewModel.excerptCount}
              </span>
            ) : null}
          </div>
        </div>

        <div
          className={cx(
            "grid shrink-0 grid-cols-2 gap-1 opacity-0 transition group-hover:opacity-100",
            isVirtualRoot && "grid-cols-1",
            data.isSelected && "opacity-100"
          )}
        >
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
      </div>
      <Handle type="source" position={Position.Right} className="!h-2 !w-2 !border-0 !bg-app-accent" />
    </div>
  );
}
