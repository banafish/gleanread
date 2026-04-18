## 上下文

当前知识树 UI 只有 [TreeScreen.kt](/D:/code/android/read/glean-read-android/app/src/main/java/com/gleanread/android/ui/workspace/screens/TreeScreen.kt:1) 一个递归展开页面：顶部只有“新增根节点”，节点只能展开或直接进入详情，缺少设计稿要求的首页卡片预览、面包屑局部分支页、搜索入口、统一更多菜单和空状态。

数据层目前也只提供 `createRootNode()` 与基于 `TreeNodeUiModel` 的整棵树快照构建：

- `WorkspaceRepository` 只支持新增根节点、AI 挂载创建新节点、更新节点大纲，不支持子节点创建、重命名、删除子树或面向分支页的专门转换。
- `WorkspaceDao` 只有 `findNodeById()`、`observeNodes()` 等基础查询，没有按节点递归查找子树、批量软删除、重命名更新等专用接口。
- `WorkspaceSnapshot.treeRoots` 只适合“把整棵树一次性递归渲染”，不适合首页显示 1~3 层、分支页显示“当前节点后三层”的局部预览。

这次改版仍要遵守仓库约束：

- Android UI 使用 Material Design 3 / Material You。
- 知识树 Tab 仍由 `WorkspaceScreen.kt` 承载底部导航和 NavHost，不能破坏现有工作台结构。
- 原有点击节点进入 `node/{nodeId}` 的逻辑必须保留；只有在节点仍有更深层需要浏览时，才改为进入知识树分支页。

## 目标 / 非目标

**目标：**

- 将知识树改造成“首页浅层预览 + 分支页局部子树”的双层浏览结构，并与 `知识树页面设计260418.md` 的层级与交互规则对齐。
- 把知识树实现拆分为独立 `feature/knowledge_tree/` 包，降低 `TreeScreen.kt` 单文件承担的状态、导航和 UI 复杂度。
- 引入统一的知识树 UI 状态模型和 UI action/event，使首页、分支页、Dialog、菜单和搜索内容可以共享一套交互语义。
- 在 Repository / Dao 层补齐节点新增、重命名、删除子树和局部浏览需要的能力，并保持 Room 本地优先与 `sync_status` 更新规则一致。
- 保留现有节点详情导航和工作台返回栈体验，使新知识树浏览只是在“需要继续下钻”时增加分支路由，而不是替换节点详情页。

**非目标：**

- 不改造节点详情页、局部图谱页、摘录流页或标签页的布局与交互。
- 不引入新的后端 API，也不改变 Room 的基础表结构。
- 不在本次设计中实现真正的全文搜索索引；搜索页先基于本地已加载的节点/摘录数据做前端过滤与展示。
- 不扩展超过设计稿要求的复杂手势、拖拽排序或跨层级批量操作。

## 决策

### 1. 采用 `WorkspaceScreen` 托管导航 + `feature/knowledge_tree` 独立实现

知识树仍由 `WorkspaceScreen.kt` 中的工作台 `NavHost` 托管，但具体页面和组件迁移到 `com.gleanread.android.feature.knowledge_tree` 包下：

- `KnowledgeTreeHomeRoute.kt` / `KnowledgeTreeHomeScreen.kt`
- `KnowledgeTreeBranchRoute.kt` / `KnowledgeTreeBranchScreen.kt`
- `component/`
- `model/`

这样可以保持底部导航和现有工作台路由不变，同时把知识树首页、分支页和子组件从 `ui/workspace/screens/TreeScreen.kt` 拆出。

备选方案：

- 继续在 `TreeScreen.kt` 内扩展所有逻辑。被放弃，因为会继续放大单文件状态耦合，后续 Dialog、菜单、搜索和分支导航难以维护。
- 把知识树做成独立 Activity。被放弃，因为会破坏现有工作台底部导航与返回栈模型。

