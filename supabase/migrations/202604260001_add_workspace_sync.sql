-- =========================================================
-- 1. 知识树节点表
-- create_time / update_time 完全由 Android 本地生成
-- =========================================================

create table if not exists public.knowledge_tree_node (
    id text primary key,
    user_id uuid not null,
    parent_id text,
    node_title text not null,
    outline_markdown text,
    create_time bigint not null,
    update_time bigint not null,
    is_deleted boolean not null default false,
    device_id text
);

comment on table public.knowledge_tree_node
is '知识树节点表，用于保存用户的知识树、目录、分类节点。';

comment on column public.knowledge_tree_node.id
is '主键。本地离线生成的 UUID 字符串，避免多端冲突。';

comment on column public.knowledge_tree_node.user_id
is '数据所属用户，对应 Supabase Auth 的 auth.uid()。';

comment on column public.knowledge_tree_node.parent_id
is '父级知识树节点 ID。为空表示根节点。';

comment on column public.knowledge_tree_node.node_title
is '知识树节点标题。';

comment on column public.knowledge_tree_node.outline_markdown
is '节点下的大纲内容，使用 Markdown 格式保存。';

comment on column public.knowledge_tree_node.create_time
is '创建时间，Unix 毫秒时间戳。由 Android 本地生成并上传，Supabase 不自动修改。';

comment on column public.knowledge_tree_node.update_time
is '更新时间，Unix 毫秒时间戳。由 Android 本地生成并上传，用于同步和冲突判断，Supabase 不自动修改。';

comment on column public.knowledge_tree_node.is_deleted
is '软删除标记。true 表示已删除，但仍保留用于多端同步。';

comment on column public.knowledge_tree_node.device_id
is '最后修改该记录的设备 ID，用于多端同步、排查冲突或避免回环同步。';


-- =========================================================
-- 2. 标签表
-- create_time / update_time 完全由 Android 本地生成
-- =========================================================

create table if not exists public.tags (
    id text primary key,
    user_id uuid not null,
    tag_name text not null,
    color_icon text,
    heat_weight integer not null default 0,
    create_time bigint not null,
    update_time bigint not null,
    is_deleted boolean not null default false,
    device_id text
);

comment on table public.tags
is '标签表，用于保存用户创建的摘录标签。';

comment on column public.tags.id
is '主键。本地离线生成的 UUID 字符串。';

comment on column public.tags.user_id
is '数据所属用户，对应 Supabase Auth 的 auth.uid()。';

comment on column public.tags.tag_name
is '标签名称。同一用户下，未删除标签名称唯一。';

comment on column public.tags.color_icon
is '标签颜色或图标标识，可保存颜色值、emoji、图标 key 等。';

comment on column public.tags.heat_weight
is '标签热度权重，可用于排序、推荐或展示常用标签。';

comment on column public.tags.create_time
is '创建时间，Unix 毫秒时间戳。由 Android 本地生成并上传，Supabase 不自动修改。';

comment on column public.tags.update_time
is '更新时间，Unix 毫秒时间戳。由 Android 本地生成并上传，用于同步和冲突判断，Supabase 不自动修改。';

comment on column public.tags.is_deleted
is '软删除标记。true 表示已删除，但仍保留用于多端同步。';

comment on column public.tags.device_id
is '最后修改该记录的设备 ID，用于多端同步、排查冲突或避免回环同步。';


-- =========================================================
-- 3. 摘录表
-- create_time / update_time 完全由 Android 本地生成
-- =========================================================

create table if not exists public.excerpts (
    id text primary key,
    user_id uuid not null,
    content text not null,
    url text,
    source_title text,
    user_thought text,
    tree_node_id text,
    create_time bigint not null,
    update_time bigint not null,
    is_deleted boolean not null default false,
    device_id text
);

comment on table public.excerpts
is '摘录表，用于保存用户摘录的正文、来源、想法和所属知识树节点。';

comment on column public.excerpts.id
is '主键。本地离线生成的 UUID 字符串。';

comment on column public.excerpts.user_id
is '数据所属用户，对应 Supabase Auth 的 auth.uid()。';

comment on column public.excerpts.content
is '摘录正文内容。';

comment on column public.excerpts.url
is '摘录来源链接，可为空。';

comment on column public.excerpts.source_title
is '摘录来源标题，例如网页标题、书名、文章标题等。';

comment on column public.excerpts.user_thought
is '用户对该摘录的想法、批注或笔记。';

comment on column public.excerpts.tree_node_id
is '所属知识树节点 ID，可为空。';

comment on column public.excerpts.create_time
is '创建时间，Unix 毫秒时间戳。由 Android 本地生成并上传，Supabase 不自动修改。';

comment on column public.excerpts.update_time
is '更新时间，Unix 毫秒时间戳。由 Android 本地生成并上传，用于同步和冲突判断，Supabase 不自动修改。';

