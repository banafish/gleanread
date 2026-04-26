## 1. SDK 与依赖基线

- [x] 1.1 将 Android `minSdk` 升级到 26，并将 `compileSdk`、`targetSdk` 升级到当前稳定 Android SDK。
- [x] 1.2 评估并升级 AGP、Gradle、Kotlin、Compose Compiler 等构建链版本，确保与新 SDK 和 Supabase Kotlin SDK 兼容。
- [x] 1.3 接入 Supabase Kotlin、Ktor client、序列化和必要的生命周期/后台调度依赖，避免引入未使用依赖。
- [x] 1.4 使用仓库指定 JDK 运行 `:app:compileDebugKotlin`，确认升级后的空接入可以编译。

## 2. Room Schema 与本地同步字段

- [x] 2.1 为四张核心表新增 `device_id`、`last_sync_time`、`sync_error`、`retry_count`、`local_dirty_time` 字段。
- [x] 2.2 将 `SyncStatus` Room 存储从 Int 迁移为字符串，并新增 `SYNCING`、`FAILED`、`CONFLICT` 状态。
- [x] 2.3 增加 Room migration，把旧 Int 状态映射为字符串状态，并保护既有数据不丢失。
- [x] 2.4 更新本地 Repository 写入路径，确保本地变更写入当前 `device_id`、`local_dirty_time` 和正确待同步状态。
- [x] 2.5 为 schema migration 和同步状态转换增加单元测试。

## 3. Supabase 登录与数据归属

- [x] 3.1 新增 Supabase client 初始化、邮箱密码登录、会话持久化和当前用户读取边界。
- [x] 3.2 新增登录/退出登录入口和 ViewModel 状态，保持 Compose UI 遵循 Route/Screen 分层。
- [x] 3.3 登录后检测 `local-user` 数据，并实现“合并到当前账号 / 保留本地模式 / 使用云端数据”三种归属选择。
- [x] 3.4 合并本地数据时保留本地 ID，更新 `user_id`、`device_id`、`local_dirty_time`，并标记为待上传。
- [x] 3.5 退出登录后停止同步与 Realtime 订阅，同时保留本地 Room 数据。

## 4. Supabase 后端模型

- [x] 4.1 编写 Supabase SQL migration，创建或调整 `knowledge_tree_node`、`tags`、`excerpts`、`excerpt_tags` 云端表。
- [x] 4.2 为四张云端表保留 `id`、`user_id`、`create_time`、`update_time`、`is_deleted`、`device_id` 和业务字段。
- [x] 4.3 为四张云端表配置 RLS，确保用户只能读写 `auth.uid()` 对应数据。
- [x] 4.4 为增量同步字段和常用查询添加必要索引。
- [x] 4.5 配置 Realtime，使前台设备能收到当前用户数据变化通知并触发拉取。

## 5. 同步引擎

- [x] 5.1 新增 Supabase remote data source，封装四张表的 upsert、软删除同步和增量查询。
- [x] 5.2 新增同步仓库，扫描 `PENDING_CREATE`、`PENDING_UPDATE`、`PENDING_DELETE`、可重试 `FAILED` 记录。
- [x] 5.3 上传时按节点/标签、摘录、摘录标签关系的依赖顺序处理，并使用 `SYNCING` 避免重复同步。
- [x] 5.4 上传成功后更新 `SYNCED`、`last_sync_time`、`retry_count` 和 `sync_error`。
- [x] 5.5 上传失败后写入 `FAILED`、`sync_error` 和递增后的 `retry_count`。
- [x] 5.6 实现启动、前台恢复、后台调度、设置页手动触发和 Realtime 通知触发的增量拉取。
- [x] 5.7 拉取远端变化时在 Room transaction 中应用数据，并继续让 UI 只读本地 Room。
- [x] 5.8 实现冲突检测：本地脏数据和云端同 ID 记录都在上次同步后变化时标记 `CONFLICT`。
- [x] 5.9 实现 MVP 冲突覆盖策略：按 `update_time` 较新者覆盖，无法可靠比较时云端优先。

## 6. 同步状态与用户反馈

- [x] 6.1 在设置页展示登录状态、同步状态、最近同步时间、失败提示和立即同步入口。
- [x] 6.2 为 `FAILED` 数据提供用户可触发的重试入口。
- [x] 6.3 为 `CONFLICT` 数据提供轻量提示，说明第一版已按覆盖策略处理，后续可扩展人工解决。
- [x] 6.4 保持未登录或离线状态下的本地读写体验不被同步 UI 阻断。

## 7. 验证

- [x] 7.1 增加 Room migration、Repository 同步状态、首次生成并持久化 `device_id`、登录数据归属和同步冲突策略的单元测试。
- [x] 7.2 增加 Supabase remote data source 的可替换测试边界，避免普通单元测试依赖真实网络。
- [x] 7.3 使用仓库指定 JDK 运行 `:app:compileDebugKotlin`。
- [x] 7.4 运行 `:app:testDebugUnitTest`，并记录任何与本变更无关的既有失败。
- [ ] 7.5 使用两个设备或模拟器账号场景手动验证：本地合并、云端替换、离线写入后补偿同步、软删除同步和冲突覆盖。
