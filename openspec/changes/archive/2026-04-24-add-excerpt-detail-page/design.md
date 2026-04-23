## 上下文

当前 Android 工作台里，单条摘录的打开方式并不统一：
- 摘录流卡片点击仍以列表浏览为主，没有独立详情页。
- 节点详情页、Backlinks、局部图谱和展示态内联摘录链接通过 `onPreviewExcerpt` 汇入 `MainApp`，最终打开 `ExcerptPreviewDialog` 这类轻量预览弹窗。
- 现有弹窗适合“临时看一眼”，但不适合承接沉浸阅读、页面内编辑、来源跳转和明确的返回栈。
- 现有工作台也缺少“编辑已有摘录标签和挂载位置”的单页入口；标签关系目前只有新增时写入逻辑，`ExcerptTagDao` 也还没有更新关系的接口。

从代码结构看，当前工作台已经有较稳定的模式：
- `MainApp.kt` 负责集中声明导航路由和跨页面回调。
- `MainAppViewModel` 负责工作台级本地写操作，如删除摘录、移动摘录、更新节点大纲等。
- `NodeDetailRoute` 采用“`snapshot` + Route 本地暂存编辑草稿 + 通过上层回调保存”的模式，而不是为每个详情页都引入新的 `ViewModel`。
- Room 本地库已经承载摘录的 `content / userThought / sourceTitle / url / treeNodeId / syncStatus`，但目前缺少“更新已有摘录正文与来源信息”的仓储入口。

这次变更同时触及导航、详情页 UI、展示态链接跳转和本地保存链路，属于跨多个现有 feature 的一致性改造，因此在实现前需要先确定统一方案。

## 目标 / 非目标

**目标：**
- 为摘录新增独立详情页，默认提供阅读态，并在同页切换到编辑态。
- 允许用户在摘录详情页编辑标签和挂载位置，并通过选择器完成这些元信息调整。
- 统一所有“打开单条摘录”的工作台入口，使其都进入摘录详情页，而不是继续分裂为卡片点击、图谱点击、预览弹窗等多种形态。
- 复用当前本地优先架构：详情页读取 `WorkspaceSnapshot`，保存只写 Room 本地表并正确维护 `syncStatus`。
- 保持与现有模块风格一致，优先沿用 `Route + Screen + 上层 ViewModel/Repository 回调` 的现有模式，避免为这次功能引入过度的新抽象。
- 在 UI 上遵循 Material 3，使用更适合阅读页的 `Scaffold + LargeTopAppBar/TopAppBar + LazyColumn` 结构，而不是继续使用弹窗式预览。

**非目标：**
- 不开放创建时间编辑。
- 不重做摘录流卡片布局、多选逻辑或 AI 提炼流程。
- 不扩展数据库表结构，也不引入新的远程接口。
- 不把所有现有详情页都重构成相同骨架；本次只为摘录详情页建立新结构。
- 不在这次变更中强行把正文编辑也升级为完整的 `[[` 联想输入能力；只需保证现有展示态链接解析和想法区联想能力可延续。
- 不在这次变更中引入“新建标签”能力；标签编辑先以选择已有标签为边界。

## 决策

### 决策 1：新增独立导航路由，而不是继续复用预览弹窗

实现方式：
- 在 `MainRoutes` 中新增 `ExcerptPattern = "excerpt/{excerptId}"` 和 `excerpt(excerptId)`。
- 在 `MainApp` 的 `NavHost` 中新增摘录详情路由，统一由该路由承接单条摘录打开行为。
- 现有 `previewExcerptId` 和 `ExcerptPreviewDialog` 不再作为主路径；当所有入口都切换完成后，可在实现阶段一起移除。

选择原因：
- 独立路由天然支持返回栈，能正确回到摘录流、节点详情页或图谱页，不需要再靠弹窗态额外记住来源。
- 详情页需要阅读态与编辑态切换、滚动收起顶栏、来源跳转等行为，弹窗交互空间不足，且不符合用户给出的页面层级设计。
- 统一成路由后，`Feed -> ExcerptDetail`、`NodeDetail -> ExcerptDetail`、`Graph -> ExcerptDetail` 和 `InlineLink -> ExcerptDetail` 的语义完全一致，后续测试也更直接。

备选方案：
- 保留 `ExcerptPreviewDialog`，只在弹窗中新增“进入详情页”按钮。
  - 放弃原因：仍然保留了双路径，用户对“哪些入口是预览、哪些入口是详情”会继续感知不一致。
