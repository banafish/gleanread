## 为什么

当前 Android App 已经完成本地优先的 Room 知识库能力，但数据仍停留在单设备。用户在多台设备上记录、整理和归档摘录时，需要通过 Supabase 云端同步保持数据一致，同时继续保留离线可用、本地先写入和快速响应的体验。

这次变更也会补齐登录与数据归属选择，让现有本地数据可以由用户决定是否迁移、保留或替换，避免在引入云端账号后隐式改变用户数据所有权。

## 变更内容

- 新增 Supabase 登录能力，包括会话持久化、登录状态入口，以及登录后本地数据归属选择。
- 新增本地优先的 Supabase 云端同步能力，覆盖知识树节点、标签、摘录和摘录标签关系四类数据。
- 新增同步引擎，支持本地待同步数据上传、云端增量拉取、失败重试、软删除同步和同步状态展示所需的本地元数据。
- 调整本地同步字段模型：`sync_status` 改为字符串状态，并新增 `SYNCING`、`FAILED`、`CONFLICT`；新增仅本地保存的 `last_sync_time`、`sync_error`、`retry_count`、`local_dirty_time`。
- 在本地与云端数据中都保留 `device_id`，用于记录最后修改设备，并为后续冲突诊断和多端体验扩展提供基础。
- 升级 Android SDK 基线以满足 Supabase Kotlin SDK：`minSdk` 调整为 26，并将 `compileSdk`、`targetSdk` 升级到当前稳定 Android SDK。
- BREAKING: 本地 Room schema 需要迁移，`sync_status` 从 Int 编码迁移为字符串状态，并为四张核心表补齐新的同步字段。

## 功能 (Capabilities)

### 新增功能
- `android-supabase-auth`: Android 端 Supabase 登录、会话管理，以及登录后本地数据归属选择。
- `android-supabase-cloud-sync`: Android 端本地优先的 Supabase 云端同步，包括上传、拉取、软删除、失败重试、冲突覆盖和同步状态管理。

### 修改功能
- `android-local-first-knowledge-store`: 本地知识库四张核心表的同步字段模型需要调整，`sync_status` 改为字符串状态，并增加设备、同步时间、错误、重试和本地脏数据时间字段。

## 影响

- Android 构建配置：`minSdk`、`compileSdk`、`targetSdk`、AGP/Gradle/Kotlin/Compose 兼容性，以及 Supabase/Ktor/序列化依赖。
- Android 数据层：Room entities、DAO 查询、schema migration、Repository 写入路径、同步状态转换和本地优先写入约定。
- Android 同步层：新增 Supabase remote data source、同步仓库、同步调度、失败重试和冲突处理。
- Android 登录与设置入口：新增登录状态、账号会话、数据归属选择，以及同步状态反馈。
- Supabase 后端：Postgres 表结构、Row Level Security、Auth 配置、Realtime 配置和必要的索引。
