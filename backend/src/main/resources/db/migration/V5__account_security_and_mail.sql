ALTER TABLE accounts ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE accounts ADD COLUMN has_password BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE accounts ADD COLUMN clerk_subject VARCHAR(100) UNIQUE;
ALTER TABLE accounts ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;
UPDATE accounts SET email_verified=TRUE WHERE role='ADMIN';
CREATE TABLE auth_tokens (
 token_hash VARCHAR(64) PRIMARY KEY,
 account_id VARCHAR(40) NOT NULL REFERENCES accounts(id),
 purpose VARCHAR(20) NOT NULL,
 expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
 consumed BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX tokens_account_purpose ON auth_tokens(account_id,purpose);
CREATE INDEX tokens_expiry ON auth_tokens(expires_at);
CREATE TABLE mail_outbox (
 id VARCHAR(40) PRIMARY KEY,
 encrypted_payload TEXT NOT NULL,
 state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 attempts INTEGER NOT NULL DEFAULT 0,
 next_attempt TIMESTAMP WITH TIME ZONE NOT NULL,
 expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 last_error VARCHAR(100)
);
CREATE INDEX outbox_pending ON mail_outbox(state,next_attempt);
