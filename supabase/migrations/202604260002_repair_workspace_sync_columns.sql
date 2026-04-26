-- Existing projects may already have these tables from an earlier draft.
-- `create table if not exists` does not add missing columns, so keep this
-- additive repair migration separate from the initial schema migration.

alter table public.knowledge_tree_node
    add column if not exists user_id uuid,
    add column if not exists parent_id text,
    add column if not exists node_title text,
    add column if not exists outline_markdown text,
    add column if not exists create_time bigint,
    add column if not exists update_time bigint,
    add column if not exists is_deleted boolean not null default false,
    add column if not exists device_id text;

alter table public.tags
    add column if not exists user_id uuid,
    add column if not exists tag_name text,
    add column if not exists color_icon text,
    add column if not exists heat_weight integer not null default 0,
    add column if not exists create_time bigint,
    add column if not exists update_time bigint,
    add column if not exists is_deleted boolean not null default false,
    add column if not exists device_id text;

alter table public.excerpts
    add column if not exists user_id uuid,
    add column if not exists content text,
    add column if not exists url text,
    add column if not exists source_title text,
    add column if not exists user_thought text,
    add column if not exists tree_node_id text,
    add column if not exists create_time bigint,
    add column if not exists update_time bigint,
    add column if not exists is_deleted boolean not null default false,
    add column if not exists device_id text;

alter table public.excerpt_tags
    add column if not exists user_id uuid,
    add column if not exists excerpt_id text,
    add column if not exists tag_id text,
    add column if not exists create_time bigint,
    add column if not exists update_time bigint,
    add column if not exists is_deleted boolean not null default false,
    add column if not exists device_id text;
