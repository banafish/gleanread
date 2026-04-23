## 为什么

当前 Android 端摘录流主要承担浏览和多选归档职责，但单条摘录缺少一个承接“深入阅读、查看元信息、编辑内容”的独立详情页。随着摘录流卡片信息逐步收敛，继续把阅读、来源查看和编辑都压在列表卡片或弹窗里，会让信息层级不完整，也难以支撑更沉浸的阅读与轻量编辑体验。

## 变更内容

- 新增一个 Android 端摘录详情页，支持从摘录流点击单条摘录进入详情页。
- 详情页默认以阅读态展示摘录正文、想法、来源和元信息，并提供进入编辑态的入口。
- 编辑态支持在同一页面内修改摘录正文、想法、来源标题、来源链接、标签和挂载位置，并在保存后返回阅读态。
- 详情页元信息区展示当前归档节点或 Inbox、标签以及创建时间；标签和挂载位置编辑必须通过选择器入口完成，来源区在阅读态支持点击访问链接。
- 主工作台导航补充摘录详情页路由、返回行为，以及从摘录流、知识树节点挂载摘录、Backlinks、局部图谱和展示态内联摘录链接进入详情页的交互约束。

## 功能 (Capabilities)

### 新增功能
- `android-excerpt-detail-page`: 定义摘录详情页的阅读态、编辑态、元信息展示、来源展示与页面内保存行为。

### 修改功能
- `android-main-workspace`: 调整主工作台导航要求，使摘录流和知识树节点内的摘录入口统一进入摘录详情页，并维护正确的返回栈与页面层级。
- `android-inline-link-composer`: 调整展示态摘录链接的打开行为，使其统一跳转到摘录详情页，而不再使用本地预览或仅定位内容。
- `android-node-relationship-graph`: 调整局部关系图中的摘录节点点击行为，使其统一跳转到摘录详情页。
- `android-local-first-knowledge-store`: 扩展主工作台本地编辑范围，使摘录详情页的文本、标签和挂载位置编辑都只写本地库并维护待同步状态。
- `excerpt-tag-relation`: 扩展摘录与标签关系规则，使摘录详情页编辑已有摘录标签时能够同步维护 `excerpt_tags` 关系。

## 影响

- OpenSpec 规范：新增 `android-excerpt-detail-page`，并修改 `android-main-workspace`、`android-inline-link-composer`、`android-node-relationship-graph`、`android-local-first-knowledge-store` 与 `excerpt-tag-relation`。
- Android 导航与页面：`MainApp` 路由、摘录流卡片点击行为、知识树节点详情中的摘录入口、局部图谱摘录节点点击和展示态摘录链接点击，以及新的摘录详情页相关 `Route / Screen / UiState / ViewModel` 或等价结构。
- 本地数据写入：现有摘录本地存储需要承接详情页编辑后的正文、来源、挂载位置与标签关系更新，并维护待同步标记。
- UI 资源：新增页面标题、编辑/保存等文案与相关 Material 3 交互状态。
