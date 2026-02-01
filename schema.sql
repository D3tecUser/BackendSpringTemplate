
    create sequence role_seq_generator start with 1 increment by 1;

    create sequence users_seq_generator start with 1 increment by 1;

    create table role (
        created_at timestamp(6),
        id bigint not null,
        name varchar(255),
        primary key (id)
    );

    create table users (
        account_non_expired boolean not null,
        account_non_locked boolean not null,
        credentials_non_expired boolean not null,
        enabled boolean not null,
        created_at timestamp(6),
        id bigint not null,
        updated_at timestamp(6),
        email varchar(255) unique,
        password varchar(255),
        primary key (id)
    );

    create table users_roles (
        role_id bigint not null,
        user_id bigint not null,
        primary key (role_id, user_id)
    );

    alter table if exists users_roles 
       add constraint FKt4v0rrweyk393bdgt107vdx0x 
       foreign key (role_id) 
       references role;

    alter table if exists users_roles 
       add constraint FK2o0jvgh89lemvvo17cbqvdxaa 
       foreign key (user_id) 
       references users;

    create sequence role_seq_generator start with 1 increment by 1;

    create sequence users_seq_generator start with 1 increment by 1;

    create table role (
        created_at timestamp(6),
        id bigint not null,
        name varchar(255),
        primary key (id)
    );

    create table users (
        account_non_expired boolean not null,
        account_non_locked boolean not null,
        credentials_non_expired boolean not null,
        enabled boolean not null,
        created_at timestamp(6),
        id bigint not null,
        updated_at timestamp(6),
        email varchar(255) unique,
        password varchar(255),
        primary key (id)
    );

    create table users_roles (
        role_id bigint not null,
        user_id bigint not null,
        primary key (role_id, user_id)
    );

    alter table if exists users_roles 
       add constraint FKt4v0rrweyk393bdgt107vdx0x 
       foreign key (role_id) 
       references role;

    alter table if exists users_roles 
       add constraint FK2o0jvgh89lemvvo17cbqvdxaa 
       foreign key (user_id) 
       references users;

    create sequence role_seq_generator start with 1 increment by 1;

    create sequence users_seq_generator start with 1 increment by 1;

    create table role (
        created_at timestamp(6),
        id bigint not null,
        name varchar(255),
        primary key (id)
    );

    create table users (
        account_non_expired boolean not null,
        account_non_locked boolean not null,
        credentials_non_expired boolean not null,
        enabled boolean not null,
        created_at timestamp(6),
        id bigint not null,
        updated_at timestamp(6),
        email varchar(255) unique,
        password varchar(255),
        primary key (id)
    );

    create table users_roles (
        role_id bigint not null,
        user_id bigint not null,
        primary key (role_id, user_id)
    );

    alter table if exists users_roles 
       add constraint FKt4v0rrweyk393bdgt107vdx0x 
       foreign key (role_id) 
       references role;

    alter table if exists users_roles 
       add constraint FK2o0jvgh89lemvvo17cbqvdxaa 
       foreign key (user_id) 
       references users;
