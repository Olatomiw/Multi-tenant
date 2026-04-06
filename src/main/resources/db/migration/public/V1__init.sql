CREATE TABLE IF NOT EXISTS public.tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    schema_name VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

INSERT INTO public.tenants (tenant_id, schema_name, name, status)
VALUES
    ('Ministry_Health', 'ministry_health', 'Ministry of Health', 'ACTIVE'),
    ('Ministry_Education', 'ministry_education', 'Ministry of Education', 'ACTIVE');