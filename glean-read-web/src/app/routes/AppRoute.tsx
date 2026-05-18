import { Navigate } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { AppShell } from "@/app/AppShell";
import { useAuth } from "@/app/providers/AuthProvider";
import { resolveProtectedRoute } from "@/app/routes/routePolicy";

export function AppRoute() {
  const { session, isLoading } = useAuth();
  const routeDecision = resolveProtectedRoute(Boolean(session), isLoading);

  if (routeDecision.kind === "loading") {
    return (
      <main className="flex min-h-screen items-center justify-center bg-app-bg text-app-text">
        <div className="rounded-2xl border border-app-border bg-app-surface p-6 text-center shadow-panel">
          <Loader2 className="mx-auto animate-spin text-app-accent" size={28} />
          <div className="mt-3 text-sm text-app-muted">正在加载本地工作台</div>
        </div>
      </main>
    );
  }

  if (routeDecision.kind === "redirect") {
    return <Navigate to={routeDecision.to} replace={routeDecision.replace} />;
  }

  return <AppShell />;
}
