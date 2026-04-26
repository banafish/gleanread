## 上下文

Android App 当前已经采用本地优先架构：工作台数据由 Room 四张表驱动，Repository 负责写入，UI 通过本地快照渲染。现有实体已经包含 `user_id`、`is_deleted`、`sync_status` 和时间字段，为同步能力预留了基础，但当前 `user_id` 固定为本地用户，`sync_status` 以 Int 编码保存，也没有登录、远端存储、失败重试或冲突处理。

Supabase 将作为第一版云端同步后端，承担 Auth、Postgres、RLS 和 Realtime 通知能力。Android 端仍然保持本地优先：所有用户操作先写 Room，云端同步由独立同步层异步完成。

## 目标 / 非目标

**目标：**
- 引入 Supabase 登录，并在登录后让用户显式选择现有本地数据归属。
- 将 Android SDK 基线升级到可直接使用 Supabase Kotlin SDK 的版本，`minSdk` 调整为 26，并将 `compileSdk`、`targetSdk` 升级到当前稳定 Android SDK。
- 为 `knowledge_tree_node`、`tags`、`excerpts`、`excerpt_tags` 建立本地与 Supabase 云端之间的双向同步。
- 保持本地优先体验：离线时继续可读写，联网后自动补偿同步。
- 将本地 `sync_status` 从 Int 改为字符串状态，并增加同步失败、重试、最近同步时间和本地脏数据时间等本地元数据。
- 在本地与云端都保存 `device_id`，用于记录最后修改设备。
- MVP 阶段检测多端冲突，但先采用覆盖策略，不提供人工合并 UI。

**非目标：**
- 不在第一版实现字段级合并、冲突详情 diff 或手动冲突解决页面。
- 不引入自建 Spring Boot 同步服务作为 Supabase 代理。
- 不把业务写入改成在线优先，也不让 Composable 或 Screen 直接访问 Supabase。
- 不在第一版实现端到端加密或跨账号共享协作。

## 决策

### 决策 1: 保持 Room 为唯一 UI 数据源

所有页面继续只读取 Room 派生出的本地状态。创建、编辑、删除、挂载和标签关系变更都先写本地表，并把相关记录标记为待同步。同步层只负责把本地变化推到 Supabase，以及把远端变化应用回 Room。

这样可以继续复用现有 Repository 和快照模型，避免网络状态影响主流程。替代方案是 UI 直接读取 Supabase 或 Repository 写入时同步等待云端结果，但这会破坏现有离线能力，并让快摘等高频入口变慢。

### 决策 2: Supabase 登录不隐式接管本地数据

第一版登录方式使用邮箱密码登录，不接入 magic link 或第三方 OAuth。用户可以在未登录时继续使用本地库。登录 Supabase 后，如果检测到 `local-user` 数据，必须显示归属选择：
- 合并到当前账号：保留本地 ID，将 `user_id` 改为 Supabase 用户 ID，并标记为待上传。
- 保留本地模式：不上传本地数据，不开启云同步。
- 使用云端数据：清空本地工作台数据，再拉取当前账号云端数据。

这避免了登录动作隐式上传、覆盖或删除用户数据。替代方案是登录后自动合并，但风险较高，尤其是用户只是临时登录或登录到错误账号时。邮箱密码登录的替代方案包括 magic link 和第三方 OAuth；第一版选择邮箱密码，是因为它能以最少外部回调配置覆盖登录、退出、会话刷新和错误提示这些同步前置能力。

### 决策 3: 本地同步状态使用字符串枚举

Room 中 `sync_status` 改为字符串字段，直接保存 `SYNCED`、`PENDING_CREATE`、`PENDING_UPDATE`、`PENDING_DELETE`、`SYNCING`、`FAILED`、`CONFLICT`。Kotlin 代码仍然使用 `enum class SyncStatus`，通过 Room converter 保存为字符串。

这样数据库可读性更好，也便于调试和迁移。替代方案是继续保存 Int 编码，但新增状态后可读性差，调试失败和冲突时不直观。

### 决策 4: 云端只保存领域同步字段，本地保存同步控制字段

本地与云端都保留：
- `id`
- `user_id`
- `create_time`
- `update_time`
- `is_deleted`
- `device_id`

仅本地 Room 保留：
- `sync_status`
- `last_sync_time`
- `sync_error`
- `retry_count`
- `local_dirty_time`