- 继续用底部抽屉或全屏 Dialog 实现详情。
  - 放弃原因：返回栈和沉浸式阅读体验都不如标准导航页清晰。

### 决策 2：新页面采用 `feature/excerpts/detail`，并沿用 Route 持有草稿、Screen 负责纯 UI 的模式

实现方式：
- 新增 `feature/excerpts/detail/ExcerptDetailRoute.kt` 与 `ExcerptDetailScreen.kt`。
- `ExcerptDetailRoute` 通过 `snapshot.excerptsById[excerptId]` 读取当前摘录，并持有以下短期 UI 状态：
  - `isEditing`
  - `draftContent`
  - `draftThought`
  - `draftSourceTitle`
  - `draftUrl`
  - `draftSelectedTags`
  - `draftArchiveNodeId`
  - `isTagPickerVisible`
  - `isMountPickerVisible`
- `ExcerptDetailScreen` 只接收展示数据与回调，负责渲染阅读态/编辑态、顶栏和内容区。
- 若文件长度在实现中明显超出当前模块可读范围，再补拆 `ExcerptDetailSections.kt`；不提前过度拆分。

选择原因：
- 这与当前 `NodeDetailRoute` 的做法一致，能保持模块内风格连续，不需要仅为一个页面再复制一套 ViewModel 和 StateFlow 模板。
- 编辑草稿属于短生命周期、页面私有的 UI 状态，使用 `rememberSaveable(excerptId)` 足以覆盖旋转/重建场景，也更贴合 Compose 文档里的“局部瞬时状态就近管理”原则。
- 真正的业务写操作仍然通过上层回调进入 `MainAppViewModel` / Repository，不会把 Repository 直接拉进 UI 层。

备选方案：
- 为摘录详情页新增专用 `ExcerptDetailViewModel`。
  - 放弃原因：当前页面没有后台生成、复杂异步编排或跨页面共享状态，专用 ViewModel 会增加注入和同步成本。
- 直接在 `Screen` 中访问 Repository。
  - 放弃原因：与现有 Compose 架构规范冲突，也会让 UI 难以测试。

### 决策 3：保存链路落在 `SnapshotRepository.updateExcerpt(...)`，由 `MainAppViewModel` 对外暴露

实现方式：
- 在 `SnapshotRepository` 新增单条摘录更新方法，输入至少包括：
  - `excerptId`
  - `content`
  - `thought`
  - `sourceTitle`
  - `url`
  - `tagNames`
  - `archiveNodeId`
- 该方法读取现有 `ExcerptEntity`，仅更新可编辑字段、`updateTime` 和 `syncStatus`：
  - `content` 必须 `trim()` 后保存，空正文禁止保存。
  - `thought / sourceTitle / url` 在为空白时回写为 `null`。
  - `archiveNodeId` 允许被更新为其他知识树节点或 `null`（回到 Inbox）。
  - `createTime` 保持不变。
  - `syncStatus` 使用现有 `SyncStatus.bump(existing.syncStatus)`。
- 该方法还需要在同一事务内对标签关系做对账：
  - 读取当前摘录已有的 `excerpt_tags` 关系。
  - 为新增标签补建关系；若标签实体不存在，则不在本次变更内创建新标签，而是以已有标签库为可选范围。
  - 对被移除的标签关系做本地删除或等价的待同步删除标记。
  - 因此需要为 `ExcerptTagDao` 增加更新关系的能力，而不是继续只有插入接口。
- `MainAppViewModel` 新增 `updateExcerpt(...)` 并供详情页 Route 调用。

选择原因：
- 这次编辑只涉及摘录单表字段，不涉及标签关系、节点层级或 AI 生成，放在已经承接工作台写操作的 `SnapshotRepository` 最小且清晰。
- `ExcerptCaptureRepository` 当前语义偏“创建新摘录”，把编辑旧摘录塞进去会混淆职责。
- 不新增表、不做 schema migration，风险更低。

备选方案：
- 在 `ExcerptCaptureRepository` 增加更新方法。
  - 放弃原因：会把“创建”与“编辑已有记录”混在一起，不利于后续维护。
- 新建 `ExcerptDetailRepository`。
  - 放弃原因：对于这次仅有单表更新的场景，抽象层级过重。

### 决策 4：所有单摘录入口统一改名为“打开详情”，不再保留“预览”语义

