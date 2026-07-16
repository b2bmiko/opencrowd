-- Access Matrix entries: normalized permission data from all connected apps
CREATE TABLE access_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    principal_type  VARCHAR(10) NOT NULL,        -- USER or GROUP
    principal_name  VARCHAR(255) NOT NULL,       -- username or group name
    application     VARCHAR(50) NOT NULL,        -- xwiki, openproject, nextcloud
    resource_type   VARCHAR(50) NOT NULL,        -- wiki, space, page, project, folder
    resource_name   VARCHAR(255) NOT NULL,       -- space name, project name, etc.
    permission      VARCHAR(50) NOT NULL,        -- view, edit, delete, admin, comment, etc.
    allow           BOOLEAN NOT NULL DEFAULT true, -- allow or deny
    source          VARCHAR(20) NOT NULL DEFAULT 'synced', -- synced, manual, profile
    connector_id    UUID,
    synced_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_access_entries_principal ON access_entries(principal_type, principal_name);
CREATE INDEX idx_access_entries_application ON access_entries(application);
CREATE INDEX idx_access_entries_resource ON access_entries(resource_type, resource_name);
CREATE INDEX idx_access_entries_permission ON access_entries(permission);
