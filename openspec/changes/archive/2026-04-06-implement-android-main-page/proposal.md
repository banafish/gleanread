## 为什么

当前 Android 客户端的启动页仍是占位内容，用户无法在应用内完成摘录流浏览、知识树整理、标签检索、AI 提炼确认和节点关系查看。本次变更聚焦安卓本地端能力，先按设计稿实现可离线使用的主工作台与本地数据闭环，为后续云同步与远端 AI 接入预留边界。

## 变更内容

- 严格按照 `doc/ui.tsx` 的页面结构与交互实现 Android 主工作台，覆盖摘录流、知识树、标签、AI 提炼页、节点详情页、全局 FAB 快速记录面板和底部导航。
- 引入 Room 本地数据层，落地 `knowledge_tree_node`、`tags`、`excerpts`、`excerpt_tags` 四张表及离线同步字段；首版所有主界面读写均只操作本地库，新增/修改数据标记为待同步。
- 当本地数据库为空时，提供内置示例数据和空状态引导，帮助用户快速理解主工作台结构与录入方式。
- 实现本地 `[[` 双向链接联想输入、本地 AI 大纲流程（不调用现有后端接口）以及节点详情页中的可交互局部图谱视图。

## 功能 (Capabilities)

### 新增功能
- `android-main-workspace`: 提供严格对齐 `doc/ui.tsx` 的 Android 主工作台，包括摘录流、知识树、标签页、AI 提炼页、节点详情页、底部导航与页面间跳转。
- `android-local-first-knowledge-store`: 提供 Room 本地数据模型、查询关系、示例数据导入与待同步标记机制，支撑主界面的离线优先读写。
- `android-local-ai-outline-workflow`: 提供本地多选摘录进入 AI 提炼页、生成大纲草稿、选择或新建目标节点并写回本地知识树的流程。
- `android-inline-link-composer`: 在摘录想法和节点大纲编辑时支持 `[[` 触发联想搜索、插入结构化链接文本，并基于本地节点/摘录数据实时检索候选项。
- `android-node-relationship-graph`: 在节点详情页提供可交互的局部关系图，展示当前节点与相关摘录、被引用节点之间的连接关系。
- `android-quick-capture-sheet`: 在主 Tab 内提供全局 Bottom Sheet 快速记录入口，默认保存到 Inbox，本地写入并标记待同步。

### 修改功能
- 无。本次变更聚焦新增 Android 本地端能力，不依赖现有后端接口行为。

## 影响

- `glean-read-android/app/src/main/java/com/gleanread/android/MainActivity.kt` 及新的 Compose 页面、导航和状态管理代码
- `glean-read-android/app/src/main/java/com/gleanread/android/**` 下新增的 Room 实体、DAO、数据库、仓储、本地 AI 草稿生成器与图谱视图代码
- `glean-read-android/app/build.gradle.kts` 的 Room、Lifecycle、Navigation Compose 等依赖和构建配置
- Android 端本地数据生命周期、示例数据导入逻辑以及未来云同步的接入边界