实现方式：
- 把跨页面回调逐步从 `onPreviewExcerpt` 收敛为 `onOpenExcerpt` 或等价命名。
- 以下入口统一导航到 `MainRoutes.excerpt(excerptId)`：
  - 摘录流卡片点击
  - 节点详情页中的挂载摘录点击
  - 节点详情页 Backlinks 中的摘录点击
  - `LinkAwareText` 解析出的摘录链接点击
  - 局部关系图中的摘录节点点击

选择原因：
- 入口虽然分散在 `FeedRoute`、`NodeDetailRoute`、`GraphRoute` 和 `LinkAwareText` 回调链上，但最终行为只有一种时，命名也应该只有一种。
- 这能减少“某处还是弹窗，某处已经是详情页”的行为残留，便于后续排查和测试。

备选方案：
- 继续保留 `onPreviewExcerpt` 命名，只是在实现里跳详情页。
  - 放弃原因：代码语义会持续误导维护者，后续容易再被误接回 Dialog。

### 决策 5：详情页使用 `Scaffold` 托管顶栏和滚动行为，正文/想法/来源输入采用统一的低强调无边界风格

实现方式：
- 页面使用 `Scaffold(topBar = ...)` + `LazyColumn`，顶栏优先采用 `LargeTopAppBar`，并接入 `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`；如果实现中发现大标题占用过多空间，再退回普通 `TopAppBar`，但仍保留 Scaffold-owned top bar。
- 阅读态内容顺序固定为：
  - 元信息区
  - 摘录内容
  - 我的想法
  - 来源
- 元信息区在编辑态仍保留同一位置，但标签与挂载位置通过显式入口打开选择器，而不是展开成多个普通输入框。
- 阅读态正文与想法继续使用 `LinkAwareText`，从而保持展示态内联链接可点击。
- 编辑态输入风格统一为无明显描边、透明 indicator 的 `TextField` 族：
  - 正文：页面私有的多行 plain `TextField`
  - 想法：优先扩展 `InlineLinkEditor` 的容器样式参数或新增页面私有包装，以保留 `[[` 联想能力，同时视觉上与正文一致
  - 来源标题 / 来源链接：页面私有的单行 plain `TextField`
- 三类输入只在容器层次上区分强弱：
  - 正文：`surfaceContainerLow`
  - 想法：更轻的容器色
  - 来源：接近透明或最弱容器

选择原因：
- 用户已经明确要求详情页承担“阅读和思考”，因此页面结构必须比 feed 卡片更沉浸，顶栏与滚动关系也应该符合 MD3 阅读页预期。
- `Scaffold` 托管顶栏比把 TopAppBar 当成首个列表项更容易处理滚动收起、WindowInsets 和后续页面扩展。
- 想法区继续复用 `InlineLinkEditor` 可以保住现有联想能力；同时通过页面私有样式包装，避免为了一个页面去重做全局输入组件默认外观。
- 让标签和挂载位置继续留在元信息区，可以保持“正文最强、元信息次要”的阅读节奏，同时满足用户要求的轻量编辑方式。

备选方案：
- 沿用 `NodeDetailScreen` 当前“TopBar 作为 LazyColumn 第一项”的模式。
  - 放弃原因：不利于实现用户要求的沉浸式滚动收起顶栏，也更难保持 MD3 标准页结构。
- 所有输入都直接复用当前 `InlineLinkEditor`。
  - 放弃原因：正文和来源字段并不都需要联想面板，会引入无必要复杂度。

### 决策 6：挂载编辑复用现有知识树节点选择器，标签编辑使用页面私有多选选择器

实现方式：
- 挂载编辑直接复用 `KnowledgeTreeNodePickerBottomSheet` 这条现有能力链，让用户在详情页中把摘录改挂到其他节点或改回 Inbox。
- 挂载编辑这次不额外开放“创建新节点”能力，先聚焦“切换已有归档位置”。
- 标签编辑新增页面私有的多选标签选择器，视觉上可借用 `TagPickerChip` / `TagPill` 的既有样式语言，但不强行复用 fast capture 的整套弹出菜单结构。
- 标签选择器只展示本地已有标签，并允许搜索、勾选和取消勾选。

选择原因：
- 节点选择器已经在 AI summary 挂载流程中验证过交互和层级浏览，复用它能减少本次 mount editing 的设计和实现风险。
- 现有 quick capture 标签 UI更偏“推荐标签快速点选”，而详情页需要的是“查看当前已选 + 搜索全量标签 + 多选编辑”的稳定页面内工具，因此更适合做页面私有选择器。