### 2. 新增知识树专用路由，但保留节点详情路由

新增两类路由：

- `WorkspaceRoutes.Tree` 仍作为知识树首页入口。
- 新增知识树分支路由，例如 `tree/branch/{nodeId}`，用于“继续浏览更深层节点”。

路由判定规则：

- 首页一级节点卡片主体点击进入对应分支页。
- 二级或三级节点若“没有更深一层需要继续浏览”，沿用现有 `node/{nodeId}` 详情跳转。
- 二级或三级节点若下一级仍存在子树且已超过当前页面的可见预览深度，则进入 `tree/branch/{nodeId}`。

备选方案：

- 所有节点一律进入详情页。被放弃，因为无法满足设计稿的“分段浏览局部子树”。
- 所有节点一律进入分支页。被放弃，因为会破坏现有详情页访问语义，也违背用户要求保留节点点击详情逻辑。

### 3. 用独立 UI Model 表达“首页视图”和“分支视图”，而不是复用 `TreeNodeUiModel`

现有 `TreeNodeUiModel(id, title, count, children)` 适合递归渲染，但不携带以下关键信息：

- 当前节点在首页还是分支页展示
- 本行点击应进入详情还是进入分支页
- 是否展示 `进入 >`
- 面包屑路径
- Dialog 目标节点和删除提示信息

因此新增设计稿建议的模型：

- `KnowledgeTreeHomeUiState`
- `KnowledgeTreeBranchUiState`
- `RootNodeCardUiModel`
- `PreviewNodeUiModel`
- `BranchNodeUiModel`
- `NodeDialogUiState`
- `DeleteDialogUiState`
- `NodeActionTarget`
- `NodeDestination`
- `KnowledgeTreeUiAction`
- `KnowledgeTreeUiEvent`

其中 `NodeDestination` 用来统一表达“进入详情 / 进入分支 / 无跳转”，页面层只消费 destination，不再在 Composable 中直接推断层级规则。

备选方案：

- 直接在 Composable 中根据 `children.isNotEmpty()` 和 level 动态判断行为。被放弃，因为首页与分支页的“可见深度规则”不同，推断逻辑容易分散且重复。

### 4. Repository 提供“平铺节点 + 派生局部子树”的转换接口，避免修改 Room 表结构

这次不改数据库 schema，而是在 `WorkspaceRepository` 基于现有 `KnowledgeTreeNodeEntity.parentId` 派生新的知识树 UI 数据：

- 继续保留 `WorkspaceSnapshot.flatNodes` 与 `treeRoots`，兼容现有页面。
- 新增面向知识树 feature 的转换函数，输入节点 id 和预览深度，输出首页卡片模型或分支页模型。
- 面包屑路径直接通过 `parentId` 向上追溯构造，无需新增表字段。

这样可以把改动控制在 Repository 转换层和 UI 模型层，避免为了展示规则调整 Room schema。

备选方案：

- 在 `WorkspaceSnapshot` 中直接塞入所有首页/分支页专用状态。被放弃，因为会让通用快照模型掺杂大量页面级临时状态。
- 为面包屑路径和层级预览新增数据库冗余字段。被放弃，因为展示结构可从现有父子关系推导，没有必要做持久化冗余。

### 5. 节点编辑能力统一下沉到 Repository + Dao，并采用软删除子树

为支持设计稿中的统一节点菜单，需要新增以下能力：

- `createChildNode(parentId, title)`
- `renameNode(nodeId, title)`
- `deleteNodeSubtree(nodeId)`

删除采用软删除：

- 递归找出目标节点及所有后代节点，将其 `is_deleted = true`
- 对已挂载到这些节点的摘录，将 `tree_node_id` 清空并更新 `sync_status`
- 节点自身和受影响摘录统一 bump `sync_status`

这样能与现有 Room 本地优先同步模型保持一致，也能满足“删除子树后整棵树都移除”的产品语义。

备选方案：

