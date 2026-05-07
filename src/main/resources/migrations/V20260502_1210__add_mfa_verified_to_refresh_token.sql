alter table refresh_token
    add column if not exists mfa_verified boolean not null default false;
