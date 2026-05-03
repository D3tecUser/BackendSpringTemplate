alter table users
    add column if not exists email_verified boolean not null default false;

create sequence if not exists email_token_seq_generator start with 1 increment by 1;

create table if not exists email_token (
    id bigint not null default nextval('email_token_seq_generator'),
    token_hash varchar(128) not null unique,
    type varchar(50) not null,
    user_id bigint not null,
    expires_at timestamp(6) not null,
    consumed boolean not null default false,
    created_at timestamp(6) not null,
    consumed_at timestamp(6),
    primary key (id)
);

create index if not exists idx_email_token_user_id on email_token(user_id);
create index if not exists idx_email_token_type on email_token(type);
create index if not exists idx_email_token_expires_at on email_token(expires_at);

alter table if exists email_token
    add constraint fk_email_token_user
    foreign key (user_id)
    references users(id);