- 物理删除节点。被放弃，因为与现有表的 `is_deleted` / `sync_status` 设计不一致，也不利于后续同步。
- 删除节点但保留后代节点挂到父级。被放弃，因为不符合设计稿中“删除后整棵子树都会被移除”的说明。

### 6. 搜索页先作为知识树内部内容面板实现，不新增全局搜索架构

设计稿包含搜索页，但当前工作台没有统一搜索体系。本次先实现 `KnowledgeTreeSearchContent`：

- 以顶部搜索入口触发
- 基于本地 `flatNodes`、`excerpts` 做即时过滤
- 展示“最近搜索、节点结果、内容结果”
- 点击节点结果根据 `NodeDestination` 进入详情或分支；点击内容结果进入关联节点详情

这样可以在不引入新索引层的情况下完成设计稿交互，并为后续升级搜索能力留出接口。

备选方案：

- 暂不实现搜索。被放弃，因为设计稿已将其列为主界面结构的一部分。
- 直接引入 FTS / 新表。被放弃，因为超出本次 UI 改版范围。

## 风险 / 权衡

- [局部预览规则比现有整树递归复杂] → 通过 `NodeDestination` 与专用 UI model 集中封装层级判断，避免把“首页看三层、分支页看三层”散落到多个 Composable。
- [新增 feature 包后，现有 workspace 包与知识树包之间可能出现双向依赖] → 让知识树 feature 只依赖 `WorkspaceViewModel` 暴露的数据和回调，不反向引用 workspace screen 组件。
- [删除子树会影响已归档摘录，若处理不全会产生脏数据] → 在 Repository 中使用事务统一更新节点和摘录，并在 design 对应实现中增加删除确认文案与测试覆盖。
- [搜索先用前端过滤，在节点和摘录数量变大时性能一般] → 首版限制结果数量并复用内存快照；若后续数据规模增长，再单独提案引入索引层。
- [首页卡片和分支页都支持展开，状态恢复可能复杂] → 用 `rememberSaveable` + `UiState` 中的 expanded id 集合按页面分别存储，避免跨页面串状态。
- [目录迁移会影响现有 import 和导航] → 先保持 `WorkspaceRoutes` 作为单一导航入口，只替换其知识树目标 Composable，降低迁移冲击。

## Migration Plan

1. 新增 `feature/knowledge_tree/` 目录与基础 model/component/screen/route 文件，先让新首页可以被 `WorkspaceRoutes.Tree` 渲染。
2. 在 `WorkspaceScreen.kt` 中新增分支路由，并把知识树首页、分支页都接入现有 NavHost。
3. 在 `WorkspaceRepository` / `WorkspaceDao` / `WorkspaceViewModel` 中补齐子节点新增、重命名、删除子树和知识树 UI 转换能力。
4. 用新 feature 替换旧 `TreeScreen.kt` 的入口实现，验证首页、分支页、节点详情跳转和返回栈。
5. 补齐搜索内容、Dialog、空状态和节点菜单，再删除或停用旧的 `TreeScreen.kt` 递归实现。

回滚策略：

- 保留 `TreeScreen.kt` 直到新路由完全可用；如新实现出现阻塞问题，可暂时把 `WorkspaceRoutes.Tree` 切回旧入口。

## Open Questions

- 删除有摘录挂载的节点后，摘录是统一回到 Inbox 还是保留“未归档但可从最近记录找到”的当前表现？设计文档只明确了删除子树，对摘录归属未显式说明；本设计暂定为回到 Inbox。
- 搜索页的“最近搜索”是否需要真实持久化到 Room / DataStore，还是本地内存会话级即可？本次设计默认先做会话级。
- 首页顶部 `⋮` 中的“展开全部 / 收起全部”是否要覆盖所有根卡片，还是仅作用于当前可见层？设计稿给了入口但未细化行为，本次实现建议作用于首页所有根卡片和分支页当前列表。
