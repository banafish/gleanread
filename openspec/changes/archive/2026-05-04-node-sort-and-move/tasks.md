## 1. 数据层：Entity 与 Migration

- [x] 1.1 在 `KnowledgeTreeNodeEntity` 中新增 `sortOrder: Long` 字段（`@ColumnInfo(name = "sort_order")`，默认值 0）
- [x] 1.2 在 `WorkspaceMigrations.kt` 中新增 `MIGRATION_2_3`：执行 `ALTER TABLE knowledge_tree_node ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0`
- [x] 1.3 在 `WorkspaceDatabase` 中将 `version` 从 2 升到 3，并注册 `MIGRATION_2_3`

## 2. 数据层：DAO 查询排序

- [x] 2.1 修改 `KnowledgeTreeNodeDao.observeNodes()` 查询排序为 `ORDER BY sort_order ASC, create_time ASC`
- [x] 2.2 修改 `KnowledgeTreeNodeDao.getNodesOnce()` 查询排序为 `ORDER BY sort_order ASC, create_time ASC`
- [x] 2.3 修改 `KnowledgeTreeNodeDao.getAllNodesOnce()` 查询排序为 `ORDER BY sort_order ASC, create_time ASC`

## 3. Sync 层：远端模型更新

- [x] 3.1 在 `RemoteKnowledgeTreeNode` 中新增 `sortOrder: Long?` 字段（`@SerialName("sort_order")`，默认值 0）
- [x] 3.2 更新 `KnowledgeTreeNodeEntity.toRemote()` 扩展函数，映射 `sortOrder` 字段
- [x] 3.3 更新 `RemoteKnowledgeTreeNode.toEntity()` 扩展函数，映射 `sortOrder` 字段（远端为 null 时使用默认值 0）

## 4. Repository 层：排序值计算

- [x] 4.1 在 `KnowledgeTreeRepository` 中新增排序值计算辅助方法：`calculateSortOrderForAppend(parentId: String?): Long`——查询同级节点最大 `sort_order` + 间隔值
- [x] 4.2 在 `KnowledgeTreeRepository` 中新增排序值计算辅助方法：`calculateSortOrderBetween(prevSortOrder: Long?, nextSortOrder: Long?): Long`——计算中间值或边界值
- [x] 4.3 在 `KnowledgeTreeRepository` 中新增局部重排方法：`rebalanceSiblings(parentId: String?)`——对指定父节点下所有兄弟节点重新分配排序值（间隔 65536），在事务中执行
- [x] 4.4 修改 `createRootNode()` 方法，调用 `calculateSortOrderForAppend(null)` 为新根节点分配 `sortOrder`
- [x] 4.5 修改 `createChildNode()` 方法，调用 `calculateSortOrderForAppend(parentId)` 为新子节点分配 `sortOrder`
- [x] 4.6 修改/新增 `moveNode()` 方法（或新增重载），支持接收目标位置参数（`targetIndex`），计算新的 `sortOrder`，必要时触发局部重排（仅同级排序移动，不涉及 `parentId` 更新；跨层级移动由 `MoveNodeBottomSheet` 处理）

## 5. 核心模型层：排序反映

- [x] 5.1 修改 `NodeProjectionFactory.create()`，确保 `childrenByParent` 分组后的子节点列表已按 `sort_order ASC, create_time ASC` 排序（DAO 查询已排序，验证 `groupBy` 保持顺序）
- [x] 5.2 验证 `WorkspaceSnapshot.treeRoots` 列表的根节点顺序反映 `sort_order`

## 6. UI 层：拖拽排序状态管理

- [x] 6.1 在 `KnowledgeTreeRouteController` 中新增拖拽状态字段：`draggedNodeId: String?`、`dragOffsetY: Float`、`displacedNodeIds: List<String>` 等
- [x] 6.2 新增拖拽状态操作方法：`onDragStart(nodeId)`、`onDragMove(offsetY)`、`onDragEnd()`、`onDragCancel()`（仅支持同级排序，不支持跨层级移动）
- [x] 6.3 拖拽模式下禁用节点展开/收起和点击跳转

## 7. UI 层：拖拽手势与视觉反馈

- [x] 7.1 创建可复用的拖拽手势 Modifier（`Modifier.draggableNode()`），封装 `pointerInput` + `detectDragGesturesAfterLongPress`，仅作用于整张卡片，不可单独拖拽卡片内子节点
- [x] 7.2 为 `RootNodeCard` 添加长按拖拽手势支持
- [x] **7.3 修复与测试**
  - 编译并验证：确保新增组件无语法错误。
  - 在 `SampleSeedData` 构造函数中使用命名参数修复之前新增字段带来的报错。
- [x] **7.4 `BranchNodeItem` 添加拖拽**
  - 使用 `draggableNode` 和 `DraggableNodeWrapper`。
- [x] 7.5 实现拖拽中的视觉反馈：被拖拽节点 elevation 提升 + 缩放，原位置半透明占位
- [x] 7.6 实现拖拽过程中其他节点视觉位移效果：被拖拽节点经过时其他同级节点被挤上来或挤下去，用视觉位移来确定和计算插入位置
- [x] 7.7 拖拽开始时触发 HapticFeedback

#### 8. UI 层交互与数据同步（Controller & ViewModel）
- [x] **8.1 实现拖拽位置计算逻辑**
  - 编写根据被拖拽节点的 Y 轴位移和其他同级节点的视觉位移判定插入位置（被移动到了哪两个节点之间）的辅助函数。仅支持同级排序，不支持跨层级移动。
- [x] **8.2 在 `KnowledgeTreeHomeRoute` 中集成拖拽同级排序**
  - 向 `KnowledgeTreeHomeScreen` 传入 `dragSortState` 和拖拽回调。
  - `onDragEnd` 时根据视觉位移确定的插入位置触发 Repository 持久化排序（调用 `moveNodeToPosition`）。
- [x] **8.3 在 `KnowledgeTreeBranchRoute` 中集成拖拽同级排序**
  - 对分支页面的节点同理提供拖拽同级排序逻辑。结束后调用 Repository 更新。不支持跨层级移动。
- [x] 8.4 实现拖拽取消逻辑：松手未到达有效目标时被拖拽节点动画回到原位，其他节点的视觉位移动画恢复

## 9. 现有移动功能适配

- [x] 9.1 修改现有 `MoveNodeBottomSheet` 确认后的 `moveNode()` 调用，确保移动时也正确更新 `sortOrder`（追加到目标父节点末尾）

## 10. 验证与测试

- [x] 10.1 为排序值计算逻辑编写单元测试：追加、中间插入、局部重排
- [x] 10.2 为 `createRootNode`/`createChildNode` 的排序值分配编写单元测试
- [x] 10.3 为 `moveNode` 的排序值更新与跨父节点移动编写单元测试
- [x] 10.4 验证数据库迁移 `MIGRATION_2_3` 正确执行
- [ ] 10.5 手动验证知识树首页与分支页节点按 `sort_order` 排列
- [ ] 10.6 手动验证长按整张卡片拖拽同级排序功能（同级移动、取消、其他节点视觉位移效果），验证不支持跨层级移动和单独拖拽卡片内子节点
- [x] 10.7 执行 `./gradlew assembleDebug` 确保编译通过
