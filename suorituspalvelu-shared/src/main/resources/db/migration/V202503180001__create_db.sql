CREATE EXTENSION IF NOT EXISTS btree_gist;

-- taulu lisätty jotta voidaa lukita yksittäinen oppija
CREATE TABLE IF NOT EXISTS oppijat (
    oppijanumero                VARCHAR PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS versiot (
    tunniste                    UUID PRIMARY KEY,
    use_versio_tunniste         UUID REFERENCES versiot (tunniste),
    oppijanumero                VARCHAR NOT NULL REFERENCES oppijat (oppijanumero),
    voimassaolo                 TSTZRANGE NOT NULL,
    suoritusjoukko              VARCHAR NOT NULL,
    data_json                   JSONB[],
    data_xml                    XML[],
    metadata                    VARCHAR[],
    EXCLUDE USING gist (oppijanumero WITH =, suoritusjoukko WITH =, voimassaolo WITH &&),
    CHECK ((suoritusjoukko='VIRTA' AND data_json IS NULL       AND data_xml IS NOT NULL) OR
                                      (data_json IS NOT NULL   AND data_xml IS NULL))
);

CREATE INDEX IF NOT EXISTS idx_versiot_metadata ON versiot USING GIN (metadata);

create table opiskeluoikeudet (
    versio_tunniste UUID    NOT NULL REFERENCES versiot (tunniste),
    data_parseroitu         JSONB
);

CREATE INDEX IF NOT EXISTS idx_opiskeluoikeudet_versio_tunniste ON opiskeluoikeudet(versio_tunniste);
CREATE INDEX IF NOT EXISTS idx_opiskeluoikeudet_data_parseroitu ON opiskeluoikeudet USING GIN (data_parseroitu jsonb_path_ops);

create table metadata_arvot (
    avain   VARCHAR,
    arvo    VARCHAR,
    PRIMARY KEY (avain, arvo)
)