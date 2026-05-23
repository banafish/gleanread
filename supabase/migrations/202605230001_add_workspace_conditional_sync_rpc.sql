alter table public.knowledge_tree_node
    add column if not exists sort_order bigint not null default 0;

create or replace function public.sync_knowledge_tree_node_conditional(p_rows jsonb)
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
declare
    item jsonb;
    applied_id text;
    remote_update_time bigint;
    results jsonb := '[]'::jsonb;
begin
    if auth.uid() is null then
        raise exception 'not authenticated';
    end if;

    for item in select value from jsonb_array_elements(coalesce(p_rows, '[]'::jsonb))
    loop
        begin
            if nullif(item->>'user_id', '')::uuid is distinct from auth.uid() then
                results := results || jsonb_build_array(jsonb_build_object('id', item->>'id', 'status', 'forbidden'));
                continue;
            end if;

            applied_id := null;
            insert into public.knowledge_tree_node (
                id, user_id, parent_id, node_title, outline_markdown, create_time,
                update_time, is_deleted, device_id, sort_order
            )
            values (
                item->>'id',
                (item->>'user_id')::uuid,
                item->>'parent_id',
                coalesce(item->>'node_title', ''),
                item->>'outline_markdown',
                (item->>'create_time')::bigint,
                (item->>'update_time')::bigint,
                coalesce((item->>'is_deleted')::boolean, false),
                item->>'device_id',
                coalesce((item->>'sort_order')::bigint, 0)
            )
            on conflict (id) do update set
                parent_id = excluded.parent_id,
                node_title = excluded.node_title,
                outline_markdown = excluded.outline_markdown,
                create_time = excluded.create_time,
                update_time = excluded.update_time,
                is_deleted = excluded.is_deleted,
                device_id = excluded.device_id,
                sort_order = excluded.sort_order
            where public.knowledge_tree_node.user_id = auth.uid()
              and public.knowledge_tree_node.update_time <= excluded.update_time
            returning id into applied_id;

            if applied_id is not null then
                results := results || jsonb_build_array(jsonb_build_object('id', applied_id, 'status', 'applied'));
            else
                select update_time into remote_update_time
                from public.knowledge_tree_node
                where id = item->>'id' and user_id = auth.uid();
                results := results || jsonb_build_array(
                    jsonb_build_object(
                        'id', item->>'id',
                        'status', 'conflict',
                        'remote_update_time', remote_update_time
                    )
                );
            end if;
        exception when others then
            results := results || jsonb_build_array(
                jsonb_build_object('id', item->>'id', 'status', 'error', 'error', sqlerrm)
            );
        end;
    end loop;

    return results;
end;
$$;

create or replace function public.sync_excerpts_conditional(p_rows jsonb)
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
declare
    item jsonb;
    applied_id text;
    remote_update_time bigint;
    results jsonb := '[]'::jsonb;
begin
    if auth.uid() is null then
        raise exception 'not authenticated';
    end if;

    for item in select value from jsonb_array_elements(coalesce(p_rows, '[]'::jsonb))
    loop
        begin
            if nullif(item->>'user_id', '')::uuid is distinct from auth.uid() then
                results := results || jsonb_build_array(jsonb_build_object('id', item->>'id', 'status', 'forbidden'));
                continue;
            end if;

            applied_id := null;
            insert into public.excerpts (
                id, user_id, content, url, source_title, user_thought, tree_node_id,
                create_time, update_time, is_deleted, device_id
            )
            values (
                item->>'id',
                (item->>'user_id')::uuid,
                coalesce(item->>'content', ''),
                item->>'url',
                item->>'source_title',
                item->>'user_thought',
                item->>'tree_node_id',
                (item->>'create_time')::bigint,
                (item->>'update_time')::bigint,
                coalesce((item->>'is_deleted')::boolean, false),
                item->>'device_id'
            )
            on conflict (id) do update set
                content = excluded.content,
                url = excluded.url,
                source_title = excluded.source_title,
                user_thought = excluded.user_thought,
                tree_node_id = excluded.tree_node_id,
                create_time = excluded.create_time,
                update_time = excluded.update_time,
                is_deleted = excluded.is_deleted,
                device_id = excluded.device_id
            where public.excerpts.user_id = auth.uid()
              and public.excerpts.update_time <= excluded.update_time
            returning id into applied_id;

            if applied_id is not null then
                results := results || jsonb_build_array(jsonb_build_object('id', applied_id, 'status', 'applied'));
            else
                select update_time into remote_update_time
                from public.excerpts
                where id = item->>'id' and user_id = auth.uid();
                results := results || jsonb_build_array(
                    jsonb_build_object(
                        'id', item->>'id',
                        'status', 'conflict',
                        'remote_update_time', remote_update_time
                    )
                );
            end if;
        exception when others then
            results := results || jsonb_build_array(
                jsonb_build_object('id', item->>'id', 'status', 'error', 'error', sqlerrm)
            );
        end;
    end loop;

    return results;
end;
$$;

