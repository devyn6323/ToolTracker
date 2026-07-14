CREATE TABLE companies (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_user_email UNIQUE (email)
);

CREATE TABLE tools (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    asset_number VARCHAR(80) NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(255),
    manufacturer VARCHAR(255),
    model VARCHAR(255),
    serial_number VARCHAR(255),
    purchase_date DATE,
    condition VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_location VARCHAR(255),
    qr_code_value VARCHAR(255) NOT NULL,
    photo_url VARCHAR(255),
    notes VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_tool_company_asset UNIQUE (company_id, asset_number),
    CONSTRAINT uk_tool_qr UNIQUE (qr_code_value)
);

CREATE TABLE tool_transactions (
    id UUID PRIMARY KEY,
    tool_id UUID NOT NULL REFERENCES tools(id),
    user_id UUID NOT NULL REFERENCES app_users(id),
    transaction_type VARCHAR(30) NOT NULL,
    job_name VARCHAR(255),
    location VARCHAR(255),
    condition_at_checkout VARCHAR(255),
    condition_at_return VARCHAR(255),
    checked_out_at TIMESTAMPTZ,
    expected_return_at TIMESTAMPTZ,
    returned_at TIMESTAMPTZ,
    notes VARCHAR(2000)
);

CREATE INDEX idx_tools_company ON tools(company_id);
CREATE INDEX idx_transactions_tool ON tool_transactions(tool_id, checked_out_at DESC);
CREATE INDEX idx_transactions_user_open ON tool_transactions(user_id, returned_at);
