## 为什么

当前知识树节点的显示顺序完全依赖 `create_time`（DAO 查询 `ORDER BY create_time ASC`）。用户无法自定义同级节点的排列顺序，也无法通过拖拽/长按操作直观地调整节点位置。这限制了知识整理的灵活性——用户可能希望把最重要的主题排在前面，或在整理过程中调整同级节点的顺序。

现有的 `moveNode()` 方法只修改了 `parentId`，不涉及排序；新增节点时也没有分配排序值。需要引入显式排序字段并提供前端同级拖拽排序交互来解决该问题。跨层级移动仍由现有的 `MoveNodeBottomSheet` 承担。

## 变更内容

1. **新增 `sort_order` 字段**：在 `knowledge_tree_node` 表中新增 `sort_order INTEGER NOT NULL DEFAULT 0` 列，使用稀疏排序策略（例如初始间隔 65536），使得在两个节点之间插入新节点时只需计算中间值，无需批量更新全部兄弟节点。
2. **数据库迁移**：新增 `MIGRATION_2_3`，通过 `ALTER TABLE` 添加 `sort_order` 列，默认值为 0。已有节点的初始排序值由 Supabase 端脚本计算，App 通过主动同步获取。
3. **节点排序查询**：修改 DAO 查询，将 `ORDER BY create_time ASC` 改为 `ORDER BY sort_order ASC`（或先按 `sort_order` 再按 `create_time` 兜底）。`NodeProjectionFactory` 中 `childrenByParent` 的子节点列表也需按此排序。
4. **新增节点排序值分配**：`createRootNode()` 和 `createChildNode()` 在创建节点时为 `sort_order` 赋值，默认追加到同级末尾。
5. **长按整个卡片进入拖拽模式**：知识树首页与分支页的节点卡片（整张卡片）支持长按进入拖拽模式，仅支持同级排序移动，不支持跨层级移动，也不能单独拖拽卡片内部的子节点。拖拽过程中，被拖拽节点经过的位置，其他节点会视觉上相应移动（往下挤或往上让），以此直观显示插入位置并计算拖拽节点被移动到了哪两个节点之间。现有的 `MoveNodeBottomSheet` 保留用于跨层级移动。
6. **拖拽后更新排序值**：拖拽结束后根据视觉位移确定目标插入位置的前后兄弟节点 `sort_order` 计算新值（稀疏排序插入）；当间隔不足时触发局部重排。

## 功能 (Capabilities)

### 新增功能
- `node-sort-order`: 覆盖知识树节点的稀疏排序字段管理，包括数据库 schema、排序值分配策略、插入/移动时的排序值计算与局部重排逻辑。
- `node-reorder-interaction`: 覆盖知识树页面长按整张卡片进入拖拽模式的 UI 交互，包括拖拽手势检测、拖拽状态管理、拖拽过程中其他节点的视觉位移（被挤上来或挤下去）以指示插入位置、拖拽结束后的排序值更新。仅支持同级排序移动，不支持跨层级移动和单独拖拽卡片内子节点。

### 修改功能
- `android-knowledge-tree-browser`: 同级节点的显示顺序从按 `create_time` 改为按 `sort_order`，影响首页根节点卡片列表与分支页子节点列表的排列规则。
- `android-knowledge-tree-node-actions`: 长按整张节点卡片触发同级拖拽排序作为新增交互方式，不支持跨层级拖拽移动和单独拖拽卡片内子节点。现有菜单中的"移动"操作保留不变，用于跨层级移动。

## 影响

- **数据层**：`KnowledgeTreeNodeEntity` 新增 `sortOrder` 字段；`KnowledgeTreeNodeDao` 查询排序条件变更（`ORDER BY sort_order ASC, create_time ASC`）；`WorkspaceDatabase` 版本号升至 3；新增 `MIGRATION_2_3`（仅加列赋 0）。
- **核心模型层**：`FlatNodeUiModel.childNodeIds` 的排列需反映 `sort_order`；`NodeProjectionFactory` 的分组排序逻辑需调整。
- **Repository 层**：`KnowledgeTreeRepository.createRootNode()`、`createChildNode()`、`moveNode()` 需计算和更新 `sort_order`。
- **UI 层**：`KnowledgeTreeHomeScreen`、`KnowledgeTreeBranchScreen` 需支持长按整张卡片进入拖拽模式（仅同级排序，不支持跨层级移动和单独拖拽卡片内子节点）；`KnowledgeTreeRouteController` 需管理拖拽状态；拖拽过程中其他节点根据被拖拽节点的位置视觉上相应移动（被挤上来或挤下去），以此指示和计算插入位置。
- **Cloud Sync**：`sort_order` 字段需要被同步；Supabase 端的 `knowledge_tree_node` 表需新增列并执行初始值脚本。
