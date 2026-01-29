CREATE EXTENSION IF NOT EXISTS btree_gist;

-- taulu lisätty jotta voidaa lukita yksittäinen henkilö
CREATE TABLE IF NOT EXISTS henkilot (
    oid                         VARCHAR PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS versiot (
    tunniste                    UUID PRIMARY KEY,
    henkilo_oid                 VARCHAR NOT NULL REFERENCES henkilot (oid),
    voimassaolo                 TSTZRANGE NOT NULL,
    lahdejarjestelma            VARCHAR NOT NULL,
    lahdetunniste               VARCHAR NOT NULL,
    lahdeversio                 INTEGER,
    data_json                   JSONB[],
    data_xml                    XML[],
    opiskeluoikeudet            JSONB,
    parser_versio               INTEGER,
    EXCLUDE USING gist (henkilo_oid WITH =, lahdejarjestelma WITH =, lahdetunniste WITH =, voimassaolo WITH &&)
);

CREATE INDEX IF NOT EXISTS idx_versiot_opiskeluoikeudet ON versiot USING GIN (opiskeluoikeudet jsonb_path_ops);

CREATE UNIQUE INDEX IF NOT EXISTS idx_versiot_lahde_versio_unique ON versiot (henkilo_oid, lahdejarjestelma, lahdetunniste, lahdeversio) WHERE lahdeversio IS NOT NULL;

CREATE TABLE lahtokoulut (
    versio_tunniste         UUID NOT NULL REFERENCES versiot (tunniste),
    henkilo_oid             VARCHAR NOT NULL,
    lahdejarjestelma        VARCHAR NOT NULL,
    lahdetunniste           VARCHAR NOT NULL,
    suorituksen_alku        DATE NOT NULL,
    suorituksen_loppu       DATE,
    valmistumisvuosi        INTEGER,    -- suorituksen oletettu (tai todellinen) valmistumisvuosi tarkastusnäkymää varten
    oppilaitos_oid          VARCHAR NOT NULL,
    luokka                  VARCHAR,
    tila                    VARCHAR NOT NULL,
    arvosanapuuttuu         BOOLEAN,
    suoritustyyppi          VARCHAR NOT NULL
);

CREATE INDEX idx_lahtokoulut_oppilaitos_vuosi_oid ON lahtokoulut (oppilaitos_oid, valmistumisvuosi);
CREATE INDEX idx_lahtokoulut_henkilo_oid ON lahtokoulut (henkilo_oid);
