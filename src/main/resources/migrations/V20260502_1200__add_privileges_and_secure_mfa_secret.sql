create sequence if not exists privilege_seq_generator start with 1 increment by 1;

create table if not exists privilege (
    id bigint not null default nextval('privilege_seq_generator'),
    name varchar(255) not null unique,
    created_at timestamp(6),
    primary key (id)
);

create table if not exists roles_privileges (
    role_id bigint not null,
    privilege_id bigint not null,
    primary key (role_id, privilege_id)
);

alter table if exists roles_privileges
    add constraint fk_roles_privileges_role
    foreign key (role_id)
    references role(id);

alter table if exists roles_privileges
    add constraint fk_roles_privileges_privilege
    foreign key (privilege_id)
    references privilege(id);

alter table if exists users
    alter column secret type varchar(512);

insert into privilege (id, name, created_at)
values
    (1, 'MFA_SELF_MANAGE', current_timestamp),
    (2, 'MFA_SELF_DISABLE', current_timestamp),
    (3, 'USER_ADMIN', current_timestamp),
    (4, 'SECURITY_ADMIN', current_timestamp)
on conflict (id) do nothing;

insert into roles_privileges (role_id, privilege_id)
values
    (1, 1),
    (1, 2),
    (2, 1),
    (2, 2),
    (2, 3),
    (2, 4)
on conflict do nothing;
