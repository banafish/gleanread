import { LogOut, Moon, PanelLeftClose, PanelLeftOpen, PanelRightOpen, Search, Sun } from "lucide-react";
import { useAuth } from "@/app/providers/AuthProvider";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";
import { Button, IconButton } from "@/shared/components";

export function WorkbenchTopBar({ syncMessage }: { syncMessage: string }) {
  const { session, signOut } = useAuth();
  const themeMode = useWorkbenchStore((state) => state.themeMode);
  const leftPanelWidth = useWorkbenchStore((state) => state.leftPanelWidth);
  const drawerOpen = useWorkbenchStore((state) => state.drawerOpen);
  const setThemeMode = useWorkbenchStore((state) => state.setThemeMode);
  const setLeftPanelWidth = useWorkbenchStore((state) => state.setLeftPanelWidth);
  const setDrawerOpen = useWorkbenchStore((state) => state.setDrawerOpen);
  const setSearchOpen = useWorkbenchStore((state) => state.setSearchOpen);

  const leftCollapsed = leftPanelWidth <= 96;

  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b border-app-border bg-app-surface px-3">
      <div className="flex min-w-0 items-center gap-2">
        <IconButton
          type="button"
          title={leftCollapsed ? "展开收件箱" : "折叠收件箱"}
          aria-label={leftCollapsed ? "展开收件箱" : "折叠收件箱"}
          onClick={() => setLeftPanelWidth(leftCollapsed ? 320 : 72)}
        >
          {leftCollapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
        </IconButton>
        <div className="flex items-center gap-2 px-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-app-accent text-sm font-bold text-white">G</div>
          <div className="leading-tight">
            <div className="text-sm font-semibold text-app-text">GleanRead</div>
            <div className="text-[11px] text-app-muted">{syncMessage}</div>
          </div>
        </div>
      </div>

      <div className="flex min-w-0 flex-1 justify-center px-4">
        <button
          type="button"
          className="flex h-10 w-full max-w-lg items-center justify-between rounded-xl border border-app-border bg-app-bg px-3 text-left text-sm text-app-muted transition hover:border-app-accent"
          onClick={() => setSearchOpen(true)}
        >
          <span className="inline-flex items-center gap-2">
            <Search size={16} />
            搜索节点、摘录或标签
          </span>
        </button>
      </div>

      <div className="flex items-center gap-2">
        <IconButton
          type="button"
          title={themeMode === "dark" ? "切换浅色模式" : "切换深色模式"}
          aria-label={themeMode === "dark" ? "切换浅色模式" : "切换深色模式"}
          onClick={() => setThemeMode(themeMode === "dark" ? "light" : "dark")}
        >
          {themeMode === "dark" ? <Sun size={18} /> : <Moon size={18} />}
        </IconButton>
        <IconButton
          type="button"
          title="打开沉淀抽屉"
          aria-label="打开沉淀抽屉"
          onClick={() => setDrawerOpen(!drawerOpen)}
        >
          <PanelRightOpen size={18} />
        </IconButton>
        <div className="hidden max-w-[180px] truncate text-xs text-app-muted md:block">{session?.email}</div>
        <Button type="button" variant="secondary" className="h-10 px-3" onClick={() => void signOut()}>
          <LogOut size={16} />
          退出
        </Button>
      </div>
    </header>
  );
}
