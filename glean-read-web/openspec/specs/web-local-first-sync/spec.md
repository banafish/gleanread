# 规范：web-local-first-sync

## Purpose

定义 GleanRead Web 端的本地优先数据层、增量同步流程和冲突处理规则，确保工作台可以先从本地打开，再在后台与 Supabase 保持一致。

## Requirements

### Requirement: 工作台必须先从本地持久层初始化

系统 MUST 在页面启动时先从本地持久层读取 `knowledge_tree_node`、`excerpts`、`tags` 和 `excerpt_tags`，并以此初始化工作台状态。工作台主体必须优先响应本地数据，而不是等待远端请求完成后再渲染。

#### Scenario: 首次打开工作台

- **WHEN** 用户打开 `/app`
- **THEN** 系统 MUST 先使用本地持久层中的数据初始化界面
- **THEN** 工作台不得因为等待远端同步而长时间空白

#### Scenario: 本地数据更新后刷新界面

- **WHEN** 本地持久层中的数据发生变化
- **THEN** 系统 MUST 立即让工作台状态反映这些变化
- **THEN** UI 不得依赖远端返回作为唯一刷新来源

### Requirement: UI 内存状态必须与本地持久层分层协作

系统 MUST 使用内存状态承载画布、收件箱、抽屉和当前选中节点等高频 UI 状态，并将持久化数据落入本地持久层。用户操作必须先在内存状态上获得即时反馈，再异步写入本地持久层，避免因 I/O 阻塞破坏交互流畅度。

#### Scenario: 拖拽或选中节点

- **WHEN** 用户在画布中拖拽节点或切换选中节点
- **THEN** 系统 MUST 先更新内存状态
- **THEN** 系统不得等待持久化写入完成后才更新画面

#### Scenario: 编辑摘录或大纲

- **WHEN** 用户修改摘录、标签或节点大纲
- **THEN** 系统 MUST 先让界面反映新状态
- **THEN** 持久化写入必须在后台异步完成

### Requirement: 本地与云端必须通过轻量增量同步保持一致

系统 MUST 将本地持久层作为云端同步的中间层，并通过增量方式把变更推送到 Supabase。系统 MUST 在登录后、页面刷新后和网络恢复后主动下行远端更新；常驻运行期间的远端更新 MUST 优先使用 Supabase Realtime payload 写回本地持久层，而不是依赖固定间隔下行轮询。同步流程必须根据 `update_time`、`device_id` 和 `is_deleted` 进行判断，且必须支持断网后自动续传。

#### Scenario: pending 本地变更触发上行

- **WHEN** 本地出现 `syncStatus != "synced"` 或 `localDirtyTime != null` 的新增或更新记录
- **THEN** 系统 MUST 在短暂 debounce 后主动调度 push-only 上行
- **THEN** 系统 MUST 合并短时间内连续出现的 pending 变更，避免每次本地写入都立刻产生一次网络请求
- **THEN** 页面隐藏或即将离开时，系统 SHOULD 尝试 flush 仍未同步的 pending 变更

#### Scenario: RPC 批量上传本地变更

- **WHEN** 待上行记录属于 `knowledge_tree_node`、`excerpts`、`tags` 或 `excerpt_tags`
- **THEN** 系统 MUST 优先通过对应的 Supabase RPC 批量提交变更：`sync_knowledge_tree_node_conditional`、`sync_excerpts_conditional`、`sync_tags_conditional`、`sync_excerpt_tags_conditional`
- **THEN** RPC 请求 MUST 使用 `p_rows` 携带同一表的待同步行，并完整携带该表同步所需字段
- **THEN** 上传过程中每条记录 MUST 携带当前设备标识 `device_id`
- **THEN** RPC 可用时，系统不得退回到对各业务表发起无条件 REST upsert

#### Scenario: RPC 写入字段保持一致

- **WHEN** 本地节点、摘录、标签和摘录标签关系通过 RPC 批量上行
- **THEN** Supabase 中对应行的业务字段、`create_time`、`update_time`、`is_deleted`、`device_id` 和排序或权重字段 MUST 与本地待同步记录一致
- **THEN** 上行成功后，本地对应记录 MUST 被标记为 `synced`，清空 `syncError` 和 `localDirtyTime`

