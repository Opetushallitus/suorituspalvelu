CREATE TABLE IF NOT EXISTS cas_client_session (
    mapped_ticket_id VARCHAR PRIMARY KEY,
    session_id CHAR(36) NOT NULL UNIQUE,
    CONSTRAINT cas_client_session_fk FOREIGN KEY (session_id) REFERENCES spring_session(session_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS cas_client_session_idx ON cas_client_session (mapped_ticket_id);