comment on column public.excerpts.is_deleted
is '软删除标记。true 表示已删除，但仍保留用于多端同步。';

comment on column public.excerpts.device_id
is '最后修改该记录的设备 ID，用于多端同步、排查冲突或避免回环同步。';


-- =========================================================
-- 4. 摘录-标签关系表
-- create_time / update_time 完全由 Android 本地生成
-- =========================================================

create table if not exists public.excerpt_tags (
    id text primary key,
    user_id uuid not null,
    excerpt_id text not null,
    tag_id text not null,
    create_time bigint not null,
    update_time bigint not null,
    is_deleted boolean not null default false,
    device_id text
);

comment on table public.excerpt_tags
is '摘录和标签的多对多关系表。';

comment on column public.excerpt_tags.id
is '主键。本地离线生成的 UUID 字符串。';

comment on column public.excerpt_tags.user_id
is '数据所属用户，对应 Supabase Auth 的 auth.uid()。';

comment on column public.excerpt_tags.excerpt_id
is '摘录 ID，对应 excerpts.id。';

comment on column public.excerpt_tags.tag_id
is '标签 ID，对应 tags.id。';

comment on column public.excerpt_tags.create_time
is '创建时间，Unix 毫秒时间戳。由 Android 本地生成并上传，Supabase 不自动修改。';

comment on column public.excerpt_tags.update_time
is '更新时间，Unix 毫秒时间戳。由 Android 本地生成并上传，用于同步和冲突判断，Supabase 不自动修改。';

comment on column public.excerpt_tags.is_deleted
is '软删除标记。true 表示该摘录和标签的关系已删除。';

comment on column public.excerpt_tags.device_id
is '最后修改该记录的设备 ID，用于多端同步、排查冲突或避免回环同步。';


-- =========================================================
-- 5. 删除旧的不适合软删除的唯一约束
-- =========================================================

alter table public.tags
drop constraint if exists tags_user_id_tag_name_key;

alter table public.excerpt_tags
drop constraint if exists excerpt_tags_excerpt_id_tag_id_key;


-- =========================================================
-- 6. 软删除友好的唯一索引
-- =========================================================

create unique index if not exists tags_user_tag_name_active_uidx
on public.tags(user_id, lower(trim(tag_name)))
where is_deleted = false;

comment on index public.tags_user_tag_name_active_uidx
is '同一用户下，未删除标签名称唯一。忽略大小写和首尾空格。';


create unique index if not exists excerpt_tags_active_uidx
on public.excerpt_tags(user_id, excerpt_id, tag_id)
where is_deleted = false;

comment on index public.excerpt_tags_active_uidx
is '同一用户下，同一摘录和同一标签之间，未删除关系唯一。';


-- =========================================================
-- 7. 删除旧的单字段索引
-- =========================================================

drop index if exists public.knowledge_tree_node_user_id_idx;
drop index if exists public.knowledge_tree_node_parent_id_idx;
drop index if exists public.knowledge_tree_node_user_update_idx;

drop index if exists public.tags_user_update_idx;
drop index if exists public.tags_heat_weight_idx;

drop index if exists public.excerpts_user_id_idx;
drop index if exists public.excerpts_tree_node_id_idx;
drop index if exists public.excerpts_create_time_idx;
drop index if exists public.excerpts_user_update_idx;

drop index if exists public.excerpt_tags_user_update_idx;
drop index if exists public.excerpt_tags_excerpt_id_idx;
drop index if exists public.excerpt_tags_tag_id_idx;


-- =========================================================
-- 8. 更适合多用户和同步查询的组合索引
-- =========================================================

create index if not exists knowledge_tree_node_user_update_idx
on public.knowledge_tree_node(user_id, update_time);

comment on index public.knowledge_tree_node_user_update_idx
is '知识树节点增量同步索引，按 user_id 和 update_time 查询。';


create index if not exists knowledge_tree_node_user_parent_idx
on public.knowledge_tree_node(user_id, parent_id, is_deleted, create_time);

comment on index public.knowledge_tree_node_user_parent_idx
is '查询某用户某父节点下的知识树子节点。';


create index if not exists tags_user_update_idx
on public.tags(user_id, update_time);

comment on index public.tags_user_update_idx
is '标签增量同步索引，按 user_id 和 update_time 查询。';


create index if not exists tags_user_heat_idx
on public.tags(user_id, is_deleted, heat_weight desc);

comment on index public.tags_user_heat_idx
is '查询用户未删除标签，并按热度权重排序。';


create index if not exists excerpts_user_update_idx
on public.excerpts(user_id, update_time);

comment on index public.excerpts_user_update_idx
is '摘录增量同步索引，按 user_id 和 update_time 查询。';


create index if not exists excerpts_user_create_idx
on public.excerpts(user_id, is_deleted, create_time desc);

