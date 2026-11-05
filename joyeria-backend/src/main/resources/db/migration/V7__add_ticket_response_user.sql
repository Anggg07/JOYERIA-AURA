ALTER TABLE ticket_responses
    ADD COLUMN user_id BIGINT REFERENCES users(id);