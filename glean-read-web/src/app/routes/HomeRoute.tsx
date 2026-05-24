import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  Brain,
  Layers,
  Sparkles,
  Smartphone,
  Download,
  Github,
  Cpu,
  Database,
  Network,
  Infinity as InfinityIcon,
  CheckCircle2,
  Zap,
  Code,
  Lock,
  GitBranch,
} from "lucide-react";
import { useAuth } from "@/app/providers/AuthProvider";

export function HomeRoute() {
  const { session } = useAuth();
  const [sandboxStep, setSandboxStep] = useState(0);

  // 迷你工作台动态交互沙盒演示逻辑
  useEffect(() => {
    const timer = setInterval(() => {
      setSandboxStep((prev) => (prev + 1) % 5);
    }, 3000);
    return () => clearInterval(timer);
  }, []);

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100 overflow-x-hidden font-sans selection:bg-blue-500/30 selection:text-blue-200">
      {/* 顶部现代化悬浮毛玻璃导航栏 */}
      <header className="fixed top-4 inset-x-4 z-50 mx-auto max-w-7xl">
        <div className="flex h-16 items-center justify-between rounded-2xl border border-white/5 bg-slate-900/60 px-6 backdrop-blur-xl shadow-[0_8px_32px_rgba(0,0,0,0.4)]">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-blue-500 to-cyan-400 text-sm font-bold text-white shadow-lg shadow-blue-500/20">
              G
            </div>
            <span className="text-lg font-bold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
              GleanRead
            </span>
          </div>

          <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-slate-400">
            <a href="#features" className="transition hover:text-white">核心功能</a>
            <a href="#android" className="transition hover:text-white">安卓原生端</a>
            <a href="#sync" className="transition hover:text-white">多端同步</a>
            <a href="#download" className="transition hover:text-white">开源与下载</a>
          </nav>

          <div className="flex items-center gap-3">
            <a
              href="https://github.com/banafish/glean-read"
              target="_blank"
              rel="noreferrer"
              className="flex items-center justify-center h-9 w-9 rounded-xl border border-white/5 bg-slate-800/40 text-slate-400 transition hover:bg-slate-800 hover:text-white"
              title="GitHub 仓库"
            >
              <Github size={18} />
            </a>
            <Link
              to="/login"
              className="text-sm font-medium text-slate-300 transition hover:text-white px-3 py-1.5"
            >
              登录
            </Link>
            <Link
              to={session ? "/app" : "/login"}
              className="inline-flex h-9 items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-blue-500 to-cyan-500 px-4 text-xs font-semibold text-white shadow-lg shadow-blue-500/25 transition hover:opacity-95 active:scale-95"
            >
              免费开始
              <ArrowRight size={13} />
            </Link>
          </div>
        </div>
      </header>

      {/* Hero 区域：发光星云与科技网格 */}
      <section className="relative flex min-h-screen flex-col items-center justify-center px-6 pt-24 pb-16 overflow-hidden">
        {/* 背景科技网格与发光星云 */}
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#0f172a_1px,transparent_1px),linear-gradient(to_bottom,#0f172a_1px,transparent_1px)] bg-[size:4rem_4rem] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_50%,#000_70%,transparent_100%)] opacity-60" />
        <div className="absolute top-[20%] left-[10%] h-[350px] w-[350px] rounded-full bg-blue-500/10 blur-[120px] animate-pulse" />
        <div className="absolute bottom-[20%] right-[10%] h-[350px] w-[350px] rounded-full bg-cyan-500/10 blur-[120px]" />

        <div className="relative z-10 mx-auto max-w-4xl text-center">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-blue-500/20 bg-blue-500/5 px-4 py-1.5 text-xs font-medium text-blue-400 shadow-[0_0_15px_rgba(59,130,246,0.1)] backdrop-blur">
            <Sparkles size={13} className="animate-spin" style={{ animationDuration: '3s' }} />
            本地优先视觉知识工作台
          </div>

          <h1 className="text-4xl font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-b from-white via-slate-100 to-slate-400 sm:text-6xl md:text-7xl leading-none">
            连接碎片灵感 <br className="hidden sm:inline" />
            构建你的<span className="bg-gradient-to-r from-blue-400 to-cyan-400 bg-clip-text">可视化知识大脑</span>
          </h1>

          <p className="mx-auto mt-6 max-w-2xl text-base leading-relaxed text-slate-400 sm:text-lg">
            GleanRead 重构了阅读思考流。在 Web 端，用横向知识树编织大纲，让碎片知识有机挂载；在 Android 原生端，随手极速收集，离线无阻碍。基于 Supabase 多端自动秒级同步，完全开源，隐私至上。
          </p>

          <div className="mt-10 flex flex-wrap justify-center gap-4">
            <Link
              to={session ? "/app" : "/login"}
              className="inline-flex h-12 items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-500 to-cyan-500 px-6 text-sm font-semibold text-white shadow-xl shadow-blue-500/25 transition hover:opacity-95 hover:shadow-blue-500/35 active:scale-[0.98]"
            >
              免费开始使用 (Web端)
              <ArrowRight size={16} />
            </Link>
            <a
              href="#download"
              className="inline-flex h-12 items-center justify-center gap-2 rounded-xl border border-white/10 bg-slate-900/80 px-6 text-sm font-semibold text-slate-300 transition hover:bg-slate-800 hover:text-white hover:border-white/20 active:scale-[0.98]"
            >
              <Smartphone size={16} className="text-cyan-400" />
              下载安卓原生 App
            </a>
          </div>
        </div>

        {/* 动态交互迷你工作台沙盒 (Live Demo Sandbox) */}
        <div className="relative z-10 mt-16 w-full max-w-5xl rounded-2xl border border-white/5 bg-slate-900/60 p-1 shadow-2xl backdrop-blur shadow-black/80">
          <div className="flex h-6 items-center gap-1.5 rounded-t-xl bg-slate-900/80 px-4 border-b border-white/5">
            <span className="h-2 w-2 rounded-full bg-red-500/60" />
            <span className="h-2 w-2 rounded-full bg-yellow-500/60" />
            <span className="h-2 w-2 rounded-full bg-green-500/60" />
            <span className="ml-2 text-[10px] text-slate-500 font-mono">glean-read-workbench.io</span>
          </div>

          <div className="grid h-[280px] grid-cols-[180px_1fr_240px] overflow-hidden bg-slate-950/80 rounded-b-xl border border-white/5">
            {/* 左侧 Inbox */}
            <div className="border-r border-white/5 p-4 flex flex-col justify-between bg-slate-900/20">
              <div>
                <span className="text-[10px] font-bold tracking-wider text-slate-500 uppercase">Inbox 碎片收集</span>
                <div className="mt-3 space-y-2">
                  <div
                    className={`rounded-lg border p-2 transition-all duration-500 ${
                      sandboxStep === 0
                        ? "border-blue-500/40 bg-blue-500/5 shadow-[0_0_12px_rgba(59,130,246,0.15)] scale-102"
                        : "border-white/5 bg-slate-900/40"
                    }`}
                  >
                    <div className="h-2 w-3/4 rounded bg-slate-200/30" />
                    <div className="mt-2 h-1.5 w-full rounded bg-slate-500/20" />
                  </div>
                  <div className="rounded-lg border border-white/5 bg-slate-900/40 p-2 opacity-50">
                    <div className="h-2 w-1/2 rounded bg-slate-200/20" />
                    <div className="mt-2 h-1.5 w-2/3 rounded bg-slate-500/10" />
                  </div>
                </div>
              </div>
              <div className="text-[9px] text-slate-600 font-mono">Web / Android 统一收纳</div>
            </div>

            {/* 中间 ReactFlow 画布 */}
            <div className="relative p-4 overflow-hidden flex items-center justify-center">
              {/* 网格点背景 */}
              <div className="absolute inset-0 bg-[radial-gradient(rgba(255,255,255,0.03)_1px,transparent_1px)] bg-[size:10px_10px]" />
              
              {/* 连线路径 */}
              <svg className="absolute inset-0 h-full w-full pointer-events-none">
                <path
                  d="M 40 120 Q 120 120 160 80"
                  fill="none"
                  stroke={sandboxStep >= 1 ? "#3b82f6" : "#1e293b"}
                  strokeWidth="2"
                  className="transition-colors duration-500"
                />
                <path
                  d="M 40 120 Q 120 120 160 160"
                  fill="none"
                  stroke="#1e293b"
                  strokeWidth="2"
                />
              </svg>

              {/* 虚拟根节点 */}
              <div className="absolute left-4 top-1/2 -translate-y-1/2 rounded-lg border border-white/10 bg-slate-900 px-3 py-1.5 text-[10px] font-semibold text-slate-300">
                知识宇宙
              </div>

              {/* 右侧两个挂载点 */}
              <div
                className={`absolute left-[160px] top-[50px] rounded-lg border transition-all duration-500 px-3 py-2 ${
                  sandboxStep >= 2
                    ? "border-blue-500/50 bg-blue-500/10 text-white shadow-[0_0_15px_rgba(59,130,246,0.2)]"
                    : "border-white/5 bg-slate-900 text-slate-400"
                }`}
              >
                <div className="text-[10px] font-semibold">本地优先同步</div>
                <div className="mt-1 flex items-center gap-1.5">
                  <span className="text-[8px] bg-slate-800 px-1 rounded text-slate-500">大纲</span>
                  <span
                    className={`text-[8px] px-1 rounded transition-colors ${
                      sandboxStep >= 2 ? "bg-blue-500/30 text-blue-300" : "bg-slate-800 text-slate-500"
                    }`}
                  >
                    摘录: {sandboxStep >= 2 ? "1" : "0"}
                  </span>
                </div>
              </div>

              <div className="absolute left-[160px] top-[140px] rounded-lg border border-white/5 bg-slate-900 px-3 py-2 text-slate-400 opacity-60">
                <div className="text-[10px] font-semibold">安卓 Material You</div>
                <div className="mt-1 flex gap-1.5 text-[8px] text-slate-500">
                  <span>节点</span>
                  <span>摘录: 0</span>
                </div>
              </div>

              {/* 模拟从 Inbox 飞到知识节点的摘录卡片 */}
              {sandboxStep === 1 && (
                <div className="absolute left-[-20px] top-[110px] h-6 w-24 rounded border border-blue-500/60 bg-blue-950 px-2 py-1 text-[8px] text-blue-300 shadow-lg animate-ping-once transition-all duration-1000 ease-out" style={{ transform: 'translate(130px, -40px)', transition: 'all 1s ease-in-out' }}>
                  挂载碎片中...
                </div>
              )}
            </div>

            {/* 右侧 Tiptap 编辑器大纲 */}
            <div className="border-l border-white/5 p-4 flex flex-col justify-between bg-slate-900/20">
              <div>
                <span className="text-[10px] font-bold tracking-wider text-slate-500 uppercase">大纲沉淀编辑器</span>
                <div className="mt-3 space-y-3 font-mono">
                  <div className="h-2.5 w-1/3 rounded bg-blue-500/20" />
                  <div className="space-y-1.5">
                    <div className="h-1.5 w-full rounded bg-slate-700/30" />
                    <div className="h-1.5 w-5/6 rounded bg-slate-700/30" />
                    {sandboxStep >= 3 && (
                      <div className="h-7 rounded border border-blue-500/20 bg-blue-500/5 p-1.5 text-[7px] text-blue-300 transition-all duration-700 animate-fadeIn">
                        ⚡ Excerpt: "本地 Room & Dexie 实现断网流畅运行..."
                      </div>
                    )}
                  </div>
                </div>
              </div>
              <div className="text-[9px] text-slate-600 font-mono">双绑定自动同步</div>
            </div>
          </div>
        </div>
      </section>

      {/* 核心卖点 Bento Grid 网格 */}
      <section id="features" className="relative py-24 px-6 max-w-7xl mx-auto">
        <div className="text-center max-w-2xl mx-auto mb-16">
          <h2 className="text-3xl font-extrabold tracking-tight text-white sm:text-4xl">
            极致打磨的硬核核心卖点
          </h2>
          <p className="mt-4 text-slate-400 text-sm sm:text-base">
            融合 Web 的大屏深度建树，与 Android 端的随时随地极速捕捉，为您创造全方位的无损知识链。
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Card 1: 横向知识树 */}
          <div className="group relative rounded-2xl border border-white/5 bg-slate-900/40 p-8 shadow-sm backdrop-blur transition-all duration-300 hover:-translate-y-1 hover:border-blue-500/30 hover:bg-slate-900/60 hover:shadow-blue-500/5">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-500/10 text-blue-400 transition-transform group-hover:scale-110 group-hover:rotate-6">
              <Brain size={24} />
            </div>
            <h3 className="mt-5 text-lg font-bold text-white">从左至右横向知识树</h3>
            <p className="mt-3 text-sm leading-relaxed text-slate-400">
              基于 ReactFlow Canvas 深度定制，突破传统文件夹树状列表。通过 `Tab`（建子节点）与 `Enter`（建兄弟节点）的键盘优先操作，极速构建立体的脑图大纲。
            </p>
            <div className="mt-4 inline-flex items-center gap-1.5 text-xs text-blue-400 font-medium">
              稀疏排序引擎稳定不抖动
            </div>
          </div>

          {/* Card 2: 本地优先与离线驱动 */}
          <div className="group relative rounded-2xl border border-white/5 bg-slate-900/40 p-8 shadow-sm backdrop-blur transition-all duration-300 hover:-translate-y-1 hover:border-cyan-500/30 hover:bg-slate-900/60 hover:shadow-cyan-500/5">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-cyan-500/10 text-cyan-400 transition-transform group-hover:scale-110 group-hover:rotate-6">
              <Database size={24} />
            </div>
            <h3 className="mt-5 text-lg font-bold text-white">本地优先离线驱动</h3>
            <p className="mt-3 text-sm leading-relaxed text-slate-400">
              Web 端使用 Dexie (IndexedDB) 本地数据库，Android 原生端基于 Room 本地数据库，所有查阅与数据流转完全从本地极速派生，断网可用，带来飞一般的响应速度。
            </p>
            <div className="mt-4 inline-flex items-center gap-1.5 text-xs text-cyan-400 font-medium">
              100% 隐私安全与数据本地归属
            </div>
          </div>

          {/* Card 3: 双链联动大纲 */}
          <div className="group relative rounded-2xl border border-white/5 bg-slate-900/40 p-8 shadow-sm backdrop-blur transition-all duration-300 hover:-translate-y-1 hover:border-purple-500/30 hover:bg-slate-900/60 hover:shadow-purple-500/5">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-purple-500/10 text-purple-400 transition-transform group-hover:scale-110 group-hover:rotate-6">
              <Layers size={24} />
            </div>
            <h3 className="mt-5 text-lg font-bold text-white">大纲双链抽屉编辑器</h3>
            <p className="mt-3 text-sm leading-relaxed text-slate-400">
              侧边拉出 Tiptap 驱动的富文本大纲编辑器，与画布节点双向绑定。支持独特的行内链接编辑器 (Inline Link Composer)，快捷建立卡片与卡片间的星链网状关联。
            </p>
            <div className="mt-4 inline-flex items-center gap-1.5 text-xs text-purple-400 font-medium">
              配合局部关系图谱深度定位
            </div>
          </div>

          {/* Card 4: Android 极速桌面挂件 */}
          <div className="group relative rounded-2xl border border-white/5 bg-slate-900/40 p-8 shadow-sm backdrop-blur transition-all duration-300 hover:-translate-y-1 hover:border-emerald-500/30 hover:bg-slate-900/60 hover:shadow-emerald-500/5 md:col-span-2">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-400 transition-transform group-hover:scale-110 group-hover:rotate-6">
              <Smartphone size={24} />
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 mt-5">
              <div>
                <h3 className="text-lg font-bold text-white">Android 桌面小挂件与剪裁</h3>
                <p className="mt-3 text-sm leading-relaxed text-slate-400">
                  安卓端拥有专门设计的桌面快速捕获小部件 (Fast Capture Widget) 和底部拉起的快速记录单。随时捕获粘贴板，甚至可在系统分享中直接投递摘录，完全免开应用，省时省力。
                </p>
              </div>
              <div className="flex flex-col justify-center space-y-2 border-l border-white/5 pl-6">
                <div className="flex items-center gap-2 text-xs text-slate-300">
                  <CheckCircle2 size={14} className="text-emerald-400" />
                  智能提取网页标题 URL
                </div>
                <div className="flex items-center gap-2 text-xs text-slate-300">
                  <CheckCircle2 size={14} className="text-emerald-400" />
                  智能分类进入全局 Inbox
                </div>
                <div className="flex items-center gap-2 text-xs text-slate-300">
                  <CheckCircle2 size={14} className="text-emerald-400" />
                  Room 存储并后台静默队列同步
                </div>
              </div>
            </div>
          </div>

          {/* Card 5: AI 提炼与 AI 综合树 */}
          <div className="group relative rounded-2xl border border-white/5 bg-slate-900/40 p-8 shadow-sm backdrop-blur transition-all duration-300 hover:-translate-y-1 hover:border-rose-500/30 hover:bg-slate-900/60 hover:shadow-rose-500/5">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-rose-500/10 text-rose-400 transition-transform group-hover:scale-110 group-hover:rotate-6">
              <Cpu size={24} />
            </div>
            <h3 className="mt-5 text-lg font-bold text-white">AI 批量生成与综合树</h3>
            <p className="mt-3 text-sm leading-relaxed text-slate-400">
              本地/云端 LLM 大模型双向整合。AI 批量大纲工作流可将杂乱无序的 Inbox 摘录智能分析并秒级提取，利用 AI 综合知识树自动合并同类节点，完成逻辑整理。
            </p>
            <div className="mt-4 inline-flex items-center gap-1.5 text-xs text-rose-400 font-medium">
              一键精炼化零为整
            </div>
          </div>
        </div>
      </section>

      {/* 安卓客户端与多端同步展示 */}
      <section id="android" className="relative py-24 bg-slate-900/30 border-y border-white/5">
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 h-[450px] w-[450px] rounded-full bg-blue-500/5 blur-[120px] pointer-events-none" />
        
        <div className="mx-auto max-w-7xl px-6 grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          {/* 左侧文字介绍 */}
          <div className="space-y-6">
            <div className="inline-flex items-center gap-2 rounded-full border border-cyan-500/20 bg-cyan-500/5 px-4 py-1 text-xs font-semibold text-cyan-400">
              <Smartphone size={13} />
              原生 Android 原生级美学
            </div>
            <h2 className="text-3xl font-extrabold tracking-tight text-white sm:text-4xl">
              优雅的 Android 原生客户端 <br />
              <span className="bg-gradient-to-r from-blue-400 to-cyan-400 bg-clip-text text-transparent">深度贯彻 Material You 动态色彩</span>
            </h2>
            <p className="text-slate-400 text-base leading-relaxed">
              这是根据仓库中严格的 Android UI 与 Jetpack Compose 规范开发的原生应用。UI 采用最新的 Material Design 3 基准，并完美自适应壁纸色彩——“色随壁动”，每一次滑动和手势回弹都流畅细腻。
            </p>

            <div className="grid grid-cols-2 gap-4">
              <div className="rounded-xl border border-white/5 bg-slate-950 p-4">
                <Zap className="text-cyan-400" size={20} />
                <h4 className="mt-3 font-semibold text-white text-sm">随时随地随手记</h4>
                <p className="mt-1 text-xs text-slate-400 leading-relaxed">无需联网，随想随写，利用本地 Room 数据库安全可靠暂存。</p>
              </div>
              <div className="rounded-xl border border-white/5 bg-slate-950 p-4">
                <Network className="text-blue-400" size={20} />
                <h4 className="mt-3 font-semibold text-white text-sm">Supabase 秒级同步</h4>
                <p className="mt-1 text-xs text-slate-400 leading-relaxed">自动侦测网络并启用离线队列，实现双端数据绝对一致。</p>
              </div>
            </div>
          </div>

          {/* 右侧纯 CSS 手机模型 */}
          <div className="relative flex justify-center">
            {/* 炫光阴影 */}
            <div className="absolute top-[10%] h-[360px] w-[240px] rounded-full bg-cyan-500/20 blur-[60px]" />
            
            {/* 手机容器 */}
            <div className="relative z-10 w-[270px] h-[540px] rounded-[48px] border-[10px] border-slate-800 bg-slate-950 shadow-2xl overflow-hidden shadow-black ring-1 ring-white/10">
              {/* 听筒 & 摄像头 (Dynamic Island 占位) */}
              <div className="absolute top-3 left-1/2 -translate-x-1/2 h-4 w-20 rounded-full bg-slate-900 z-30 flex items-center justify-center">
                <span className="h-1.5 w-1.5 rounded-full bg-slate-800/80 mr-2" />
                <span className="h-1 w-1 rounded-full bg-blue-900/60" />
              </div>

              {/* 模拟手机系统界面：Compose 原生 Material You 页面 */}
              <div className="h-full w-full bg-cyan-950/15 pt-8 px-4 flex flex-col justify-between font-sans">
                {/* 顶部模拟 AppBar */}
                <div className="flex items-center justify-between border-b border-white/5 pb-2.5">
                  <div>
                    <span className="text-[8px] font-mono text-slate-500 block">LOCAL-FIRST</span>
                    <span className="text-[12px] font-extrabold text-white">GleanRead App</span>
                  </div>
                  <span className="h-2 w-2 rounded-full bg-emerald-500 shadow-[0_0_8px_#10b981]" />
                </div>

                {/* 模拟 Inbox 摘录流 */}
                <div className="flex-1 py-4 space-y-3 overflow-hidden">
                  <span className="text-[9px] font-bold text-cyan-400 block tracking-wider uppercase">Inbox 快摘列表</span>
                  <div className="rounded-xl border border-white/5 bg-slate-900/40 p-2.5 space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="text-[8px] text-cyan-300 font-mono">2026-05-24</span>
                      <span className="text-[7px] bg-slate-800 px-1 rounded text-slate-400">已同步</span>
                    </div>
                    <p className="text-[10px] text-slate-200 leading-tight">“在 Compose UI 原生手势中，双端数据基于 Supabase 队列进行秒级状态迁移...”</p>
                    <div className="text-[7px] text-slate-500 truncate">Source: openspec/specs/local-store</div>
                  </div>

                  <div className="rounded-xl border border-white/5 bg-slate-900/40 p-2.5 space-y-1.5 opacity-60">
                    <div className="h-2 w-1/3 rounded bg-slate-800" />
                    <div className="h-3.5 rounded bg-slate-800/50" />
                  </div>
                </div>

                {/* 底部模拟快速捕获输入框 */}
                <div className="border-t border-white/5 py-3 bg-slate-950/80 -mx-4 px-4 space-y-2">
                  <div className="rounded-lg bg-cyan-900/20 border border-cyan-500/20 p-1.5 flex items-center justify-between">
                    <span className="text-[9px] text-cyan-200/70">剪贴板中检测到链接...</span>
                    <span className="text-[8px] bg-cyan-500 text-white font-bold px-1.5 py-0.5 rounded">快速剪藏</span>
                  </div>
                  <div className="flex h-8 items-center justify-between rounded-xl bg-slate-900 px-3 border border-white/5">
                    <span className="text-[9px] text-slate-500">输入想法或网址...</span>
                    <div className="h-4 w-4 rounded-full bg-blue-500 flex items-center justify-center text-white text-[8px] font-bold">+</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 云端秒级同步机制 */}
      <section id="sync" className="relative py-24 px-6 max-w-5xl mx-auto text-center">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-500/10 text-blue-400 mx-auto">
          <InfinityIcon size={24} className="animate-pulse" />
        </div>
        <h2 className="mt-5 text-3xl font-extrabold text-white">双端混合同步引擎 (Hybrid Cloud Sync)</h2>
        <p className="mt-4 max-w-2xl mx-auto text-slate-400 text-sm leading-relaxed sm:text-base">
          基于 `sync_status`（已同步、待新建、待更新、待删除）离线队列控制，当设备检测到网络恢复时，智能锁死本地 `sync_queue`，毫秒级上传云端并自动完成双向合并，确保 Web 与 Android 端完美契合。
        </p>
      </section>

      {/* 纯净的下载与开源板块 */}
      <section id="download" className="relative py-24 px-6 bg-slate-900/10 border-t border-white/5">
        <div className="mx-auto max-w-5xl rounded-3xl border border-white/5 bg-slate-900/40 p-8 md:p-12 shadow-2xl backdrop-blur">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
            {/* 左侧下载与仓库链接 */}
            <div className="lg:col-span-7 space-y-6">
              <h2 className="text-3xl font-extrabold text-white">下载安卓 App & 开源贡献</h2>
              <p className="text-slate-400 text-sm leading-relaxed">
                本着安全、透明与隐私自主精神，GleanRead 承诺代码完全开源。我们不在应用商店收集任何个人敏感数据，仅提供直链包下载与 GitHub 社区发布通道。
              </p>

              <div className="flex flex-col sm:flex-row gap-4">
                <a
                  href="/download/glean-read.apk"
                  className="inline-flex h-12 items-center justify-center gap-2.5 rounded-xl bg-gradient-to-r from-blue-500 to-cyan-500 px-6 text-sm font-semibold text-white shadow-xl shadow-blue-500/20 transition hover:opacity-95 hover:shadow-blue-500/30 active:scale-[0.98]"
                >
                  <Download size={16} />
                  直接下载安卓 APK
                </a>
                <a
                  href="https://github.com/banafish/glean-read/releases"
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex h-12 items-center justify-center gap-2.5 rounded-xl border border-white/10 bg-slate-950 px-6 text-sm font-semibold text-slate-300 transition hover:bg-slate-900 hover:text-white active:scale-[0.98]"
                >
                  <Github size={16} />
                  GitHub Releases 下载
                </a>
              </div>

              {/* GitHub 仓库高亮板块 */}
              <div className="pt-6 border-t border-white/5">
                <div className="flex items-center gap-6">
                  <a
                    href="https://github.com/banafish/glean-read"
                    target="_blank"
                    rel="noreferrer"
                    className="flex items-center gap-2 text-sm text-blue-400 font-bold hover:text-blue-300 transition"
                  >
                    <Code size={16} />
                    GitHub 开源主仓
                    <ArrowRight size={14} />
                  </a>
                  <div className="flex items-center gap-1.5 text-xs text-slate-400">
                    <Lock size={12} className="text-emerald-400" />
                    接受社区审计 / 100% 自主掌控数据
                  </div>
                </div>
              </div>
            </div>

            {/* 右侧二维码与极客徽章 */}
            <div className="lg:col-span-5 flex flex-col items-center justify-center border-t lg:border-t-0 lg:border-l border-white/5 pt-8 lg:pt-0 lg:pl-8 space-y-4">
              <div className="relative p-2.5 bg-white rounded-2xl shadow-xl shadow-black/40">
                {/* 纯 CSS 模拟二维码 */}
                <div className="h-32 w-32 border-4 border-slate-950 flex flex-wrap p-1 gap-1 bg-white">
                  {[...Array(64)].map((_, i) => {
                    const isCorner =
                      (i < 3 && i % 8 < 3) || // top-left
                      (i > 55 && i % 8 < 3) || // bottom-left
                      (i < 3 && i % 8 > 4); // top-right
                    return (
                      <div
                        key={i}
                        className={`h-3 w-3 rounded-[1px] ${
                          isCorner
                            ? "bg-slate-950"
                            : i % 3 === 0 || i % 7 === 0
                            ? "bg-slate-950"
                            : "bg-transparent"
                        }`}
                      />
                    );
                  })}
                </div>
                <div className="absolute inset-0 m-auto h-8 w-8 rounded-lg bg-gradient-to-tr from-blue-500 to-cyan-400 border-2 border-white flex items-center justify-center text-[10px] font-bold text-white shadow">
                  G
                </div>
              </div>
              <span className="text-[11px] text-slate-400 font-medium tracking-wide">扫码极速下载安卓原生 APK</span>
            </div>
          </div>
        </div>
      </section>

      {/* 页脚 */}
      <footer className="border-t border-white/5 py-12 px-6 bg-slate-950 text-slate-500 text-xs">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row justify-between items-center gap-6">
          <div className="flex items-center gap-2">
            <span className="font-bold text-slate-300">GleanRead</span>
            <span>-</span>
            <span>让灵感自由流淌</span>
          </div>

          <div className="flex gap-6 text-slate-400">
            <a href="#features" className="hover:text-white transition">核心功能</a>
            <a href="#android" className="hover:text-white transition">Android原生端</a>
            <a href="#download" className="hover:text-white transition">APK下载</a>
            <a href="https://github.com/banafish/glean-read" target="_blank" rel="noreferrer" className="hover:text-white transition">GitHub仓库</a>
          </div>

          <div>
            © 2026 GleanRead Project. Apache 2.0 Licensed.
          </div>
        </div>
      </footer>
    </main>
  );
}
