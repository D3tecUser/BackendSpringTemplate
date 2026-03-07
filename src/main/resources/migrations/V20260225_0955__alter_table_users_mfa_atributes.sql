ALTER TABLE users
    ADD COLUMN mfa_enabled boolean NOT NULL DEFAULT false;

ALTER TABLE users
    ADD COLUMN secret varchar(64);