
create sequence role_seq_generator start with 1 increment by 1;

create sequence users_seq_generator start with 1 increment by 1;

create table if not exists role (
                      id bigint not null default nextval('role_seq_generator'),
                      name varchar(255) not null unique,
                      created_at timestamp(6),
                      primary key (id)
);

create table if not exists users (
                       id bigint not null default nextval('users_seq_generator'),
                       email varchar(255) unique,
                       password varchar(255) not null,
                       created_at timestamp(6),
                       updated_at timestamp(6),
                       primary key (id)
);

create table if not exists users_roles (
                             role_id bigint not null,
                             user_id bigint not null,
                             primary key (role_id, user_id)
);

alter table if exists users_roles
    add constraint fk_users_roles_role
    foreign key (role_id)
    references role(id);

alter table if exists users_roles
    add constraint fk_users_roles_user
    foreign key (user_id)
    references users(id);