ALTER TABLE payments
    RENAME COLUMN method TO payment_method;

ALTER TABLE payments
    RENAME COLUMN transaction_id TO provider_reference;

ALTER TABLE payments
    ALTER COLUMN provider_reference TYPE VARCHAR(120);

ALTER TABLE payments
    ADD COLUMN provider VARCHAR(40);

UPDATE payments
SET provider = 'legacy'
WHERE provider IS NULL;

ALTER TABLE payments
    ALTER COLUMN provider SET NOT NULL;

ALTER TABLE payments
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'COP';