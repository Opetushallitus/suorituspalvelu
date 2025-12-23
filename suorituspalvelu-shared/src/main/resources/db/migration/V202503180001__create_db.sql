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

CREATE TABLE lahtokoulut (
    versio_tunniste         UUID NOT NULL REFERENCES versiot (tunniste) ON DELETE CASCADE,
    versio_voimassaolo      TSTZRANGE NOT NULL,
    oppijanumero            VARCHAR NOT NULL,
    suorituksen_alku        DATE NOT NULL,
    suorituksen_loppu       DATE,
    valmistumisvuosi        INTEGER,    -- suorituksen oletettu (tai todellinen) valmistumisvuosi tarkastusnäkymää varten
    oppilaitos_oid          VARCHAR NOT NULL,
    luokka                  VARCHAR,
    tila                    VARCHAR NOT NULL,
    arvosanapuuttuu         BOOLEAN,
    suoritustyyppi          VARCHAR NOT NULL
);

CREATE INDEX idx_lahtokoulut_oppilaitos_vuosi_oid ON lahtokoulut (oppilaitos_oid, valmistumisvuosi) WHERE upper(versio_voimassaolo) = 'infinity'::timestamptz;
CREATE INDEX idx_lahtokoulut_oppijanumero ON lahtokoulut (oppijanumero) WHERE upper(versio_voimassaolo) = 'infinity'::timestamptz;