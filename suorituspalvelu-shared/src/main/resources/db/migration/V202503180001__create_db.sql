CREATE EXTENSION IF NOT EXISTS btree_gist;

-- taulu lisätty jotta voidaa lukita yksittäinen henkilö
CREATE TABLE IF NOT EXISTS henkilot (
    oid                         VARCHAR PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS versiot (
    tunniste                    UUID PRIMARY KEY,
    use_versio_tunniste         UUID REFERENCES versiot (tunniste),
    henkilo_oid                 VARCHAR NOT NULL REFERENCES henkilot (oid),
    voimassaolo                 TSTZRANGE NOT NULL,
    suoritusjoukko              VARCHAR NOT NULL,
    data_json                   JSONB[],
    data_xml                    XML[],
    opiskeluoikeudet            JSONB,
    EXCLUDE USING gist (henkilo_oid WITH =, suoritusjoukko WITH =, voimassaolo WITH &&),
    CHECK ((suoritusjoukko='VIRTA' AND data_json IS NULL       AND data_xml IS NOT NULL) OR
                                      (data_json IS NOT NULL   AND data_xml IS NULL))
);

CREATE INDEX IF NOT EXISTS idx_versiot_opiskeluoikeudet ON versiot USING GIN (opiskeluoikeudet jsonb_path_ops);

CREATE TABLE lahtokoulut (
    versio_tunniste         UUID NOT NULL REFERENCES versiot (tunniste) ON DELETE CASCADE,
    versio_voimassaolo      TSTZRANGE NOT NULL,
    henkilo_oid             VARCHAR NOT NULL,
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
CREATE INDEX idx_lahtokoulut_henkilo_oid ON lahtokoulut (henkilo_oid) WHERE upper(versio_voimassaolo) = 'infinity'::timestamptz;