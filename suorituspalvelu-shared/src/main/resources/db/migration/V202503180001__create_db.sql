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
    EXCLUDE USING gist (oppijanumero WITH =, suoritusjoukko WITH =, voimassaolo WITH &&),
    CHECK ((suoritusjoukko='VIRTA' AND data_json IS NULL       AND data_xml IS NOT NULL) OR
                                      (data_json IS NOT NULL   AND data_xml IS NULL))
);

CREATE TABLE opiskeluoikeudet (
    versio_tunniste UUID    NOT NULL REFERENCES versiot (tunniste),
    data_parseroitu         JSONB
);

CREATE INDEX IF NOT EXISTS idx_opiskeluoikeudet_versio_tunniste ON opiskeluoikeudet(versio_tunniste);
CREATE INDEX IF NOT EXISTS idx_opiskeluoikeudet_data_parseroitu ON opiskeluoikeudet USING GIN (data_parseroitu jsonb_path_ops);

CREATE TABLE ohjausvastuut (
    versio_tunniste         UUID NOT NULL REFERENCES versiot (tunniste) ON DELETE CASCADE,
    oppijanumero            VARCHAR NOT NULL,
    voimassaolo             TSTZRANGE NOT NULL,
    versio_voimassaolo      TSTZRANGE NOT NULL,
    oppilaitos_oid          VARCHAR NOT NULL,
    valmistumisvuosi        INTEGER,
    luokka                  VARCHAR,
    tila                    VARCHAR NOT NULL,
    arvosanapuuttuu         BOOLEAN,
    toinenaste              BOOLEAN,
    EXCLUDE USING gist (versio_tunniste WITH =, voimassaolo WITH &&, oppilaitos_oid WITH =)
);

CREATE INDEX idx_ohjausvastuut_oppilaitos_oid ON ohjausvastuut (oppilaitos_oid) WHERE upper(versio_voimassaolo) = 'infinity'::timestamptz;
CREATE INDEX idx_ohjausvastuut_oppijanumero ON ohjausvastuut (oppijanumero) WHERE upper(versio_voimassaolo) = 'infinity'::timestamptz;
CREATE INDEX idx_ohjausvastuut_valmistumisvuosi ON ohjausvastuut (valmistumisvuosi) WHERE upper(versio_voimassaolo) = 'infinity'::timestamptz;
