import { Link } from "react-router-dom";
import { ArrowRight, Brain, Layers, Moon, Sparkles } from "lucide-react";
import { useAuth } from "@/app/providers/AuthProvider";

export function HomeRoute() {
  const { session } = useAuth();

  return (
    <main className="min-h-screen bg-app-bg text-app-text">
      <section className="relative flex min-h-screen overflow-hidden">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgb(var(--app-accent)/0.16),transparent_32%),radial-gradient(circle_at_80%_15%,rgb(var(--app-success)/0.12),transparent_28%)]" />
        <div className="absolute inset-x-6 bottom-8 top-24 overflow-hidden rounded-2xl border border-app-border bg-app-surface/70 shadow-2xl backdrop-blur">
          <div className="grid h-full grid-cols-[280px_minmax(0,1fr)_360px]">
            <div className="border-r border-app-border p-5">
              <div className="mb-5 h-8 w-32 rounded-lg bg-app-surface2" />
              {[0, 1, 2, 3].map((item) => (
                <div key={item} className="mb-3 rounded-panel border border-app-border bg-app-bg p-3">
                  <div className="h-3 w-3/4 rounded bg-app-text/20" />
                  <div className="mt-3 h-2 w-full rounded bg-app-muted/15" />
                  <div className="mt-2 h-2 w-2/3 rounded bg-app-muted/15" />
                </div>
              ))}
            </div>
            <div className="relative p-10">
              <div className="absolute left-20 top-1/2 h-0.5 w-[48%] bg-app-accent/30" />
              <div className="absolute left-[42%] top-[34%] h-[38%] w-0.5 bg-app-accent/20" />
              {["知识体系", "React", "本地优先", "同步引擎", "摘录沉淀"].map((label, index) => (
                <div
                  key={label}
                  className="absolute rounded-panel border border-app-border bg-app-surface px-4 py-3 shadow-panel"
                  style={{
                    left: `${12 + (index % 3) * 25}%`,
                    top: `${24 + Math.floor(index / 3) * 26 + (index === 4 ? 8 : 0)}%`,
                  }}
                >
                  <div className="text-sm font-semibold">{label}</div>
                  <div className="mt-2 flex gap-2 text-xs text-app-muted">
                    <span>大纲</span>
                    <span>摘录</span>
                  </div>
                </div>
              ))}
            </div>
            <div className="border-l border-app-border p-5">
              <div className="mb-4 h-8 w-40 rounded-lg bg-app-text/15" />
              <div className="h-48 rounded-panel border border-app-border bg-app-bg p-4">
                <div className="h-3 w-2/3 rounded bg-app-text/20" />
                <div className="mt-4 space-y-2">
                  <div className="h-2 rounded bg-app-muted/15" />
                  <div className="h-2 rounded bg-app-muted/15" />
                  <div className="h-2 w-4/5 rounded bg-app-muted/15" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="relative z-10 flex min-h-screen w-full items-start px-6 py-8 sm:px-10 lg:px-16">
          <div className="max-w-2xl pt-20">
            <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-app-border bg-app-surface/80 px-3 py-1 text-sm text-app-muted shadow-panel backdrop-blur">
              <Sparkles size={15} />
              Local-first visual knowledge workbench
            </div>
            <h1 className="text-5xl font-semibold tracking-normal text-app-text sm:text-6xl">GleanRead</h1>
            <p className="mt-5 max-w-xl text-lg leading-8 text-app-muted">
              将碎片摘录收进 Inbox，再拖入横向知识树，在右侧抽屉里把阅读、思考和大纲沉淀成可复用的知识结构。
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                to={session ? "/app" : "/login"}
                className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-app-accent px-5 text-sm font-medium text-white shadow-panel transition hover:opacity-90"
              >
                开始使用
                <ArrowRight size={16} />
              </Link>
              <Link
                to="/login"
                className="inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-app-border bg-app-surface2 px-5 text-sm font-medium text-app-text transition hover:bg-app-surface"
              >
                登录 / 注册
              </Link>
            </div>
            <div className="mt-10 grid max-w-xl grid-cols-1 gap-3 sm:grid-cols-3">
              {[
                { icon: Brain, title: "横向知识树" },
                { icon: Layers, title: "摘录挂载" },
                { icon: Moon, title: "深浅主题" },
              ].map((item) => (
                <div key={item.title} className="rounded-panel border border-app-border bg-app-surface/80 p-4 shadow-panel backdrop-blur">
                  <item.icon className="text-app-accent" size={18} />
                  <div className="mt-3 text-sm font-semibold">{item.title}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
