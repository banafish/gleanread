## 1. 知识树数据与状态基础

- [x] 1.1 为 `WorkspaceDao`、`WorkspaceRepository`、`WorkspaceViewModel` 补齐知识树子节点创建、节点重命名、子树软删除和分支浏览所需的数据操作
- [x] 1.2 在知识树 feature 中定义 `KnowledgeTreeHomeUiState`、`KnowledgeTreeBranchUiState`、`RootNodeCardUiModel`、`PreviewNodeUiModel`、`BranchNodeUiModel`、`NodeDestination` 等状态模型
- [x] 1.3 实现基于现有 `parentId` 关系派生首页浅层预览、分支页局部子树和面包屑路径的转换逻辑

## 2. 路由与包结构接入

- [x] 2.1 按约定创建 `feature/knowledge_tree/` 下的 `route`、`screen`、`component`、`model` 基础文件结构
- [x] 2.2 在 `WorkspaceScreen.kt` 中接入新的知识树首页路由和 `tree/branch/{nodeId}` 分支路由
- [x] 2.3 调整工作台底部导航、返回栈和 FAB 显隐规则，确保知识树分支页隐藏默认底部导航与全局 FAB，同时保留节点详情跳转逻辑

## 3. 知识树首页与分支页界面

- [x] 3.1 实现 `KnowledgeTreeTopBar`、`BreadcrumbBar`、`KnowledgeTreeHomeFab`、`KnowledgeTreeBranchFab`、`KnowledgeTreeEmptyState` 等通用页面组件
- [x] 3.2 实现 `RootNodeCard`、`PreviewNodeItem`、`BranchNodeItem`，完成首页三层预览、分支页两层预览、展开/收起和 `进入 >` 行为
- [x] 3.3 实现 `KnowledgeTreeHomeScreen` 与 `KnowledgeTreeBranchScreen`，接通页面状态、节点点击去详情/去分支的判定和空状态展示
- [x] 3.4 实现 `KnowledgeTreeSearchContent` 与页面级“展开全部 / 收起全部”菜单交互

## 4. 节点菜单与对话框操作

- [x] 4.1 实现 `NodeActionMenu`、`AddNodeDialog`、`RenameNodeDialog`、`DeleteNodeDialog` 以及对应的 dialog state
- [x] 4.2 打通首页新增根节点、分支页新增子节点和节点菜单新增子节点的保存流程，并确保保存后立即刷新当前页面
- [x] 4.3 打通节点重命名流程，并确保知识树列表、面包屑路径和后续跳转标题同步更新
- [x] 4.4 打通节点删除确认与子树软删除流程，并确保受影响节点从当前知识树页面移除、已挂载摘录回到 Inbox

## 5. 收尾验证与替换旧实现

- [x] 5.1 用新的知识树 feature 替换旧 `TreeScreen.kt` 入口实现，并清理不再使用的旧知识树 UI 代码引用
- [x] 5.2 补齐首页、分支页、节点菜单、对话框和返回栈的交互验证，覆盖设计稿中的关键浏览与操作场景
- [x] 5.3 使用 `E:\\program\\jdk21` 运行 Android 编译或相关静态检查，确认知识树重构后的代码可以成功通过构建验证