备选方案：
- 继续用 `ArchivePickerDialog` 递归文本按钮列表承接挂载编辑。
  - 放弃原因：与当前知识树节点选择器的交互质量不一致，也不利于后续统一入口。
- 直接复用 fast capture 的标签全屏菜单。
  - 放弃原因：该菜单围绕悬浮快摘布局设计，直接迁入详情页会带入不必要的浮层假设和动画语义。

### 决策 7：编辑草稿只在非编辑态时与实时快照重新同步

实现方式：
- `ExcerptDetailRoute` 进入页面时用当前摘录初始化草稿。
- 只在以下时机把最新快照重新回填到草稿：
  - `excerptId` 改变
  - 当前不处于编辑态且快照中的摘录内容发生变化
- 处于编辑态时，即使 `WorkspaceSnapshot` 因其他操作刷新，也不自动覆盖本地草稿。
- 保存成功后退出编辑态，此时再允许用最新快照重新对齐。

选择原因：
- 这和当前 `NodeDetailRoute` 对本地 outline 草稿的处理原则一致，能避免用户编辑到一半时被外部快照覆盖。

备选方案：
- 无论是否正在编辑，都始终以最新快照覆盖草稿。
  - 放弃原因：会直接造成输入内容丢失。

## 风险 / 权衡

- [风险] 入口改造面比较广，容易遗漏某个 `onPreviewExcerpt` 调用点 → 缓解措施：实现阶段先全局检索 `onPreviewExcerpt` / `previewExcerptId` / `ExcerptPreviewDialog`，确保所有单摘录入口都改为同一导航回调。
- [风险] 继续使用 Route 本地草稿，而不是专用 ViewModel，随着标签编辑和挂载编辑加入后页面状态会变重 → 缓解措施：本次仍保持最小实现；若后续继续加入自动生成或跨页面共享编辑状态，再单独把详情页状态提升为 ViewModel。
- [风险] 标签关系更新需要新增 `ExcerptTagDao` 更新能力，若软删除策略处理不完整会导致本地快照和同步状态不一致 → 缓解措施：在同一事务内统一处理新增关系、恢复关系和移除关系，并优先复用现有 `SyncStatus` 语义。
- [风险] 边界更弱的输入框会降低“可编辑”感知 → 缓解措施：通过分区标题、轻微容器色变化、聚焦态颜色变化和保存按钮的 dirty 状态来补足反馈，而不是回退到重表单外观。
- [风险] 来源链接可能包含用户输入的无效值，阅读态直接点击会失败 → 缓解措施：保存时仅做 trim/null 归一化，不做激进改写；阅读态点击前做最小可打开性判断，不可打开时保持只读展示。
- [权衡] 这次直接让图谱/内联链接统一跳详情页，会牺牲“快速看一眼就关掉”的轻量路径 → 缓解措施：详情页首屏尽量简洁，返回动作保持单步返回，降低打开详情页的心智成本。

## 迁移计划

1. 在 OpenSpec 层完成本次 proposal / specs / design / tasks，锁定统一导航语义。
2. 新增摘录详情页路由与 `feature/excerpts/detail` 页面实现，先打通只读阅读态。
3. 在 `SnapshotRepository`、`ExcerptTagDao` 与 `MainAppViewModel` 增加摘录更新入口，补齐正文、来源、标签和挂载位置的保存链路。
4. 在详情页接入标签选择器和挂载选择器，并确保编辑态 dirty 判断包含元信息变化。
5. 将摘录流、节点详情、Backlinks、局部图谱和展示态摘录链接的回调全部切到新路由。
6. 删除 `previewExcerptId` / `ExcerptPreviewDialog` 及相关未使用代码，避免遗留双路径。
7. 运行最小必要验证，优先检查导航回退、保存后列表/节点详情/标签关系刷新、挂载切换后的局部图谱回显，以及展示态链接跳转。

回滚策略：
- 该变更不涉及数据库 schema 迁移，回滚只需恢复导航回调和旧的 `ExcerptPreviewDialog` 路径。
- 若详情页保存链路出现问题，可临时仅保留只读详情页，并把编辑入口隐藏；导航统一仍可独立成立。

## 开放问题

- 当前没有阻塞实现的开放问题。
- 若实现阶段发现“正文是否也要支持 `[[` 联想输入”会显著影响交互复杂度，应作为后续独立变更处理，而不是在本次详情页落地时顺带扩大范围。
