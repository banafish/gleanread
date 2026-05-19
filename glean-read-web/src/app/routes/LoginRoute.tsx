import { useEffect, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { ArrowLeft, Github, KeyRound, Mail, Sparkles } from "lucide-react";
import { useAuth } from "@/app/providers/AuthProvider";
import { resolveLoginRoute } from "@/app/routes/routePolicy";
import { Button, Input } from "@/shared/components";
import { cx } from "@/shared/utils";

type LoginPanel = "login" | "signup" | "magic";

export function LoginRoute() {
  const navigate = useNavigate();
  const {
    session,
    isLoading,
    signInWithPassword,
    signUpWithPassword,
    sendMagicLink,
    signInWithOAuth,
  } = useAuth();
  const [panel, setPanel] = useState<LoginPanel>("login");
  const [email, setEmail] = useState("reader@gleanread.local");
  const [password, setPassword] = useState("gleanread-demo");
  const [confirmPassword, setConfirmPassword] = useState("gleanread-demo");
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const routeDecision = resolveLoginRoute(Boolean(session), isLoading);
  const redirectTo = routeDecision.kind === "redirect" ? routeDecision.to : null;

  useEffect(() => {
    if (redirectTo) {
      navigate(redirectTo, { replace: true });
    }
  }, [navigate, redirectTo]);

  if (routeDecision.kind === "redirect") {
    return <Navigate to={routeDecision.to} replace={routeDecision.replace} />;
  }

  const submit = async () => {
    setSubmitting(true);
    setError(null);
    setStatus(null);
    try {
      if (panel === "signup") {
        await signUpWithPassword(email, password, confirmPassword);
        setStatus("账号已创建。");
      } else if (panel === "magic") {
        await sendMagicLink(email);
        setStatus("Magic Link 已发送。");
        return;
      } else {
        await signInWithPassword(email, password);
        setStatus("登录成功。");
      }
      navigate("/app", { replace: true });
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "认证失败。");
    } finally {
      setSubmitting(false);
    }
  };

  const oauth = async (provider: "google" | "github") => {
    setSubmitting(true);
    setError(null);
    try {
      await signInWithOAuth(provider);
      navigate("/app", { replace: true });
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "OAuth 登录失败。");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="grid min-h-screen bg-app-bg text-app-text lg:grid-cols-2">
      <section className="relative hidden overflow-hidden border-r border-app-border bg-app-surface lg:block">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_25%_25%,rgb(var(--app-accent)/0.22),transparent_34%),radial-gradient(circle_at_75%_70%,rgb(var(--app-success)/0.16),transparent_32%)]" />
        <div className="relative flex h-full flex-col justify-between p-12">
          <Link to="/" className="inline-flex items-center gap-2 text-sm text-app-muted hover:text-app-text">
            <ArrowLeft size={16} />
            返回首页
          </Link>
          <div>
            <div className="mb-6 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-app-accent text-lg font-bold text-white">
              G
            </div>
            <h1 className="max-w-xl text-4xl font-semibold leading-tight tracking-normal">
              连接碎片灵感，构建你的可视化知识大脑。
            </h1>
            <p className="mt-5 max-w-lg text-base leading-7 text-app-muted">
              收集摘录、组织知识树、沉淀大纲，在一个本地优先的工作台中完成阅读后的结构化思考。
            </p>
          </div>
          <div className="relative h-56 rounded-2xl border border-app-border bg-app-bg/80 p-5 shadow-2xl backdrop-blur">
            <div className="absolute left-8 top-20 h-0.5 w-64 bg-app-accent/30" />
            <div className="absolute left-44 top-10 h-32 w-0.5 bg-app-accent/20" />
            {["Inbox", "知识体系", "大纲沉淀", "标签"].map((label, index) => (
              <div
                key={label}
                className="absolute rounded-panel border border-app-border bg-app-surface px-3 py-2 text-sm shadow-panel"
                style={{ left: `${8 + index * 21}%`, top: `${24 + (index % 2) * 34}%` }}
              >
                {label}
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="flex min-h-screen items-center justify-center p-6">
        <div className="w-full max-w-md rounded-2xl border border-app-border bg-app-surface p-6 shadow-panel">
          <div className="mb-6 flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-app-accent text-sm font-bold text-white">G</div>
            <div>
              <div className="text-lg font-semibold">GleanRead</div>
              <div className="text-sm text-app-muted">欢迎回来，继续你的知识构建</div>
            </div>
          </div>

          <div className="mb-5 grid grid-cols-3 gap-2 rounded-xl bg-app-bg p-1">
            {[
              ["login", "登录"],
              ["signup", "注册"],
              ["magic", "Magic Link"],
            ].map(([value, label]) => (
              <button
                key={value}
                type="button"
                className={cx(
                  "rounded-lg px-3 py-2 text-sm font-medium transition",
                  panel === value ? "bg-app-surface text-app-text shadow-panel" : "text-app-muted hover:text-app-text"
                )}
                onClick={() => setPanel(value as LoginPanel)}
              >
                {label}
              </button>
            ))}
          </div>

          <div className="space-y-3">
            <label className="block space-y-1">
              <span className="text-xs font-medium text-app-muted">邮箱</span>
              <Input value={email} onChange={(event) => setEmail(event.target.value)} type="email" autoComplete="email" />
            </label>
            {panel !== "magic" ? (
              <label className="block space-y-1">
                <span className="text-xs font-medium text-app-muted">密码</span>
                <Input
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  type="password"
                  autoComplete={panel === "signup" ? "new-password" : "current-password"}
                />
              </label>
            ) : null}
            {panel === "signup" ? (
              <label className="block space-y-1">
                <span className="text-xs font-medium text-app-muted">确认密码</span>
                <Input
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  type="password"
                  autoComplete="new-password"
                />
              </label>
            ) : null}
          </div>

          {error ? (
            <div
              className="mt-4 rounded-xl border border-app-danger/30 bg-app-danger/10 p-3 text-sm text-app-danger"
              data-testid="auth-error"
            >
              {error}
            </div>
          ) : null}
          {status ? <div className="mt-4 rounded-xl border border-app-success/30 bg-app-success/10 p-3 text-sm text-app-success">{status}</div> : null}

          <Button
            type="button"
            className="mt-5 h-11 w-full"
            data-testid="login-submit"
            disabled={submitting}
            onClick={() => void submit()}
          >
            {panel === "magic" ? <Mail size={16} /> : <KeyRound size={16} />}
            {panel === "signup" ? "注册并进入" : panel === "magic" ? "发送 Magic Link" : "登录"}
          </Button>

          <div className="my-5 flex items-center gap-3 text-xs text-app-muted">
            <div className="h-px flex-1 bg-app-border" />
            OAuth
            <div className="h-px flex-1 bg-app-border" />
          </div>

          <div className="grid grid-cols-2 gap-2">
            <Button type="button" variant="secondary" disabled={submitting} onClick={() => void oauth("github")}>
              <Github size={16} />
              GitHub
            </Button>
            <Button type="button" variant="secondary" disabled={submitting} onClick={() => void oauth("google")}>
              <Sparkles size={16} />
              Google
            </Button>
          </div>
        </div>
      </section>
    </main>
  );
}
