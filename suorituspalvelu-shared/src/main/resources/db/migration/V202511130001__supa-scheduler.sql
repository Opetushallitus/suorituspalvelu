CREATE TABLE task_status (
    task_instance   TEXT NOT NULL,
    task_name       TEXT NOT NULL,
    progress        DECIMAL,
    PRIMARY KEY (task_instance)
);
