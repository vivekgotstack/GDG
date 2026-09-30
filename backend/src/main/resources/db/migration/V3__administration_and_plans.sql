ALTER TABLE accounts ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE accounts ADD COLUMN suspended BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE plan_definitions (
 id VARCHAR(20) PRIMARY KEY, name VARCHAR(80) NOT NULL, description VARCHAR(300) NOT NULL,
 monthly_price INTEGER NOT NULL, currency VARCHAR(3) NOT NULL,
 members INTEGER NOT NULL, rooms INTEGER NOT NULL, bookings INTEGER NOT NULL,
 presets INTEGER NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
INSERT INTO plan_definitions VALUES ('starter','Gather','For clubs and small teams finding their rhythm.',7,'USD',10,3,30,5,0);
INSERT INTO plan_definitions VALUES ('studio','Studio','For growing teams coordinating several shared spaces.',12,'USD',30,10,150,25,0);
INSERT INTO plan_definitions VALUES ('scale','Collective','For communities and busy shared workspaces.',20,'USD',100,30,600,100,0);
CREATE TABLE site_content (id VARCHAR(30) PRIMARY KEY, payload TEXT NOT NULL, version BIGINT NOT NULL DEFAULT 0);
INSERT INTO site_content VALUES ('public','{}',0);
CREATE TABLE admin_events (id VARCHAR(40) PRIMARY KEY, actor_id VARCHAR(40) NOT NULL, action VARCHAR(100) NOT NULL, target VARCHAR(100) NOT NULL, occurred_at TIMESTAMP WITH TIME ZONE NOT NULL);
