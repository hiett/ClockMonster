CREATE TABLE IF NOT EXISTS clockmonster_jobs (
    id BIGSERIAL PRIMARY KEY UNIQUE,
    payload JSON NULL DEFAULT NULL,
    action_type VARCHAR(32) NOT NULL,
    action_url TEXT NOT NULL,
    time_type VARCHAR(32) NOT NULL,
    time_first_run TIMESTAMP NOT NULL,
    time_repeating_iterations INT NOT NULL DEFAULT 0,
    time_repeating_interval BIGINT NOT NULL DEFAULT 0
);