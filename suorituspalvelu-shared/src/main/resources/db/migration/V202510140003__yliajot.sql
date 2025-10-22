CREATE TABLE IF NOT EXISTS yliajot (
    id             SERIAL PRIMARY KEY,
    avain          VARCHAR,
    arvo           VARCHAR,
    henkilo_oid    VARCHAR,
    haku_oid       VARCHAR,
    virkailija_oid VARCHAR,
    selite         VARCHAR,
    voimassaolo    TSTZRANGE NOT NULL,
    EXCLUDE USING gist (henkilo_oid WITH =, haku_oid WITH =, avain WITH =, voimassaolo WITH &&)
);

CREATE INDEX IF NOT EXISTS yliajot_henkilo_haku_idx ON yliajot (henkilo_oid, haku_oid);