comment on index public.excerpts_user_create_idx
is '查询用户未删除摘录列表，并按创建时间倒序排列。';


create index if not exists excerpts_user_tree_node_create_idx
on public.excerpts(user_id, tree_node_id, is_deleted, create_time desc);

comment on index public.excerpts_user_tree_node_create_idx
is '查询某用户某知识树节点下的摘录，并按创建时间倒序排列。';


create index if not exists excerpt_tags_user_update_idx
on public.excerpt_tags(user_id, update_time);

comment on index public.excerpt_tags_user_update_idx
is '摘录标签关系增量同步索引，按 user_id 和 update_time 查询。';


create index if not exists excerpt_tags_user_excerpt_idx
on public.excerpt_tags(user_id, excerpt_id, is_deleted);

comment on index public.excerpt_tags_user_excerpt_idx
is '查询某用户某条摘录关联的标签。';


create index if not exists excerpt_tags_user_tag_idx
on public.excerpt_tags(user_id, tag_id, is_deleted);

comment on index public.excerpt_tags_user_tag_idx
is '查询某用户某个标签关联的摘录。';


-- =========================================================
-- 9. 启用 RLS
-- =========================================================

alter table public.knowledge_tree_node enable row level security;
alter table public.tags enable row level security;
alter table public.excerpts enable row level security;
alter table public.excerpt_tags enable row level security;


-- =========================================================
-- 10. RLS 策略
-- 先删除旧策略，避免重复创建报错
-- =========================================================

drop policy if exists "knowledge tree node select own" on public.knowledge_tree_node;
drop policy if exists "knowledge tree node insert own" on public.knowledge_tree_node;
drop policy if exists "knowledge tree node update own" on public.knowledge_tree_node;
drop policy if exists "knowledge tree node delete own" on public.knowledge_tree_node;

create policy "knowledge tree node select own"
on public.knowledge_tree_node for select
using (auth.uid() = user_id);

create policy "knowledge tree node insert own"
on public.knowledge_tree_node for insert
with check (auth.uid() = user_id);

create policy "knowledge tree node update own"
on public.knowledge_tree_node for update
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "knowledge tree node delete own"
on public.knowledge_tree_node for delete
using (auth.uid() = user_id);


drop policy if exists "tags select own" on public.tags;
drop policy if exists "tags insert own" on public.tags;
drop policy if exists "tags update own" on public.tags;
drop policy if exists "tags delete own" on public.tags;

create policy "tags select own"
on public.tags for select
using (auth.uid() = user_id);

create policy "tags insert own"
on public.tags for insert
with check (auth.uid() = user_id);

create policy "tags update own"
on public.tags for update
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "tags delete own"
on public.tags for delete
using (auth.uid() = user_id);


drop policy if exists "excerpts select own" on public.excerpts;
drop policy if exists "excerpts insert own" on public.excerpts;
drop policy if exists "excerpts update own" on public.excerpts;
drop policy if exists "excerpts delete own" on public.excerpts;

create policy "excerpts select own"
on public.excerpts for select
using (auth.uid() = user_id);

create policy "excerpts insert own"
on public.excerpts for insert
with check (auth.uid() = user_id);

create policy "excerpts update own"
on public.excerpts for update
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "excerpts delete own"
on public.excerpts for delete
using (auth.uid() = user_id);


drop policy if exists "excerpt tags select own" on public.excerpt_tags;
drop policy if exists "excerpt tags insert own" on public.excerpt_tags;
drop policy if exists "excerpt tags update own" on public.excerpt_tags;
drop policy if exists "excerpt tags delete own" on public.excerpt_tags;

create policy "excerpt tags select own"
on public.excerpt_tags for select
using (auth.uid() = user_id);

create policy "excerpt tags insert own"
on public.excerpt_tags for insert
with check (auth.uid() = user_id);

create policy "excerpt tags update own"
on public.excerpt_tags for update
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "excerpt tags delete own"
on public.excerpt_tags for delete
using (auth.uid() = user_id);


-- =========================================================
-- 11. Realtime 发布
-- =========================================================

do $$
begin
    alter publication supabase_realtime add table public.knowledge_tree_node;
exception when duplicate_object then null;
end $$;

do $$
begin
    alter publication supabase_realtime add table public.tags;
exception when duplicate_object then null;
end $$;

do $$
begin
    alter publication supabase_realtime add table public.excerpts;
exception when duplicate_object then null;
end $$;

do $$
begin
    alter publication supabase_realtime add table public.excerpt_tags;
exception when duplicate_object then null;
end $$;


-- =========================================================
-- 12. Realtime 更新事件包含完整旧数据
-- 对软删除、多端同步、冲突排查更友好
-- =========================================================

alter table public.knowledge_tree_node replica identity full;
alter table public.tags replica identity full;
alter table public.excerpts replica identity full;
alter table public.excerpt_tags replica identity full;