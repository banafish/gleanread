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
  Home as HomeIcon,
  Tag as TagIcon,
  Settings as SettingsIcon,
  Search,
  MoreVertical,
  Plus,
  ChevronDown,
  ChevronRight,
  Filter,
  Folder,
  User,
  Sun,
  Moon,
  Paintbrush,
} from "lucide-react";
import { useAuth } from "@/app/providers/AuthProvider";

export function HomeRoute() {
  const { session } = useAuth();
  const [sandboxStep, setSandboxStep] = useState(0);
  const [activePhoneTab, setActivePhoneTab] = useState<"stream" | "tree" | "tags" | "settings">("tree");

  const apkDownloadUrl = import.meta.env.VITE_APK_DOWNLOAD_URL || '/download/glean-read.apk';

  // 智能拼接成绝对路径，以供手机扫码下载
  const getAbsoluteApkUrl = (url: string) => {
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url;
    }
    const origin = typeof window !== 'undefined' ? window.location.origin : '';
    const cleanUrl = url.startsWith('/') ? url : `/${url}`;
    return `${origin}${cleanUrl}`;
  };
  const absoluteApkUrl = getAbsoluteApkUrl(apkDownloadUrl);

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
            <img
              src="/icons/icon-192.png"
              alt="GleanRead Logo"
              className="h-9 w-9 rounded-xl object-contain shadow-lg shadow-blue-500/20"
            />
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

          <div className="flex items-center gap-1.5 sm:gap-3">
            <a
              href="https://github.com/banafish/glean-read"
              target="_blank"
              rel="noreferrer"
              className="hidden sm:flex items-center justify-center h-9 w-9 rounded-xl border border-white/5 bg-slate-800/40 text-slate-400 transition hover:bg-slate-800 hover:text-white"
              title="GitHub 仓库"
            >
              <Github size={18} />
            </a>
            <Link
              to="/login"
              className="text-sm font-medium text-slate-300 transition hover:text-white px-2.5 sm:px-3 py-1.5"
            >
              登录
            </Link>
            <Link
              to={session ? "/app" : "/login"}
              className="inline-flex h-9 items-center justify-center gap-1 sm:gap-1.5 rounded-xl bg-gradient-to-r from-blue-500 to-cyan-500 px-3 sm:px-4 text-xs font-semibold text-white shadow-lg shadow-blue-500/25 transition hover:opacity-95 active:scale-95 whitespace-nowrap"
            >
              <span className="hidden sm:inline">免费开始</span>
              <span className="sm:hidden">开始</span>
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

          <h1 className="text-3xl font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-b from-white via-slate-100 to-slate-400 sm:text-6xl md:text-7xl leading-tight sm:leading-none">
            <span className="inline-block">收集碎片化摘录知识</span> <br />
            <span className="inline-block">
              构建你的<span className="bg-gradient-to-r from-blue-400 to-cyan-400 bg-clip-text">可视化知识体系</span>
            </span>
          </h1>

          <p className="mx-auto mt-6 max-w-2xl text-base leading-relaxed text-slate-400 sm:text-lg">
            GleanRead 重构了阅读思考流。在 Web 端，用横向知识树编织大纲，让知识摘录有机挂载；在 Android 原生端，随手极速收集，离线无阻碍。基于 Supabase 多端自动秒级同步，完全开源，隐私至上。
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

          <div className="grid h-[280px] grid-cols-1 md:grid-cols-[180px_1fr_240px] overflow-hidden bg-slate-950/80 rounded-b-xl border border-white/5">
            {/* 左侧 Inbox */}
            <div className="hidden md:flex border-r border-white/5 p-4 flex-col justify-between bg-slate-900/20">
              <div>
                <span className="text-[10px] font-bold tracking-wider text-slate-500 uppercase">Inbox 摘录收集</span>
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
                  挂载摘录中...
                </div>
              )}
            </div>

            {/* 右侧 Tiptap 编辑器大纲 */}
            <div className="hidden md:flex border-l border-white/5 p-4 flex-col justify-between bg-slate-900/20">
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
            极致打磨的硬核核心功能
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
                <h3 className="text-lg font-bold text-white">Android 桌面小挂件与快速摘录</h3>
                <p className="mt-3 text-sm leading-relaxed text-slate-400 text-justify">
                  在安卓端，从浏览器选中一段文字点击分享到 app 会弹出一个快速摘录弹窗，您可以即时输入自己的想法，系统会自动补全文章的来源链接和标题。在碎片化阅读时，这能帮您快速收集知识摘录；有空时，再在 web 端把这些摘录打磨成结构化的知识体系。
                </p>
              </div>
              <div className="flex flex-col justify-center space-y-2 border-t border-white/5 pt-6 sm:border-t-0 sm:border-l sm:pl-6 sm:pt-0 mt-6 sm:mt-0">
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
            <h3 className="mt-5 text-lg font-bold text-white">AI 总结知识摘录形成知识大纲</h3>
            <p className="mt-3 text-sm leading-relaxed text-slate-400">
              深度融合 AI 大模型，一键对 Inbox 中杂乱分散的知识摘录进行提炼与逻辑总结。AI 会自动过滤噪音、提炼核心论点，生成清晰的知识大纲，并直接挂载作为您横向知识树节点的大纲内容。
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
            <div className="relative z-10 w-[270px] h-[540px] rounded-[48px] border-[10px] border-slate-800 bg-[#0C0F17] shadow-2xl overflow-hidden shadow-black ring-1 ring-white/10 select-none">
              {/* 局部微动画与局部样式定义，确保四屏切换动画丝滑并消除滚动条 */}
              <style>{`
                @keyframes phoneFadeIn {
                  from { opacity: 0; transform: translateY(4px); }
                  to { opacity: 1; transform: translateY(0); }
                }
                .animate-phoneFadeIn {
                  animation: phoneFadeIn 0.28s cubic-bezier(0.4, 0, 0.2, 1) forwards;
                }
                .scrollbar-none::-webkit-scrollbar {
                  display: none;
                }
                .scrollbar-none {
                  -ms-overflow-style: none;
                  scrollbar-width: none;
                }
              `}</style>

              {/* 听筒 & 摄像头 (Dynamic Island 占位) */}
              <div className="absolute top-3 left-1/2 -translate-x-1/2 h-4 w-20 rounded-full bg-slate-900 z-30 flex items-center justify-center">
                <span className="h-1.5 w-1.5 rounded-full bg-slate-800/80 mr-2" />
                <span className="h-1 w-1 rounded-full bg-blue-900/60" />
              </div>

              {/* 模拟手机系统界面：Compose 原生 Material You 页面 */}
              <div className="h-full w-full pt-8 flex flex-col justify-between font-sans text-slate-200 relative overflow-hidden bg-[#0C0F17]">
                
                {/* 屏幕核心内容滚动区 */}
                <div className="flex-1 overflow-y-auto px-3.5 py-2.5 space-y-3.5 scrollbar-none pb-20" style={{ scrollbarWidth: "none" }}>
                  {activePhoneTab === "tree" && (
                    <div className="space-y-3.5 animate-phoneFadeIn">
                      {/* AppBar */}
                      <div className="flex items-center justify-between py-1">
                        <span className="text-[14px] font-bold text-white tracking-wide">知识体系</span>
                        <div className="flex items-center gap-3.5 text-slate-300">
                          <Search size={14} className="cursor-pointer hover:text-white" />
                          <MoreVertical size={14} className="cursor-pointer hover:text-white" />
                        </div>
                      </div>

                      {/* 卡片 1 (展开状态) */}
                      <div className="bg-[#1A1F2C] rounded-2xl p-3 space-y-2.5 border border-white/5 text-left shadow-sm">
                        <div className="flex items-center justify-between text-[11px] font-bold text-slate-100">
                          <div className="flex items-center gap-1.5">
                            <ChevronDown size={12} className="text-slate-400" />
                            <span>🧠 个人成长</span>
                          </div>
                          <div className="flex items-center gap-2 text-slate-400">
                            <span>0条</span>
                            <MoreVertical size={10} className="opacity-80" />
                          </div>
                        </div>
                        {/* 子大纲 */}
                        <div className="pl-3.5 space-y-2 border-l border-slate-700/50 ml-1.5">
                          <div className="flex items-center justify-between text-[10px] text-slate-300">
                            <div className="flex items-center gap-1.5">
                              <span className="h-1 w-1 rounded-full bg-slate-400" />
                              <span>⏱️ 时间管理</span>
                            </div>
                            <div className="flex items-center gap-1 text-slate-500">
                              <span>0条</span>
                              <MoreVertical size={9} />
                            </div>
                          </div>
                          <div className="flex items-center justify-between text-[10px] text-slate-300">
                            <div className="flex items-center gap-1.5">
                              <span className="h-1 w-1 rounded-full bg-slate-400" />
                              <span>📖 阅读方法</span>
                            </div>
                            <div className="flex items-center gap-1 text-slate-500">
                              <span>2条</span>
                              <MoreVertical size={9} />
                            </div>
                          </div>
                        </div>
                      </div>

                      {/* 卡片 2 (折叠状态) */}
                      <div className="bg-[#1A1F2C] rounded-2xl p-3 flex items-center justify-between border border-white/5 text-left shadow-sm">
                        <div className="flex items-center gap-1.5 text-[11px] font-bold text-slate-100">
                          <span className="h-1.5 w-1.5 rounded-full bg-slate-400 ml-1 mr-1" />
                          <span>💻 技术开发</span>
                        </div>
                        <div className="flex items-center gap-2 text-slate-400 text-[10px]">
                          <span>0条</span>
                          <MoreVertical size={10} className="opacity-80" />
                        </div>
                      </div>

                      {/* 卡片 3 (折叠状态) */}
                      <div className="bg-[#1A1F2C] rounded-2xl p-3 flex items-center justify-between border border-white/5 text-left shadow-sm">
                        <div className="flex items-center gap-1.5 text-[11px] font-bold text-slate-100">
                          <span className="h-1.5 w-1.5 rounded-full bg-slate-400 ml-1 mr-1" />
                          <span>💰 投资理财</span>
                        </div>
                        <div className="flex items-center gap-2 text-slate-400 text-[10px]">
                          <span>0条</span>
                          <MoreVertical size={10} className="opacity-80" />
                        </div>
                      </div>
                    </div>
                  )}

                  {activePhoneTab === "stream" && (
                    <div className="space-y-3.5 animate-phoneFadeIn">
                      {/* 搜索过滤框 */}
                      <div className="py-0.5 space-y-2 text-left">
                        <div className="flex h-7.5 items-center justify-between rounded-full bg-[#1A1F2C] px-3.5 border border-white/5">
                          <span className="text-[10px] text-slate-500">搜索摘录、想法或标...</span>
                          <Filter size={11} className="text-slate-400" />
                        </div>
                        <span className="text-[8px] text-slate-500 block pl-1">长按卡片可进入多选</span>
                      </div>

                      {/* 摘录卡片 1 */}
                      <div className="bg-[#1A1F2C] rounded-2xl p-3 space-y-2.5 border border-white/5 text-left shadow-sm">
                        <div className="flex items-center gap-1.5">
                          <span className="text-[8px] bg-slate-800/80 px-1.5 py-0.5 rounded text-slate-300 font-bold">📖 阅读方法</span>
                          <span className="text-[8px] text-slate-400">#学习方法</span>
                        </div>
                        <p className="text-[10px] text-slate-200 leading-relaxed font-normal">
                          费曼技巧的核心就是用最简单的语言把复杂概念解释给外行听。
                        </p>
                        <div className="bg-[#121622] rounded-xl p-2 text-[9px] text-slate-400 leading-normal border border-white/5">
                          适合和 <span className="text-blue-400 font-semibold cursor-pointer">[[📖 阅读方法]]</span> 互相链接。
                        </div>
                        <div className="flex items-center justify-between text-[8px] text-slate-500 pt-1.5 border-t border-white/5">
                          <span>🔗 学习方法卡片</span>
                          <span>5月19日</span>
                        </div>
                      </div>

                      {/* 摘录卡片 2 */}
                      <div className="bg-[#1A1F2C] rounded-2xl p-3 space-y-2.5 border border-white/5 text-left shadow-sm">
                        <div className="flex items-center gap-1.5">
                          <span className="text-[8px] bg-slate-800/80 px-1.5 py-0.5 rounded text-slate-300 font-bold">📖 阅读方法</span>
                          <span className="text-[8px] text-slate-400">#学习方法</span>
                        </div>
                        <p className="text-[10px] text-slate-200 leading-relaxed font-normal">
                          SQ3R阅读法包含纵览、提问、阅读、背诵和复习。
                        </p>
                        <div className="bg-[#121622] rounded-xl p-2 text-[9px] text-slate-400 leading-normal border border-white/5">
                          <span className="text-blue-400 font-semibold cursor-pointer">[[📖 阅读方法]]</span> 里可以引用这一条。
                        </div>
                        <div className="flex items-center justify-between text-[8px] text-slate-500 pt-1.5 border-t border-white/5">
                          <span>🔗 阅读方法实践</span>
                          <span>5月19日</span>
                        </div>
                      </div>

                      {/* 摘录卡片 3 (底部裁切效果) */}
                      <div className="bg-[#1A1F2C] rounded-2xl p-3 space-y-2 border border-white/5 text-left shadow-sm opacity-40">
                        <div className="flex items-center gap-1.5">
                          <span className="text-[8px] bg-red-900/60 px-1.5 py-0.5 rounded text-red-300 font-bold">Inbox</span>
                          <span className="text-[8px] text-slate-400">#方法论 #知识管理</span>
                        </div>
                        <p className="text-[10px] text-slate-200 leading-relaxed line-clamp-1">
                          卡片盒笔记法强调原子化记录，每张卡片只写一个不可分割的概念。
                        </p>
                      </div>
                    </div>
                  )}

                  {activePhoneTab === "tags" && (
                    <div className="space-y-4 animate-phoneFadeIn">
                      {/* AppBar */}
                      <div className="flex items-center justify-between py-1">
                        <span className="text-[14px] font-bold text-white tracking-wide">标签</span>
                        <Search size={14} className="text-slate-300 cursor-pointer" />
                      </div>

                      {/* 标签列表 */}
                      <div className="space-y-4">
                        {/* 组 1 */}
                        <div className="space-y-2 text-left">
                          <div className="flex items-center gap-1.5 text-[11px] font-bold text-slate-300">
                            <Folder size={11} className="text-slate-400 fill-slate-400/20" />
                            <span>Uncategorized (44)</span>
                          </div>
                          <div className="flex flex-wrap gap-1.5 pl-1.5">
                            {["#知识管理 15", "#效率工具 12", "#方法论 10", "#学习方法 7"].map((tag, idx) => (
                              <span key={idx} className="text-[8px] bg-[#1E293B]/60 text-blue-300 border border-blue-500/10 px-2 py-0.5 rounded-full font-bold">
                                {tag}
                              </span>
                            ))}
                          </div>
                        </div>

                        {/* 组 2 */}
                        <div className="space-y-2 text-left">
                          <div className="flex items-center gap-1.5 text-[11px] font-bold text-slate-300">
                            <Folder size={11} className="text-slate-400 fill-slate-400/20" />
                            <span>技术 (25)</span>
                          </div>
                          <div className="flex flex-wrap gap-1.5 pl-1.5">
                            {["#后端 15", "#前端 10"].map((tag, idx) => (
                              <span key={idx} className="text-[8px] bg-[#1E293B]/60 text-blue-300 border border-blue-500/10 px-2 py-0.5 rounded-full font-bold">
                                {tag}
                              </span>
                            ))}
                          </div>
                        </div>

                        {/* 组 3 */}
                        <div className="space-y-2 text-left">
                          <div className="flex items-center gap-1.5 text-[11px] font-bold text-slate-300">
                            <Folder size={11} className="text-slate-400 fill-slate-400/20" />
                            <span>AI (18)</span>
                          </div>
                          <div className="flex flex-wrap gap-1.5 pl-1.5">
                            {["#大模型 18"].map((tag, idx) => (
                              <span key={idx} className="text-[8px] bg-[#1E293B]/60 text-blue-300 border border-blue-500/10 px-2 py-0.5 rounded-full font-bold">
                                {tag}
                              </span>
                            ))}
                          </div>
                        </div>

                        {/* 组 4 */}
                        <div className="space-y-2 text-left">
                          <div className="flex items-center gap-1.5 text-[11px] font-bold text-slate-300">
                            <Folder size={11} className="text-slate-400 fill-slate-400/20" />
                            <span>无分类 (8)</span>
                          </div>
                          <div className="flex flex-wrap gap-1.5 pl-1.5">
                            {["#随想 8"].map((tag, idx) => (
                              <span key={idx} className="text-[8px] bg-[#1E293B]/60 text-blue-300 border border-blue-500/10 px-2 py-0.5 rounded-full font-bold">
                                {tag}
                              </span>
                            ))}
                          </div>
                        </div>

                        {/* 组 5 */}
                        <div className="space-y-2 text-left">
                          <div className="flex items-center gap-1.5 text-[11px] font-bold text-slate-300">
                            <Folder size={11} className="text-slate-400 fill-slate-400/20" />
                            <span>阅读 (5)</span>
                          </div>
                          <div className="flex flex-wrap gap-1.5 pl-1.5">
                            {["#心理学 5"].map((tag, idx) => (
                              <span key={idx} className="text-[8px] bg-[#1E293B]/60 text-blue-300 border border-blue-500/10 px-2 py-0.5 rounded-full font-bold">
                                {tag}
                              </span>
                            ))}
                          </div>
                        </div>
                      </div>
                    </div>
                  )}

                  {activePhoneTab === "settings" && (
                    <div className="space-y-4.5 animate-phoneFadeIn">
                      {/* AppBar */}
                      <div className="flex items-center py-1">
                        <span className="text-[14px] font-bold text-white tracking-wide">设置</span>
                      </div>

                      {/* 个人中心登录卡片 */}
                      <div className="flex flex-col items-center text-center p-3.5 space-y-3 bg-[#1A1F2C] border border-white/5 rounded-2xl shadow-sm">
                        <div className="relative h-11 w-11 rounded-full bg-slate-800/80 flex items-center justify-center border border-white/5">
                          <div className="h-9 w-9 rounded-full bg-slate-700/40 flex items-center justify-center">
                            <User size={18} className="text-slate-400" />
                          </div>
                        </div>
                        <p className="text-[9.5px] text-slate-300 leading-normal px-1 font-medium text-center">
                          登录解锁完整体验，开启云端同步，实现多设备数据自动同步
                        </p>
                        <button className="w-full py-1.5 rounded-full bg-[#8AB4F8] text-slate-900 text-[10px] font-bold shadow-md shadow-blue-500/10 transition-all hover:brightness-105 active:scale-[0.98]">
                          登录 / 注册
                        </button>
                      </div>

                      {/* 外观设置 */}
                      <div className="text-left space-y-1.5">
                        <span className="text-[10px] font-bold text-slate-400 block px-1 tracking-wider">外观</span>
                        <div className="bg-[#1A1F2C] rounded-2xl p-1.5 grid grid-cols-3 gap-1 border border-white/5">
                          <div className="flex flex-col items-center gap-1 py-1.5 rounded-xl cursor-pointer hover:bg-white/5 text-[8px] text-slate-400 transition-all">
                            <Sun size={11} />
                            <span>浅色</span>
                          </div>
                          <div className="flex flex-col items-center gap-1 py-1.5 rounded-xl bg-blue-500/20 border border-blue-500/20 text-[8px] text-blue-200 transition-all font-bold">
                            <Moon size={11} />
                            <span>深色</span>
                          </div>
                          <div className="flex flex-col items-center gap-1 py-1.5 rounded-xl cursor-pointer hover:bg-white/5 text-[8px] text-slate-400 transition-all">
                            <Paintbrush size={11} />
                            <span>跟随系统</span>
                          </div>
                        </div>
                      </div>

                      {/* 主题颜色设置 */}
                      <div className="text-left space-y-1.5">
                        <span className="text-[10px] font-bold text-slate-400 block px-1 tracking-wider">主题颜色</span>
                        <div className="bg-[#1A1F2C] rounded-2xl p-2.5 border border-white/5 shadow-sm">
                          <div className="grid grid-cols-4 gap-2 text-center">
                            {/* 动态 */}
                            <div className="flex flex-col items-center gap-1 cursor-pointer">
                              <div className="h-6.5 w-6.5 flex items-center justify-center relative scale-[1.05]">
                                <svg viewBox="0 0 24 24" className="h-full w-full text-[#8AB4F8] fill-current">
                                  <path d="M12 2a1.5 1.5 0 0 1 1.06.44l1.06 1.06 1.48-.25a1.5 1.5 0 0 1 1.72 1.25l.25 1.48 1.06 1.06a1.5 1.5 0 0 1 0 2.12l-1.06 1.06-.25 1.48a1.5 1.5 0 0 1-1.25 1.72l-1.48.25-1.06 1.06a1.5 1.5 0 0 1-2.12 0l-1.06-1.06-1.48-.25a1.5 1.5 0 0 1-1.72-1.25l-.25-1.48-1.06-1.06a1.5 1.5 0 0 1 0-2.12l1.06-1.06.25-1.48a1.5 1.5 0 0 1 1.25-1.72l1.48-.25 1.06-1.06A1.5 1.5 0 0 1 12 2z" />
                                </svg>
                                <span className="absolute text-[8px] font-bold text-slate-900">✓</span>
                              </div>
                              <span className="text-[8px] text-[#8AB4F8] font-bold mt-0.5">动态</span>
                            </div>
                            {/* 海洋 */}
                            <div className="flex flex-col items-center gap-1 cursor-pointer group">
                              <div className="h-6.5 w-6.5 rounded-full bg-[#1E5D7A] border border-white/5 transition-transform group-hover:scale-105" />
                              <span className="text-[8px] text-slate-400 mt-0.5">海洋</span>
                            </div>
                            {/* 紫色 */}
                            <div className="flex flex-col items-center gap-1 cursor-pointer group">
                              <div className="h-6.5 w-6.5 rounded-full bg-[#7D52B3] border border-white/5 transition-transform group-hover:scale-105" />
                              <span className="text-[8px] text-slate-400 mt-0.5">紫色</span>
                            </div>
                            {/* 森林 */}
                            <div className="flex flex-col items-center gap-1 cursor-pointer group">
                              <div className="h-6.5 w-6.5 rounded-full bg-[#3B7A1E] border border-white/5 transition-transform group-hover:scale-105" />
                              <span className="text-[8px] text-slate-400 mt-0.5">森林</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                </div>

                {/* 悬浮 FAB 按钮 (根据 Tab 状态渲染，在设置页隐藏) */}
                {activePhoneTab !== "settings" && (
                  <button className="absolute bottom-[58px] right-4 h-9.5 w-9.5 rounded-2xl bg-[#1E4E79] text-white flex items-center justify-center shadow-lg shadow-black/40 border border-white/5 cursor-pointer transition-transform hover:scale-105 active:scale-95 z-20 animate-phoneFadeIn">
                    <Plus size={16} strokeWidth={2.5} />
                  </button>
                )}

                {/* 底部 M3 NavigationBar (Dock 栏) */}
                <div className="absolute bottom-0 inset-x-0 h-[52px] bg-[#0F121A] border-t border-white/5 flex items-center justify-around px-1 pb-1.5 z-20">
                  {/* 摘录流 */}
                  <div className="flex flex-col items-center cursor-pointer group select-none flex-1" onClick={() => setActivePhoneTab("stream")}>
                    <div className={`px-4.5 py-0.5 rounded-full flex items-center justify-center transition-all ${activePhoneTab === "stream" ? "bg-[#2C3B4E] text-[#D2E4F9]" : "text-[#A0AAB8] group-hover:text-slate-200"}`}>
                      <HomeIcon size={12} strokeWidth={2} />
                    </div>
                    <span className={`text-[7.5px] mt-0.5 font-bold tracking-wider ${activePhoneTab === "stream" ? "text-white" : "text-[#A0AAB8]"}`}>摘录流</span>
                  </div>

                  {/* 知识树 */}
                  <div className="flex flex-col items-center cursor-pointer group select-none flex-1" onClick={() => setActivePhoneTab("tree")}>
                    <div className={`px-4.5 py-0.5 rounded-full flex items-center justify-center transition-all ${activePhoneTab === "tree" ? "bg-[#2C3B4E] text-[#D2E4F9]" : "text-[#A0AAB8] group-hover:text-slate-200"}`}>
                      <GitBranch size={12} strokeWidth={2} />
                    </div>
                    <span className={`text-[7.5px] mt-0.5 font-bold tracking-wider ${activePhoneTab === "tree" ? "text-white" : "text-[#A0AAB8]"}`}>知识树</span>
                  </div>

                  {/* 标签 */}
                  <div className="flex flex-col items-center cursor-pointer group select-none flex-1" onClick={() => setActivePhoneTab("tags")}>
                    <div className={`px-4.5 py-0.5 rounded-full flex items-center justify-center transition-all ${activePhoneTab === "tags" ? "bg-[#2C3B4E] text-[#D2E4F9]" : "text-[#A0AAB8] group-hover:text-slate-200"}`}>
                      <TagIcon size={12} strokeWidth={2} />
                    </div>
                    <span className={`text-[7.5px] mt-0.5 font-bold tracking-wider ${activePhoneTab === "tags" ? "text-white" : "text-[#A0AAB8]"}`}>标签</span>
                  </div>

                  {/* 设置 */}
                  <div className="flex flex-col items-center cursor-pointer group select-none flex-1" onClick={() => setActivePhoneTab("settings")}>
                    <div className={`px-4.5 py-0.5 rounded-full flex items-center justify-center transition-all ${activePhoneTab === "settings" ? "bg-[#2C3B4E] text-[#D2E4F9]" : "text-[#A0AAB8] group-hover:text-slate-200"}`}>
                      <SettingsIcon size={12} strokeWidth={2} />
                    </div>
                    <span className={`text-[7.5px] mt-0.5 font-bold tracking-wider ${activePhoneTab === "settings" ? "text-white" : "text-[#A0AAB8]"}`}>设置</span>
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
                  href={apkDownloadUrl}
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
              <div className="relative p-2.5 bg-white rounded-2xl shadow-xl shadow-black/40 flex items-center justify-center">
                {/* 动态生成真正的二维码 */}
                <img
                  src={`https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=${encodeURIComponent(absoluteApkUrl)}`}
                  alt="GleanRead APK QR Code"
                  className="h-32 w-32"
                  crossOrigin="anonymous"
                />
                <img
                  src="/icons/icon-192.png"
                  alt="G"
                  className="absolute inset-0 m-auto h-8 w-8 rounded-lg border-2 border-white bg-white shadow object-contain"
                />
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
