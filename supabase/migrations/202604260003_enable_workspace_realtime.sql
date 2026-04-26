-- Ensure existing projects that already applied earlier workspace migrations
-- also publish workspace tables to Supabase Realtime.

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

alter table public.knowledge_tree_node replica identity full;
alter table public.tags replica identity full;
alter table public.excerpts replica identity full;
alter table public.excerpt_tags replica identity full;
