## 为什么

GleanRead 需要一个独立的 Web 端工作台，把“摘录收件箱 -> 横向知识树 -> 右侧沉淀抽屉”这套知识整理流程落成可执行的产品能力。当前设计文档已经明确了路由结构、三栏布局、键盘优先建树、拖拽挂载、标签与搜索、本地优先存储和云端同步，但还没有对应的 OpenSpec 变更来约束实现。

这次变更的目标是把 Web 端主工作台从概念设计变成可分解、可实现、可验证的规范，并且与上一级 Android 仓库的 OpenSpec 目录彻底分开，避免混淆。

## 变更内容

1. 新增 Web 端路由结构：
   - `/` 作为 Landing Page
   - `/login` 作为独立登录页
   - `/auth/callback` 作为认证回调页
   - `/app` 作为受保护的主工作台
2. 建立三栏式主工作台：
   - 左侧 Inbox 收件箱，用于展示未挂载摘录
   - 中间 Canvas，用于展示横向知识树和节点关系
   - 右侧 Detail Drawer，用于 Markdown 沉淀和挂载摘录浏览
3. 实现知识树交互：
   - 节点横向展开、折叠、选中和高亮
   - `Tab`、`Enter`、方向键、`Space` 等键盘快捷操作
   - `sort_order` 稀疏排序与本地展开状态记忆
   - MiniMap、缩放、适应屏幕等视图控制
4. 实现摘录工作流：
   - Inbox 卡片展示内容、来源、思考和标签
   - 拖拽摘录到节点后更新 `tree_node_id`
   - 节点摘录计数同步更新
5. 实现右侧抽屉沉淀能力：
   - 绑定节点 `outline_markdown`
   - 全屏专注写作模式
   - 展示该节点下的摘录流
   - 支持 `user_thought` 快速编辑、标签补全和移回未分类
6. 实现全局能力：
   - 标签热度推荐与搜索
   - 软删除垃圾篓
   - 浅色/深色主题切换
   - 本地优先存储与增量同步

## 功能 (Capabilities)

### 新增功能
- `web-knowledge-workbench`: Web 端主工作台整体能力，包含三栏布局、导航、抽屉和页面级状态。
- `web-knowledge-tree-graph`: 中间 Canvas 知识树图谱能力，包含节点布局、键盘操作、缩放和平移。
- `web-inbox-excerpts`: 左侧收件箱摘录列表和拖拽挂载能力。
- `web-detail-drawer`: 右侧详情沉淀抽屉能力，包含 Markdown 编辑、摘录流和标签交互。
- `web-local-first-sync`: Zustand + Dexie + Supabase 的本地优先数据流与同步能力。
- `web-auth-shell`: 登录、回调和受保护路由的 Web 认证壳。

## 影响

- 需要新增 Web 路由、页面结构和全局状态管理。
- 需要定义前端本地数据库结构，并与 `knowledge_tree_node`、`excerpts`、`tags`、`excerpt_tags` 对齐。
- 需要把展开状态、排序字段、软删除和同步字段纳入前端行为规范。
- 需要后续继续拆分设计和任务，才能进入具体实现。
