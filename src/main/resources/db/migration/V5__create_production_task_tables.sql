create sequence production_task_number_seq start with 1 increment by 1;

create table production_task (
    id uuid primary key,
    task_number varchar(32) not null unique,
    order_id uuid not null,
    order_item_id uuid,
    purpose varchar(255) not null,
    item_name varchar(255) not null,
    quantity numeric(19, 4) not null,
    uom varchar(32) not null,
    status varchar(32) not null,
    previous_active_status varchar(32),
    executor_user_id uuid,
    planned_start_date date,
    planned_finish_date date,
    blocked_reason varchar(500),
    created_by_user_id uuid not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null,
    constraint fk_production_task_order foreign key (order_id) references customer_order(id),
    constraint fk_production_task_order_item foreign key (order_item_id) references customer_order_item(id),
    constraint fk_production_task_executor foreign key (executor_user_id) references app_user(id),
    constraint fk_production_task_created_by foreign key (created_by_user_id) references app_user(id),
    constraint chk_production_task_quantity_positive check (quantity > 0)
);

create table production_task_history_event (
    id uuid primary key,
    task_id uuid not null,
    event_type varchar(64) not null,
    actor_user_id uuid not null,
    event_at timestamp with time zone not null,
    from_status varchar(32),
    to_status varchar(32),
    previous_executor_user_id uuid,
    new_executor_user_id uuid,
    planned_start_date_before date,
    planned_start_date_after date,
    planned_finish_date_before date,
    planned_finish_date_after date,
    reason varchar(500),
    note varchar(500),
    constraint fk_production_task_history_task foreign key (task_id) references production_task(id),
    constraint fk_production_task_history_actor foreign key (actor_user_id) references app_user(id),
    constraint fk_production_task_history_previous_executor foreign key (previous_executor_user_id) references app_user(id),
    constraint fk_production_task_history_new_executor foreign key (new_executor_user_id) references app_user(id)
);

create table production_task_audit_event (
    id uuid primary key,
    event_type varchar(128) not null,
    actor_user_id uuid not null,
    target_type varchar(64) not null,
    target_id uuid not null,
    event_at timestamp with time zone not null,
    summary varchar(500) not null,
    metadata text,
    constraint fk_production_task_audit_actor foreign key (actor_user_id) references app_user(id)
);

create index idx_production_task_order_id on production_task(order_id);
create index idx_production_task_order_item_id on production_task(order_item_id);
create index idx_production_task_status on production_task(status);
create index idx_production_task_executor_user_id on production_task(executor_user_id);
create index idx_production_task_planned_finish_date on production_task(planned_finish_date);
create index idx_production_task_history_task_event_at on production_task_history_event(task_id, event_at);
create index idx_production_task_audit_target on production_task_audit_event(target_type, target_id);

insert into app_role (id, code, name, created_at)
values
    ('00000000-0000-0000-0000-000000000105', 'PRODUCTION_SUPERVISOR', 'Production Supervisor', now()),
    ('00000000-0000-0000-0000-000000000106', 'PRODUCTION_EXECUTOR', 'Production Executor', now())
on conflict (code) do nothing;
