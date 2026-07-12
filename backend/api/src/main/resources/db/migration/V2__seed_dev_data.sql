-- Seed data for development environment
-- Creates a default tenant 'acme' for local development

INSERT INTO public.tenants (slug, name, status, plan, settings)
VALUES ('acme', 'Acme Corporation', 'active', 'community', '{"description": "Default development tenant"}')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO public.platform_admins (external_id, email, name)
VALUES ('dev-admin-001', 'admin@opencrowd.local', 'Platform Admin')
ON CONFLICT (external_id) DO NOTHING;
