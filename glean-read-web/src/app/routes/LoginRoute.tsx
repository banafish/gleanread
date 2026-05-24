import { useEffect, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  KeyRound,
  Mail,
  Sparkles,
  Smartphone,
  ChevronRight,
  ShieldCheck,
  CheckCircle,
  Eye,
  EyeOff,
} from "lucide-react";
import { useAuth } from "@/app/providers/AuthProvider";
import { resolveLoginRoute } from "@/app/routes/routePolicy";
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
  } = useAuth();
  
  const [panel, setPanel] = useState<LoginPanel>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
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
        setStatus("账号已成功创建，正在进入工作台...");
      } else if (panel === "magic") {
        await sendMagicLink(email);
        setStatus("Magic Link 已发送，请检查您的电子邮箱。");
        return;
      } else {
        await signInWithPassword(email, password);
        setStatus("登录成功，正在进入工作台...");
      }
      navigate("/app", { replace: true });
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "身份认证失败。");
    } finally {
      setSubmitting(false);
    }
  };

  const tabs = [
    { id: "login", label: "登录" },
    { id: "signup", label: "注册" },
    { id: "magic", label: "Magic Link" },
  ] as const;

  return (
    <main className="grid min-h-screen bg-slate-950 text-slate-100 lg:grid-cols-12 overflow-hidden selection:bg-blue-500/30 selection:text-blue-200">
      {/* 左侧：艺术级知识大脑星云区 (7格宽) */}
      <section className="relative hidden lg:flex lg:col-span-7 overflow-hidden border-r border-white/5 bg-slate-950 flex-col justify-between p-12">
        {/* 背景科技网格与渐变发光 */}
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#0f172a_1px,transparent_1px),linear-gradient(to_bottom,#0f172a_1px,transparent_1px)] bg-[size:3rem_3rem] opacity-30" />
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_40%_40%,rgb(59,130,246,0.12),transparent_45%)]" />
        
        {/* 悬浮气泡返回首页按钮 */}
        <Link
          to="/"
          className="relative z-20 inline-flex items-center gap-2 rounded-xl border border-white/5 bg-slate-900/60 px-4 py-2 text-xs font-semibold text-slate-400 backdrop-blur transition hover:bg-slate-800 hover:text-white hover:scale-102 active:scale-98"
        >
          <ArrowLeft size={14} />
          返回主页
        </Link>

        {/* 核心价值介绍 */}
        <div className="relative z-10 mt-16 max-w-xl">
          <div className="mb-4 inline-flex items-center gap-1.5 rounded-full bg-gradient-to-r from-blue-500/10 to-cyan-500/10 border border-blue-500/20 px-3.5 py-1 text-[11px] font-bold text-blue-400">
            <Sparkles size={12} />
            可视化知识神经元系统
          </div>
          <h1 className="text-4xl font-extrabold leading-tight tracking-tight text-white md:text-5xl">
            连接碎片灵感，<br />
            构建你的<span className="bg-gradient-to-r from-blue-400 to-cyan-400 bg-clip-text text-transparent">可视化知识大脑</span>。
          </h1>
          <p className="mt-4 text-sm leading-relaxed text-slate-400">
            一端收集，双端闪现。在 Web 桌面端横向建树、理清宏大脉络；在 Android 原生端通过极速挂件剪藏灵感。数据基于 Room + Dexie 离线优先， Supabase 静默队列自动安全同步。
          </p>
        </div>

        {/* CSS/SVG 知识宇宙连线网图 */}
        <div className="relative h-60 w-full rounded-2xl border border-white/5 bg-slate-900/30 p-5 shadow-2xl backdrop-blur-sm overflow-hidden">
          {/* 星云旋转连线 */}
          <svg className="absolute inset-0 h-full w-full opacity-40">
            <line x1="15%" y1="20%" x2="50%" y2="25%" stroke="rgba(59, 130, 246, 0.3)" strokeWidth="1" />
            <line x1="50%" y1="25%" x2="85%" y2="35%" stroke="rgba(59, 130, 246, 0.3)" strokeWidth="1" />
            <line x1="85%" y1="35%" x2="70%" y2="75%" stroke="rgba(59, 130, 246, 0.3)" strokeWidth="1" />
            <line x1="70%" y1="75%" x2="35%" y2="70%" stroke="rgba(59, 130, 246, 0.3)" strokeWidth="1" />
            <line x1="35%" y1="70%" x2="15%" y2="20%" stroke="rgba(59, 130, 246, 0.3)" strokeWidth="1" />
            <line x1="50%" y1="25%" x2="70%" y2="75%" stroke="rgba(59, 130, 246, 0.3)" strokeWidth="1" />

            <circle cx="15%" cy="20%" r="4" fill="#3b82f6" className="animate-ping" style={{ animationDuration: '3s' }} />
            <circle cx="15%" cy="20%" r="3" fill="#60a5fa" />
            
            <circle cx="50%" cy="25%" r="4" fill="#22d3ee" className="animate-ping" style={{ animationDuration: '4s' }} />
            <circle cx="50%" cy="25%" r="3" fill="#22d3ee" />

            <circle cx="85%" cy="35%" r="4" fill="#8b5cf6" className="animate-ping" style={{ animationDuration: '5s' }} />
            <circle cx="85%" cy="35%" r="3" fill="#a78bfa" />

            <circle cx="70%" cy="75%" r="4" fill="#ec4899" className="animate-ping" style={{ animationDuration: '3.5s' }} />
            <circle cx="70%" cy="75%" r="3" fill="#f472b6" />

            <circle cx="35%" cy="70%" r="4" fill="#10b981" className="animate-ping" style={{ animationDuration: '4.5s' }} />
            <circle cx="35%" cy="70%" r="3" fill="#34d399" />
          </svg>

          {/* 绝对定位的浮动标签 */}
          <div className="absolute left-[8%] top-[12%] animate-pulse rounded-lg border border-white/5 bg-slate-950/80 px-3 py-1.5 text-xs text-slate-300 backdrop-blur shadow-lg" style={{ animationDuration: '4s' }}>
            📥 Inbox 快摘
          </div>
          <div className="absolute left-[40%] top-[40%] animate-pulse rounded-lg border border-blue-500/20 bg-blue-950/80 px-3 py-1.5 text-xs font-semibold text-blue-400 backdrop-blur shadow-lg" style={{ animationDuration: '5s' }}>
            🌲 横向知识树
          </div>
          <div className="absolute right-[5%] top-[25%] animate-pulse rounded-lg border border-white/5 bg-slate-950/80 px-3 py-1.5 text-xs text-slate-300 backdrop-blur shadow-lg" style={{ animationDuration: '6s' }}>
            📝 大纲双链联动
          </div>
          <div className="absolute left-[28%] bottom-[12%] animate-pulse rounded-lg border border-white/5 bg-slate-950/80 px-3 py-1.5 text-xs text-slate-300 backdrop-blur shadow-lg" style={{ animationDuration: '4.5s' }}>
            📲 离线 Room+Sync
          </div>

          <div className="absolute bottom-3 right-4 text-[9px] text-slate-600 font-mono tracking-wider">星空网状知识结构</div>
        </div>

        {/* 底部署名 */}
        <div className="relative z-10 flex items-center justify-between text-xs text-slate-500 font-medium">
          <span>GleanRead 个人大脑实验室</span>
          <span className="flex items-center gap-1.5">
            <ShieldCheck size={13} className="text-emerald-500" />
            数据 100% 本地归属
          </span>
        </div>
      </section>

      {/* 右侧：极简高级磨砂登录面板 (5格宽) */}
      <section className="flex min-h-screen lg:col-span-5 items-center justify-center p-6 sm:p-10 relative bg-slate-950">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_70%_80%,rgb(6,182,212,0.06),transparent_40%)]" />
        
        <div className="relative z-10 w-full max-w-md rounded-2xl border border-white/5 bg-slate-900/40 p-8 shadow-2xl backdrop-blur-xl shadow-black/40">
          
          {/* Logo 顶栏 */}
          <div className="mb-8 flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-blue-500 to-cyan-400 text-sm font-bold text-white shadow-lg shadow-blue-500/20">
              G
            </div>
            <div>
              <div className="text-base font-bold tracking-tight text-white">GleanRead 登录中心</div>
              <div className="text-xs text-slate-400">开启你的可视化高效知识沉淀</div>
            </div>
          </div>

          {/* 带滑动背景条的页签指示器 */}
          <div className="relative mb-6 grid grid-cols-3 gap-1 rounded-xl bg-slate-950/60 p-1 border border-white/5">
            {/* 滑动滑块 */}
            <div
              className="absolute top-1 bottom-1 rounded-lg bg-slate-800 transition-all duration-300 ease-out shadow-[0_2px_8px_rgba(0,0,0,0.4)]"
              style={{
                left: panel === "login" ? "4px" : panel === "signup" ? "calc(33.333% + 2px)" : "calc(66.666% + 2px)",
                width: "calc(33.333% - 6px)",
              }}
            />
            {/* 标签选项 */}
            {tabs.map((tab) => (
              <button
                key={tab.id}
                type="button"
                className={cx(
                  "relative z-10 rounded-lg py-2 text-xs font-semibold tracking-wide transition-colors duration-200",
                  panel === tab.id ? "text-white" : "text-slate-400 hover:text-slate-200"
                )}
                onClick={() => setPanel(tab.id)}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* 表单输入区域 */}
          <div className="space-y-4">
            <div className="space-y-1.5">
              <label htmlFor="email-input" className="text-xs font-semibold tracking-wider text-slate-400 block uppercase">电子邮箱</label>
              <div className="relative flex items-center">
                <span className="absolute left-3.5 text-slate-500">
                  <Mail size={15} />
                </span>
                <input
                  id="email-input"
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="请输入您的邮箱地址"
                  className="w-full rounded-xl border border-white/5 bg-slate-950 pl-10 pr-4 py-2.5 text-xs text-white outline-none transition placeholder:text-slate-600 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                />
              </div>
            </div>

            {panel !== "magic" ? (
              <div className="space-y-1.5 animate-fadeIn">
                <label htmlFor="password-input" className="text-xs font-semibold tracking-wider text-slate-400 block uppercase">密码</label>
                <div className="relative flex items-center">
                  <span className="absolute left-3.5 text-slate-500">
                    <KeyRound size={15} />
                  </span>
                  <input
                    id="password-input"
                    type={showPassword ? "text" : "password"}
                    autoComplete={panel === "signup" ? "new-password" : "current-password"}
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    placeholder="请输入登录密码"
                    className="w-full rounded-xl border border-white/5 bg-slate-950 pl-10 pr-10 py-2.5 text-xs text-white outline-none transition placeholder:text-slate-600 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3.5 text-slate-500 hover:text-slate-300 transition focus:outline-none flex items-center justify-center p-1 rounded"
                    title={showPassword ? "隐藏密码" : "显示密码"}
                  >
                    {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                  </button>
                </div>
              </div>
            ) : null}

            {panel === "signup" ? (
              <div className="space-y-1.5 animate-fadeIn">
                <label htmlFor="confirm-password-input" className="text-xs font-semibold tracking-wider text-slate-400 block uppercase">确认密码</label>
                <div className="relative flex items-center">
                  <span className="absolute left-3.5 text-slate-500">
                    <KeyRound size={15} />
                  </span>
                  <input
                    id="confirm-password-input"
                    type={showConfirmPassword ? "text" : "password"}
                    autoComplete="new-password"
                    value={confirmPassword}
                    onChange={(event) => setConfirmPassword(event.target.value)}
                    placeholder="请再次确认登录密码"
                    className="w-full rounded-xl border border-white/5 bg-slate-950 pl-10 pr-10 py-2.5 text-xs text-white outline-none transition placeholder:text-slate-600 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3.5 text-slate-500 hover:text-slate-300 transition focus:outline-none flex items-center justify-center p-1 rounded"
                    title={showConfirmPassword ? "隐藏密码" : "显示密码"}
                  >
                    {showConfirmPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                  </button>
                </div>
              </div>
            ) : null}
          </div>

          {/* 状态与错误提示 */}
          {error ? (
            <div
              className="mt-4 rounded-xl border border-red-500/20 bg-red-500/5 p-3 text-xs text-red-400 leading-relaxed animate-shake"
              data-testid="auth-error"
            >
              {error}
            </div>
          ) : null}
          {status ? (
            <div className="mt-4 rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-3 text-xs text-emerald-400 leading-relaxed flex items-start gap-2">
              <CheckCircle size={14} className="mt-0.5 shrink-0" />
              <span>{status}</span>
            </div>
          ) : null}

          {/* 主提交按钮 */}
          <button
            type="button"
            data-testid="login-submit"
            disabled={submitting}
            onClick={() => void submit()}
            className="mt-6 h-11 w-full inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-500 to-cyan-500 text-xs font-bold text-white shadow-xl shadow-blue-500/10 transition hover:opacity-95 active:scale-[0.98] disabled:opacity-50 disabled:active:scale-100"
          >
            {panel === "magic" ? <Mail size={14} /> : <KeyRound size={14} />}
            {panel === "signup" ? "注册并进入大厅" : panel === "magic" ? "发送 Magic Link 登录" : "立即安全登录"}
          </button>

          {/* 安卓端同步提醒 */}
          <div className="mt-8 pt-6 border-t border-white/5 flex items-start gap-2.5 text-[10px] text-slate-500">
            <Smartphone size={15} className="text-cyan-500 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold text-slate-400 block mb-0.5">跨平台无缝同步</span>
              无需额外操作，同一个账号可自动同步您安卓客户端的离线收集大纲。
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
