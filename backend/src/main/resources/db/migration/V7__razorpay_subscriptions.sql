-- Historical account columns are retained for existing database/import compatibility.
-- All new billing uses these provider-specific records, never historical subscription IDs.
CREATE TABLE payment_subscriptions (
 id VARCHAR(100) PRIMARY KEY,
 owner_id VARCHAR(40) NOT NULL REFERENCES accounts(id),
 plan_id VARCHAR(20) NOT NULL REFERENCES plan_definitions(id),
 provider_plan_id VARCHAR(100) NOT NULL,
 key_id VARCHAR(100) NOT NULL,
 amount INTEGER NOT NULL CHECK (amount >= 100),
 currency VARCHAR(3) NOT NULL,
 status VARCHAR(30) NOT NULL DEFAULT 'created',
 paid_until TIMESTAMP WITH TIME ZONE,
 current_end TIMESTAMP WITH TIME ZONE,
 paid_count INTEGER NOT NULL DEFAULT 0,
 cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
 pending_plan_id VARCHAR(20),
 pending_provider_plan_id VARCHAR(100),
 pending_amount INTEGER,
 pending_currency VARCHAR(3),
 payment_method VARCHAR(30),
 last_payment_id VARCHAR(100),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 synced_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX payment_subscriptions_owner ON payment_subscriptions(owner_id, created_at DESC);
CREATE INDEX payment_subscriptions_sync ON payment_subscriptions(synced_at, status);

CREATE TABLE payment_webhook_events (
 id VARCHAR(100) PRIMARY KEY,
 processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