#### Scenario: 条件写入避免覆盖远端新版本

- **WHEN** Supabase 中已存在同一条记录，且远端 `update_time` 大于本地待上行记录的 `update_time`
- **THEN** 上行写入 MUST 被判定为冲突或跳过
- **THEN** 系统不得用本地旧版本覆盖 Supabase 中较新的远端版本
- **THEN** 系统 MUST 拉取或应用远端较新版本，使本地副本回到一致状态，或保留可诊断的冲突状态

#### Scenario: RPC 不可用时的安全兜底

- **WHEN** 远端尚未暴露条件同步 RPC 或 RPC 调用失败为不可用错误
- **THEN** 系统 MAY 使用 REST 条件更新/插入作为兼容兜底
- **THEN** 兜底路径仍然 MUST 基于 `update_time` 做条件写入
- **THEN** 兜底路径不得使用会覆盖远端新版本的无条件 upsert

#### Scenario: 低频兜底调度

- **WHEN** 常驻运行期间没有捕获到 pending 触发，或前一次同步失败后仍存在 pending 变更
- **THEN** 系统 MUST 通过低频兜底调度继续尝试 push-only 上行
- **THEN** 兜底调度不得发起下行拉取
- **THEN** 失败重试 MUST 使用退避策略，避免在网络或服务异常时制造请求风暴

#### Scenario: 拉取远端变更

- **WHEN** Supabase 中存在比本地更新的记录
- **THEN** 系统 MUST 在登录、页面刷新或网络恢复触发的主动同步中把这些变化增量拉回本地
- **THEN** 常驻低频兜底调度不得发起下行拉取

#### Scenario: 断网后恢复同步

- **WHEN** 用户离线期间产生了本地变更
- **THEN** 系统 MUST 暂存这些变更
- **THEN** 网络恢复后必须自动继续同步

#### Scenario: Realtime 应用远端变更

- **WHEN** Supabase Realtime 收到非当前设备产生的远端行变更
- **THEN** 系统 MUST 从 Realtime payload 中读取远端行并写入本地持久层
- **THEN** 系统 MUST 刷新工作台本地视图而不发起额外下行请求

### Requirement: 同步必须处理软删除、冲突和自我回音

系统 MUST 把删除操作视为软删除，并始终保留 `is_deleted = true` 的状态用于同步与恢复；系统 MUST 根据 `update_time` 处理多端冲突，并忽略由当前设备自己发出的回流变更，避免自我回音。

#### Scenario: 处理软删除

- **WHEN** 某条本地或远端记录被删除
- **THEN** 系统 MUST 写入或保留 `is_deleted = true`
- **THEN** 系统不得在同步流程中直接依赖物理删除

#### Scenario: 处理冲突

- **WHEN** 本地与远端对同一条记录都产生了不同修改
- **THEN** 系统 MUST 比较双方 `update_time`
- **THEN** 下行或 Realtime 应用远端变更时，系统 MUST 使用较新的版本覆盖当前工作副本
- **THEN** 上行写入时，系统 MUST 使用条件写入保护远端较新的版本不被本地旧版本覆盖

#### Scenario: 忽略自我回音

- **WHEN** 同步通知中返回的 `device_id` 等于当前设备标识
- **THEN** 系统 MUST 忽略该通知造成的重复刷新
- **THEN** 系统不得因为自我回音再次触发同一轮写入

### Requirement: 未登录或退出登录时必须停用云同步

系统 MUST 在用户未登录或退出登录后停用云同步和实时订阅，但必须保留本地持久层中的工作台数据。登录状态恢复后，系统才能重新启用云同步并继续增量收敛。

#### Scenario: 未登录进入工作台

- **WHEN** 用户尚未建立有效 Supabase 会话
- **THEN** 系统 MUST 仅使用本地持久层工作
- **THEN** 系统不得发起云端上传或拉取

#### Scenario: 退出登录

- **WHEN** 用户退出 Supabase 会话
- **THEN** 系统 MUST 停止云同步和实时订阅
- **THEN** 本地工作台数据必须继续保留

#### Scenario: 重新登录

- **WHEN** 用户重新登录并恢复有效会话
- **THEN** 系统 MUST 重新启用云同步
- **THEN** 系统 MUST 继续基于本地数据进行增量收敛
