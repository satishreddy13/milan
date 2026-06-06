CREATE TABLE flows (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    definition  TEXT         NOT NULL DEFAULT '{"nodes":[],"edges":[]}',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE execution_logs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    flow_id     UUID        NOT NULL REFERENCES flows(id) ON DELETE CASCADE,
    node_id     VARCHAR(255),
    level       VARCHAR(20)  NOT NULL DEFAULT 'INFO',
    message     TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_execution_logs_flow_id ON execution_logs(flow_id, created_at DESC);
