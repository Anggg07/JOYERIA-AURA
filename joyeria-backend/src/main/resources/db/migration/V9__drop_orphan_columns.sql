-- Columnas huérfanas: sin campos en las entidades JPA ni uso en queries nativas.
ALTER TABLE cart DROP COLUMN IF EXISTS session_id;
ALTER TABLE support_tickets DROP COLUMN IF EXISTS customer_name;
ALTER TABLE support_tickets DROP COLUMN IF EXISTS customer_email;
ALTER TABLE ticket_responses DROP COLUMN IF EXISTS is_staff;
ALTER TABLE inventory DROP COLUMN IF EXISTS reference;
