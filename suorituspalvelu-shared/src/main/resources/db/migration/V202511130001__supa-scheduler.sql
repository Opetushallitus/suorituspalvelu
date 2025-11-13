--CREATE UNIQUE INDEX scheduled_tasks_task_instance_idx ON scheduled_tasks (task_instance);

CREATE TABLE task_status (
    task_instance   TEXT NOT NULL,
    task_name       TEXT NOT NULL,
    progress        DECIMAL,
    PRIMARY KEY (task_instance)
);
