CREATE TABLE IF NOT EXISTS yliajot (
    id             SERIAL PRIMARY KEY,
    avain          VARCHAR,
    arvo           VARCHAR,
    henkilo_oid    VARCHAR,
    haku_oid       VARCHAR,
    virkailija_oid VARCHAR,
    selite         VARCHAR,
    voimassaolo    TSTZRANGE NOT NULL
);
