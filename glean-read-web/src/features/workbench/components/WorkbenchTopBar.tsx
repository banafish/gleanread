import { useEffect, useRef, useState } from "react";
import {
  ChevronDown,
  LogOut,
  Moon,
  PanelLeftClose,
  PanelLeftOpen,
  PanelRightOpen,
  Search,
  Sparkles,
  Sun,
  Trash2,
} from "lucide-react";
import { useAuth } from "@/app/providers/AuthProvider";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";
import { IconButton } from "@/shared/components";
import { cx } from "@/shared/utils";
import { AiConfigDialog } from "@/features/detail-drawer/components/AiConfigDialog";

const appIconSrc = "/icons/icon-192.png";

type AvatarSize = "compact" | "expanded";

const avatarSizeClassName: Record<AvatarSize, string> = {
  compact: "h-6 w-6 text-xs",
  expanded: "h-10 w-10 text-sm",
};

function AccountAvatar({
  avatarUrl,
  fallback,
  size = "compact",
}: {
  avatarUrl: string | null;
  fallback: string;
  size?: AvatarSize;
}) {
  const [isImageBroken, setIsImageBroken] = useState(false);

  useEffect(() => {
    setIsImageBroken(false);
  }, [avatarUrl]);

  return (
    <span
      className={cx(
        "relative flex shrink-0 items-center justify-center overflow-hidden rounded-full bg-app-accent font-semibold text-white",
        avatarSizeClassName[size]
      )}
    >
      {avatarUrl && !isImageBroken ? (
        <img
          src={avatarUrl}
          alt=""
          className="absolute inset-0 h-full w-full object-cover"
          referrerPolicy="no-referrer"
          onError={() => setIsImageBroken(true)}
        />
      ) : (
        fallback
      )}
    </span>
  );
}

export function WorkbenchTopBar({ syncMessage }: { syncMessage: string }) {
  const { session, signOut } = useAuth();
  const themeMode = useWorkbenchStore((state) => state.themeMode);
  const leftPanelWidth = useWorkbenchStore((state) => state.leftPanelWidth);
  const drawerOpen = useWorkbenchStore((state) => state.drawerOpen);
  const setThemeMode = useWorkbenchStore((state) => state.setThemeMode);
  const setLeftPanelWidth = useWorkbenchStore((state) => state.setLeftPanelWidth);
  const setDrawerOpen = useWorkbenchStore((state) => state.setDrawerOpen);
  const setSearchOpen = useWorkbenchStore((state) => state.setSearchOpen);
  const setTrashOpen = useWorkbenchStore((state) => state.setTrashOpen);
  const [menuOpen, setMenuOpen] = useState(false);
  const [aiConfigOpen, setAiConfigOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const handlePointerDown = (event: PointerEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMenuOpen(false);
      }
    };
    window.addEventListener("pointerdown", handlePointerDown);
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("pointerdown", handlePointerDown);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, []);

  const leftCollapsed = leftPanelWidth <= 96;
  const avatarFallback = (session?.email ?? "U").trim().charAt(0).toUpperCase() || "U";
  const avatarUrl = session?.avatarUrl?.trim() || null;

  return (
    <header className="relative flex h-14 shrink-0 items-center justify-between border-b border-app-border bg-app-surface px-3">
      <div className="flex min-w-0 items-center gap-2">
        <IconButton
          type="button"
          title={leftCollapsed ? "展开收件箱" : "折叠收件箱"}
          aria-label={leftCollapsed ? "展开收件箱" : "折叠收件箱"}
          onClick={() => setLeftPanelWidth(leftCollapsed ? 280 : 72)}
        >
          {leftCollapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
        </IconButton>
        <div className="flex items-center gap-2 px-2">
          <img
            src={appIconSrc}
            alt=""
            aria-hidden="true"
            className="h-8 w-8 shrink-0 rounded-xl object-cover"
          />
          <div className="leading-tight">
            <div className="text-sm font-semibold text-app-text">GleanRead</div>
            <div className="text-[11px] text-app-muted">{syncMessage}</div>
          </div>
        </div>
      </div>

      <div className="flex min-w-0 flex-1 justify-center px-4">
        <button
          type="button"
          data-testid="global-search-trigger"
          className="flex h-10 w-full max-w-lg items-center justify-between rounded-xl border border-app-border bg-app-bg px-3 text-left text-sm text-app-muted transition hover:border-app-accent"
          onClick={() => setSearchOpen(true)}
        >
          <span className="inline-flex items-center gap-2">
            <Search size={16} />
            搜索节点、摘录或标签
          </span>
        </button>
      </div>

      <div className="relative flex items-center gap-2" ref={menuRef}>
        <IconButton
          type="button"
          data-testid="theme-toggle"
          title={themeMode === "dark" ? "切换浅色模式" : "切换深色模式"}
          aria-label={themeMode === "dark" ? "切换浅色模式" : "切换深色模式"}
          onClick={() => setThemeMode(themeMode === "dark" ? "light" : "dark")}
        >
          {themeMode === "dark" ? <Sun size={18} /> : <Moon size={18} />}
        </IconButton>
        <IconButton
          type="button"
          data-testid="drawer-toggle"
          title="打开沉淀抽屉"
          aria-label="打开沉淀抽屉"
          onClick={() => setDrawerOpen(!drawerOpen)}
        >
          <PanelRightOpen size={18} />
        </IconButton>
        <button
          type="button"
          data-testid="account-menu-trigger"
          className="inline-flex h-10 items-center gap-2 rounded-xl border border-app-border bg-app-surface2 px-3 text-sm text-app-text transition hover:border-app-accent"
          aria-label="用户设置"
          onClick={() => setMenuOpen((value) => !value)}
        >
          <AccountAvatar avatarUrl={avatarUrl} fallback={avatarFallback} />
          <span className="hidden max-w-[160px] truncate md:block">{session?.email ?? "本地用户"}</span>
          <ChevronDown size={14} className="text-app-muted" />
        </button>
        {menuOpen ? (
          <div className="absolute right-0 top-12 z-30 w-64 rounded-2xl border border-app-border bg-app-surface p-3 shadow-2xl">
            <div className="flex items-center gap-3 border-b border-app-border pb-3">
              <AccountAvatar avatarUrl={avatarUrl} fallback={avatarFallback} size="expanded" />
              <div className="min-w-0">
                <div className="truncate text-sm font-semibold text-app-text">{session?.email ?? "本地用户"}</div>
                <div className="text-xs text-app-muted">用户设置</div>
              </div>
            </div>
            <div className="mt-3 space-y-1">
              <button
                type="button"
                className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-sm text-app-text transition hover:bg-app-surface2"
                onClick={() => {
                  setMenuOpen(false);
                  setTrashOpen(true);
                }}
              >
                <Trash2 size={15} />
                垃圾篓
              </button>
              <button
                type="button"
                data-testid="ai-settings-trigger"
                className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-sm text-app-text transition hover:bg-app-surface2"
                onClick={() => {
                  setMenuOpen(false);
                  setAiConfigOpen(true);
                }}
              >
                <Sparkles size={15} />
                AI 配置
              </button>
              <button
                type="button"
                className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-sm text-app-text transition hover:bg-app-surface2 hover:text-app-danger"
                onClick={() => {
                  setMenuOpen(false);
                  void signOut();
                }}
              >
                <LogOut size={15} />
                退出登录
              </button>
            </div>
          </div>
        ) : null}
      </div>
      <AiConfigDialog open={aiConfigOpen} onClose={() => setAiConfigOpen(false)} />
    </header>
  );
}
