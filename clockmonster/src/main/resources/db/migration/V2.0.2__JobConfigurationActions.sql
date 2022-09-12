ALTER TABLE clockmonster_jobs
    DROP COLUMN action_type,
    DROP COLUMN action_url;

ALTER TABLE clockmonster_jobs
    ADD action JSON NULL DEFAULT NULL;

ALTER TABLE clockmonster_jobs
    RENAME time_first_run TO time_next_run;