create or replace function public.sync_tags_conditional(p_rows jsonb)
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
declare
    item jsonb;
    applied_id text;
    remote_update_time bigint;
    results jsonb := '[]'::jsonb;
begin
    if auth.uid() is null then
        raise exception 'not authenticated';
    end if;

    for item in select value from jsonb_array_elements(coalesce(p_rows, '[]'::jsonb))
    loop
        begin
            if nullif(item->>'user_id', '')::uuid is distinct from auth.uid() then
                results := results || jsonb_build_array(jsonb_build_object('id', item->>'id', 'status', 'forbidden'));
                continue;
            end if;

            applied_id := null;
            insert into public.tags (
                id, user_id, tag_name, color_icon, heat_weight, create_time,
                update_time, is_deleted, device_id
            )
            values (
                item->>'id',
                (item->>'user_id')::uuid,
                coalesce(item->>'tag_name', ''),
                item->>'color_icon',
                coalesce((item->>'heat_weight')::integer, 0),
                (item->>'create_time')::bigint,
                (item->>'update_time')::bigint,
                coalesce((item->>'is_deleted')::boolean, false),
                item->>'device_id'
            )
            on conflict (id) do update set
                tag_name = excluded.tag_name,
                color_icon = excluded.color_icon,
                heat_weight = excluded.heat_weight,
                create_time = excluded.create_time,
                update_time = excluded.update_time,
                is_deleted = excluded.is_deleted,
                device_id = excluded.device_id
            where public.tags.user_id = auth.uid()
              and public.tags.update_time <= excluded.update_time
            returning id into applied_id;

            if applied_id is not null then
                results := results || jsonb_build_array(jsonb_build_object('id', applied_id, 'status', 'applied'));
            else
                select update_time into remote_update_time
                from public.tags
                where id = item->>'id' and user_id = auth.uid();
                results := results || jsonb_build_array(
                    jsonb_build_object(
                        'id', item->>'id',
                        'status', 'conflict',
                        'remote_update_time', remote_update_time
                    )
                );
            end if;
        exception when others then
            results := results || jsonb_build_array(
                jsonb_build_object('id', item->>'id', 'status', 'error', 'error', sqlerrm)
            );
        end;
    end loop;

    return results;
end;
$$;

create or replace function public.sync_excerpt_tags_conditional(p_rows jsonb)
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
declare
    item jsonb;
    applied_id text;
    remote_update_time bigint;
    results jsonb := '[]'::jsonb;
begin
    if auth.uid() is null then
        raise exception 'not authenticated';
    end if;

    for item in select value from jsonb_array_elements(coalesce(p_rows, '[]'::jsonb))
    loop
        begin
            if nullif(item->>'user_id', '')::uuid is distinct from auth.uid() then
                results := results || jsonb_build_array(jsonb_build_object('id', item->>'id', 'status', 'forbidden'));
                continue;
            end if;

            applied_id := null;
            insert into public.excerpt_tags (
                id, user_id, excerpt_id, tag_id, create_time, update_time,
                is_deleted, device_id
            )
            values (
                item->>'id',
                (item->>'user_id')::uuid,
                item->>'excerpt_id',
                item->>'tag_id',
                (item->>'create_time')::bigint,
                (item->>'update_time')::bigint,
                coalesce((item->>'is_deleted')::boolean, false),
                item->>'device_id'
            )
            on conflict (id) do update set
                excerpt_id = excluded.excerpt_id,
                tag_id = excluded.tag_id,
                create_time = excluded.create_time,
                update_time = excluded.update_time,
                is_deleted = excluded.is_deleted,
                device_id = excluded.device_id
            where public.excerpt_tags.user_id = auth.uid()
              and public.excerpt_tags.update_time <= excluded.update_time
            returning id into applied_id;

            if applied_id is not null then
                results := results || jsonb_build_array(jsonb_build_object('id', applied_id, 'status', 'applied'));
            else
                select update_time into remote_update_time
                from public.excerpt_tags
                where id = item->>'id' and user_id = auth.uid();
                results := results || jsonb_build_array(
                    jsonb_build_object(
                        'id', item->>'id',
                        'status', 'conflict',
                        'remote_update_time', remote_update_time
                    )
                );
            end if;
        exception when others then
            results := results || jsonb_build_array(
                jsonb_build_object('id', item->>'id', 'status', 'error', 'error', sqlerrm)
            );
        end;
    end loop;

    return results;
end;
$$;

revoke all on function public.sync_knowledge_tree_node_conditional(jsonb) from public;
revoke all on function public.sync_excerpts_conditional(jsonb) from public;
revoke all on function public.sync_tags_conditional(jsonb) from public;
revoke all on function public.sync_excerpt_tags_conditional(jsonb) from public;

grant execute on function public.sync_knowledge_tree_node_conditional(jsonb) to authenticated;
grant execute on function public.sync_excerpts_conditional(jsonb) to authenticated;
grant execute on function public.sync_tags_conditional(jsonb) to authenticated;
grant execute on function public.sync_excerpt_tags_conditional(jsonb) to authenticated;
