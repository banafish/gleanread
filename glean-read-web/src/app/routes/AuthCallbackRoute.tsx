import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { useAuth } from "@/app/providers/AuthProvider";

export function AuthCallbackRoute() {
  const navigate = useNavigate();
  const { completeAuthCallback } = useAuth();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    void (async () => {
      try {
        await completeAuthCallback();
        if (alive) {
          navigate("/app", { replace: true });
        }
      } catch (cause) {
        if (alive) {
          setError(cause instanceof Error ? cause.message : "认证回调处理失败。");
        }
      }
    })();
    return () => {
      alive = false;
    };
  }, [completeAuthCallback, navigate]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-app-bg p-6 text-app-text">
      <div className="w-full max-w-md rounded-2xl border border-app-border bg-app-surface p-6 text-center shadow-panel">
        {!error ? (
          <>
            <Loader2 className="mx-auto animate-spin text-app-accent" size={28} />
            <h1 className="mt-4 text-lg font-semibold">正在完成登录</h1>
            <p className="mt-2 text-sm text-app-muted">会话恢复后会自动进入工作台。</p>
          </>
        ) : (
          <>
            <h1 className="text-lg font-semibold">登录回调失败</h1>
            <p className="mt-2 text-sm text-app-muted">{error}</p>
            <Link
              to="/login"
              className="mt-5 inline-flex h-10 items-center justify-center rounded-xl bg-app-accent px-4 text-sm font-medium text-white shadow-panel transition hover:opacity-90"
            >
              返回登录
            </Link>
          </>
        )}
      </div>
    </main>
  );
}
