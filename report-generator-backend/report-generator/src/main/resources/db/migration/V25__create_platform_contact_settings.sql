-- =========================================================================
-- SwasthAI Report Generator - Platform Contact Settings (V25)
-- =========================================================================

CREATE TABLE IF NOT EXISTS platform_contact_settings (
    id BIGSERIAL PRIMARY KEY,
    support_email VARCHAR(255) NOT NULL DEFAULT 'support@swasthai.com',
    privacy_email VARCHAR(255) NOT NULL DEFAULT 'privacy@swasthai.com',
    legal_email VARCHAR(255) NOT NULL DEFAULT 'legal@swasthai.com',
    helpline_phone VARCHAR(50) NOT NULL DEFAULT '+91 99999 99999',
    whatsapp_number VARCHAR(50) NOT NULL DEFAULT '919999999999',
    operating_hours VARCHAR(255) NOT NULL DEFAULT 'Monday to Saturday, 9:00 AM – 6:00 PM IST',
    office_address TEXT NOT NULL DEFAULT 'SwasthAI Healthcare Technologies Private Limited',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO platform_contact_settings (
    id, support_email, privacy_email, legal_email, helpline_phone, whatsapp_number, operating_hours, office_address, updated_at
) VALUES (
    1,
    'support@swasthai.com',
    'privacy@swasthai.com',
    'legal@swasthai.com',
    '+91 99999 99999',
    '919999999999',
    'Monday to Saturday, 9:00 AM – 6:00 PM IST',
    'SwasthAI Healthcare Technologies Private Limited',
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