云端表不保存本地同步控制字段，避免把某台设备的重试状态、错误文案或本地同步状态污染到其他设备。

`device_id` 在应用首次启动时生成并持久化，代表当前安装实例；清除应用数据后可以重新生成。它不绑定 Supabase 账号，因此同一设备切换账号时仍可作为本机安装的最后修改来源。

### 决策 5: 同步引擎按依赖顺序上传，并用状态避免重复同步

同步器扫描 `PENDING_CREATE`、`PENDING_UPDATE`、`PENDING_DELETE`、可重试的 `FAILED` 记录，上传前将记录切换为 `SYNCING`，成功后设为 `SYNCED` 并更新 `last_sync_time`，失败后设为 `FAILED` 并记录 `sync_error`、`retry_count`。

上传顺序必须尊重关系依赖：知识树节点和标签先于摘录，摘录先于摘录标签关系。删除继续使用 `is_deleted` 软删除，并同步 tombstone，而不是立即物理删除。

### 决策 6: 增量拉取负责正确性，Realtime 只作为触发器

登录后，应用启动、前台恢复和后台调度都可以执行增量拉取；用户手动“立即同步”入口仅在设置页暴露，不放在主工作台。Supabase Realtime 用来在前台接收远端变化通知并触发拉取，但同步正确性不依赖 Realtime 必达。

这种设计可以覆盖应用离线、后台休眠、Realtime 断线或通知丢失的情况，同时避免在主工作台增加同步操作噪音。

### 决策 7: 冲突先检测、标记并覆盖

当本地记录有未同步变更，同时云端同一 `id` 也在本地上次同步后发生变化时，系统必须把本地记录标记为 `CONFLICT`。MVP 阶段不阻塞同步流程，也不展示手动合并 UI，而是按 `update_time` 选择较新的版本覆盖当前数据；时间相同或无法可靠比较时，云端版本优先。

这个策略牺牲了第一版的精细合并，但能让多端同步尽快闭环，并保留 `CONFLICT` 作为后续冲突详情和人工解决入口的扩展点。

## 风险 / 权衡

- [Room 迁移风险] `sync_status` 从 Int 到 String 且四张表新增字段，迁移不完整可能导致旧用户数据丢失或无法启动 → 增加 migration 单元测试，覆盖四张表和旧状态码映射。
- [SDK 升级风险] 升级 `compileSdk`、`targetSdk`、AGP/Kotlin/Compose 相关依赖可能暴露已有兼容问题 → 将 SDK/构建链升级作为独立任务先编译验证，再接入 Supabase。
- [冲突覆盖风险] MVP 覆盖策略可能丢失某台设备上的编辑 → 明确标记 `CONFLICT`，保留后续人工合并扩展点，并在同步日志或 UI 中暴露冲突状态。
- [RLS 配置风险] Supabase 表如果未正确配置 RLS，会产生跨用户数据泄漏 → 后端 schema 必须包含 RLS 策略验证任务，并用非当前用户访问作为测试场景。
- [关系同步顺序风险] 摘录标签关系可能先于摘录或标签到达 → 上传按依赖顺序，拉取应用时在同一个事务内处理，必要时允许关系记录等待下一轮重试。
- [多设备时钟风险] `update_time` 依赖设备时间，时间漂移会影响覆盖策略 → 第一版接受该权衡，同时保存 `device_id` 和 `local_dirty_time` 便于诊断；后续可引入服务端修订号。

## 迁移计划

1. 升级 Android 构建基线并确认 `:app:compileDebugKotlin` 通过。
2. 扩展 Room schema：新增 `device_id`、`last_sync_time`、`sync_error`、`retry_count`、`local_dirty_time`，并将 `sync_status` 迁移为字符串。
3. 更新本地 Repository 写入路径，确保本地变更写入当前 `device_id`、`local_dirty_time` 和字符串同步状态。
4. 接入 Supabase Auth 和会话持久化，新增登录入口与本地数据归属选择。
5. 建立 Supabase 表结构、RLS、索引和 Realtime 配置。
6. 实现同步 remote data source、同步仓库和调度入口。
7. 增加同步状态、失败重试和冲突覆盖测试。

回滚策略：若同步功能尚未发布给用户，可回滚应用代码与 Supabase 配置；若已发布 Room migration，则后续版本不得降低数据库版本，只能通过新 migration 兼容或禁用同步入口。

## 未决问题

- 无。
