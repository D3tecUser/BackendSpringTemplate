CREATE SEQUENCE refresh_token_seq_generator
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE refresh_token (
                               id BIGINT NOT NULL DEFAULT nextval('refresh_token_seq_generator'),
                               token_hash VARCHAR(128) NOT NULL,
                               user_id BIGINT NOT NULL,
                               expired BOOLEAN NOT NULL DEFAULT FALSE,
                               expires_at TIMESTAMP NOT NULL,
                               created_at TIMESTAMP NOT NULL,
                               expired_at TIMESTAMP,

                               CONSTRAINT pk_refresh_token PRIMARY KEY (id),
                               CONSTRAINT uq_refresh_token_token_hash UNIQUE (token_hash),
                               CONSTRAINT fk_refresh_token_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users (id)
                                       ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_user
    ON refresh_token(user_id);

CREATE INDEX idx_refresh_token_expires
    ON refresh_token(expires_at);