CREATE TABLE accounts (
 id VARCHAR(40) PRIMARY KEY, email VARCHAR(254) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL, display_name VARCHAR(80) NOT NULL,
 workspace_name VARCHAR(100) NOT NULL, timezone VARCHAR(80) NOT NULL,
 plan VARCHAR(20) NOT NULL DEFAULT 'free', stripe_customer VARCHAR(100),
 subscription_id VARCHAR(100), billing_event_time BIGINT NOT NULL DEFAULT 0
);
ALTER TABLE members ADD COLUMN owner_id VARCHAR(40) NOT NULL DEFAULT 'legacy';
ALTER TABLE rooms ADD COLUMN owner_id VARCHAR(40) NOT NULL DEFAULT 'legacy';
CREATE INDEX members_owner ON members(owner_id);
CREATE INDEX rooms_owner ON rooms(owner_id);
CREATE TABLE meeting_templates (
 id VARCHAR(40) PRIMARY KEY, owner_id VARCHAR(40) NOT NULL,
 name VARCHAR(100) NOT NULL, description VARCHAR(500) NOT NULL,
 duration_minutes INTEGER NOT NULL, capacity INTEGER NOT NULL
);
CREATE INDEX templates_owner ON meeting_templates(owner_id);
