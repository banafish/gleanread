import { useEffect } from "react";
import { DetailDrawer } from "@/features/detail-drawer/components/DetailDrawer";
import { InboxSidebar } from "@/features/inbox/components/InboxSidebar";
import { KnowledgeTreeGraph } from "@/features/knowledge-tree/components/KnowledgeTreeGraph";
import { SearchDialog } from "@/features/workbench/components/SearchDialog";
import { WorkbenchPreferenceSync } from "@/features/workbench/components/WorkbenchPreferenceSync";
import { WorkbenchTopBar } from "@/features/workbench/components/WorkbenchTopBar";
import { TrashDialog } from "@/features/workbench/components/TrashDialog";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";
import { useSyncEngine } from "@/features/sync/useSyncEngine";

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function ResizeHandle({
  onResize,
  label,
}: {
  onResize: (deltaX: number) => void;
  label: string;
}) {
  return (
    <div
      role="separator"
      aria-label={label}
      className="w-1 cursor-col-resize bg-transparent transition hover:bg-app-accent/40"
      onPointerDown={(event) => {
        event.preventDefault();
        const startX = event.clientX;
        const handlePointerMove = (moveEvent: PointerEvent) => {
          onResize(moveEvent.clientX - startX);
        };
        const handlePointerUp = () => {
          window.removeEventListener("pointermove", handlePointerMove);
          window.removeEventListener("pointerup", handlePointerUp);
        };
        window.addEventListener("pointermove", handlePointerMove);
        window.addEventListener("pointerup", handlePointerUp);
      }}
    />
  );
}

export function AppShell() {
  const leftPanelWidth = useWorkbenchStore((state) => state.leftPanelWidth);
  const rightPanelWidth = useWorkbenchStore((state) => state.rightPanelWidth);
  const drawerOpen = useWorkbenchStore((state) => state.drawerOpen);
  const setLeftPanelWidth = useWorkbenchStore((state) => state.setLeftPanelWidth);
  const setRightPanelWidth = useWorkbenchStore((state) => state.setRightPanelWidth);
  const setSearchOpen = useWorkbenchStore((state) => state.setSearchOpen);
  const sync = useSyncEngine();

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setSearchOpen(true);
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [setSearchOpen]);

  const leftCollapsed = leftPanelWidth <= 96;

  return (
    <div className="flex h-screen min-h-0 flex-col bg-app-bg text-app-text">
      <WorkbenchTopBar syncMessage={sync.message} />
      <div className="flex min-h-0 flex-1 overflow-hidden">
        <div className="min-h-0 shrink-0" style={{ width: leftPanelWidth }}>
          <InboxSidebar collapsed={leftCollapsed} />
        </div>
        <ResizeHandle
          label="调整收件箱宽度"
          onResize={(deltaX) => setLeftPanelWidth(clamp(leftPanelWidth + deltaX, 72, 440))}
        />
        <main className="min-h-0 min-w-0 flex-1">
          <KnowledgeTreeGraph />
        </main>
        {drawerOpen ? (
          <>
            <ResizeHandle
              label="调整详情抽屉宽度"
              onResize={(deltaX) => setRightPanelWidth(clamp(rightPanelWidth - deltaX, 320, 620))}
            />
            <div className="min-h-0 shrink-0" style={{ width: rightPanelWidth }}>
              <DetailDrawer />
            </div>
          </>
        ) : null}
      </div>
      <SearchDialog />
      <TrashDialog />
      <WorkbenchPreferenceSync />
    </div>
  );
}
