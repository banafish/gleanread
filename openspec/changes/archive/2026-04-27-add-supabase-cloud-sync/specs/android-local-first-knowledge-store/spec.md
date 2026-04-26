## 新增需求

## 修改需求

### 需求:Room 本地知识库必须落地四表与同步字段
系统必须在 Android 端使用 Room 持久化 `knowledge_tree_node`、`tags`、`excerpts`、`excerpt_tags` 四张表，并保留可同步到云端的 `id`、`user_id`、`create_time`、`update_time`、`is_deleted`、`device_id` 字段；其中 `excerpts` 表必须支持保存可选的 `url` 与 `source_title` 来源字段。系统还必须在本地 Room 中为四张表保留仅本地使用的 `sync_status`、`last_sync_time`、`sync_error`、`retry_count`、`local_dirty_time` 同步控制字段，且 `sync_status` 必须以字符串形式保存。

#### 场景:初始化本地数据库
- **当** 应用首次创建本地数据库
- **那么** 系统必须创建上述四张表及其主键和索引，并使新增记录能够携带待同步状态
- **那么** `excerpts` 表必须允许 `url` 与 `source_title` 为空，以兼容不同来源质量的快摘入口
- **那么** 四张表必须包含 `device_id`、`last_sync_time`、`sync_error`、`retry_count`、`local_dirty_time`
- **那么** `sync_status` 必须保存为 `SYNCED`、`PENDING_CREATE`、`PENDING_UPDATE`、`PENDING_DELETE`、`SYNCING`、`FAILED`、`CONFLICT` 之一的字符串

#### 场景:迁移既有本地数据库
- **当** 应用从旧版 Room schema 升级到支持云端同步的 schema
- **那么** 系统必须把旧的 Int 编码 `sync_status` 迁移为对应的字符串状态
- **那么** 系统必须为既有记录补齐新的同步字段且不得丢失摘录、标签、节点和关系数据

#### 场景:首次生成设备标识
- **当** 应用首次启动且本地尚未保存 `device_id`
- **那么** 系统必须生成一个稳定的设备标识并持久化
- **那么** 后续本地变更和云端上传必须复用该 `device_id`

### 需求:本地变更必须维护待同步标记
任何由主工作台发起的新增、编辑、删除和挂载操作都必须只写本地库，并维护受影响记录的待同步状态。发生本地变更时，系统必须写入当前 `device_id`、更新 `local_dirty_time`，并根据操作类型设置字符串 `sync_status`。

#### 场景:挂载摘录到知识树节点
- **当** 用户在 AI 提炼页把未归档摘录挂载到某个节点
- **那么** 系统必须只更新本地表中的节点和摘录记录，并把受影响的数据标记为待同步，而不得调用现有后端接口
- **那么** 系统必须更新受影响记录的 `device_id`、`update_time` 和 `local_dirty_time`

#### 场景:在摘录详情页编辑摘录内容
- **当** 用户在摘录详情页修改摘录正文、想法或来源信息并保存
- **那么** 系统必须只更新本地 `excerpts` 表中的对应记录，而不得调用现有后端接口
- **那么** 系统必须保留原 `create_time`，并把该摘录的 `sync_status` 标记为待同步更新
- **那么** 系统必须更新该摘录的 `device_id`、`update_time` 和 `local_dirty_time`

#### 场景:在摘录详情页修改挂载位置
- **当** 用户在摘录详情页将摘录改挂到其他知识树节点或改回 Inbox 并保存
- **那么** 系统必须只更新该摘录本地记录中的 `tree_node_id`、`update_time` 和 `sync_status`
- **那么** 摘录流、节点详情和局部图谱后续读取时必须能够立即反映新的本地挂载关系
- **那么** 系统必须更新该摘录的 `device_id` 和 `local_dirty_time`

#### 场景:本地软删除记录
- **当** 用户删除摘录、标签或知识树节点
- **那么** 系统必须优先在本地记录中写入 `is_deleted = true`
- **那么** 系统必须把该记录的 `sync_status` 标记为 `PENDING_DELETE`
- **那么** 系统必须更新该记录的 `device_id`、`update_time` 和 `local_dirty_time`

## 移除需求
