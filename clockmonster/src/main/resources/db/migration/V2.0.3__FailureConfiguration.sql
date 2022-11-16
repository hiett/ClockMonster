ALTER TABLE clockmonster_jobs
    ADD failure_dead_letter_action JSON NULL DEFAULT NULL,
    ADD failure_iterations_count INT NOT NULL DEFAULT 0,
    ADD failure_backoff JSON NULL DEFAULT NULL;