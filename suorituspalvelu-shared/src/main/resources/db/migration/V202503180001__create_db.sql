CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TYPE lahde AS ENUM ('KOSKI', 'YTR', 'VIRTA', 'VIRKAILIJA');

-- taulu lisätty jotta voidaa lukita yksittäinen oppija
CREATE TABLE IF NOT EXISTS oppijat (
    oppijanumero                VARCHAR PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS versiot (
    tunniste                    UUID PRIMARY KEY,
    oppijanumero                VARCHAR NOT NULL REFERENCES oppijat (oppijanumero),
    voimassaolo                 TSTZRANGE NOT NULL,
    lahde                       lahde NOT NULL,
    data_json                   JSONB,
    data_xml                    XML,
    EXCLUDE USING gist (oppijanumero WITH =, lahde WITH =, voimassaolo WITH &&),
    CHECK ((lahde='KOSKI'       AND data_json IS NOT NULL   AND data_xml IS NULL) OR
           (lahde='YTR'         AND data_json IS NOT NULL   AND data_xml IS NULL) OR
           (lahde='VIRTA'       AND data_json IS NULL       AND data_xml IS NOT NULL) OR
           (lahde='VIRKAILIJA'  AND data_json IS NOT NULL   AND data_xml IS NULL))
);

CREATE TABLE IF NOT EXISTS tuvat (
    versio_tunniste         UUID REFERENCES versiot (tunniste) ON DELETE CASCADE,
    koodi                   VARCHAR NOT NULL,
    vahvistuspaivamaara     DATE
);
CREATE INDEX tuvat_versio_tunniste_idx ON tuvat (versio_tunniste);

CREATE TABLE IF NOT EXISTS telmat (
    versio_tunniste         UUID REFERENCES versiot (tunniste) ON DELETE CASCADE,
    koodi                   VARCHAR NOT NULL
);
CREATE INDEX telmat_versio_tunniste_idx ON telmat (versio_tunniste);

CREATE TABLE IF NOT EXISTS perusopetuksen_vuosiluokat (
    versio_tunniste         UUID REFERENCES versiot (tunniste) ON DELETE CASCADE,
    nimi                    VARCHAR NOT NULL,
    koodi                   VARCHAR NOT NULL
);
CREATE INDEX perusopetuksen_vuosiluokat_versio_tunniste_idx ON perusopetuksen_vuosiluokat (versio_tunniste);

CREATE TABLE IF NOT EXISTS perusopetuksen_oppimaarat (
    tunniste                UUID PRIMARY KEY,
    versio_tunniste         UUID REFERENCES versiot (tunniste) ON DELETE CASCADE,
    vahvistuspaivamaara     DATE
);
CREATE INDEX perusopetuksen_oppimaarat_versio_tunniste_idx ON perusopetuksen_oppimaarat (versio_tunniste);

CREATE TABLE IF NOT EXISTS perusopetuksen_oppiaineet (
    oppimaara_tunniste      UUID REFERENCES perusopetuksen_oppimaarat (tunniste) ON DELETE CASCADE,
    nimi                    VARCHAR NOT NULL,
    koodi                   VARCHAR NOT NULL,
    arvosana                VARCHAR NOT NULL
);
CREATE INDEX perusopetuksen_oppiaineet_oppimaara_tunniste_idx ON perusopetuksen_oppiaineet (oppimaara_tunniste);

CREATE TABLE IF NOT EXISTS nuorten_perusopetuksen_oppiaineen_oppimaarat (
    versio_tunniste         UUID REFERENCES versiot (tunniste) ON DELETE CASCADE,
    nimi                    VARCHAR NOT NULL,
    koodi                   VARCHAR NOT NULL,
    arvosana                VARCHAR NOT NULL
);
CREATE INDEX nuorten_perusopetuksen_oppiaineen_oppimaarat_versio_tunniste_idx ON nuorten_perusopetuksen_oppiaineen_oppimaarat (versio_tunniste);

CREATE TABLE IF NOT EXISTS ammatilliset_tutkinnot (
    tunniste                UUID PRIMARY KEY,
    versio_tunniste         UUID REFERENCES versiot (tunniste) ON DELETE CASCADE,
    nimi                    VARCHAR NOT NULL,
    koodi                   VARCHAR NOT NULL,
    koodisto                VARCHAR NOT NULL,
    tila                    VARCHAR NOT NULL,
    tilakoodisto            VARCHAR NOT NULL,
    suoritustapa            VARCHAR NOT NULL,
    suoritustapakoodisto    VARCHAR NOT NULL,
    keskiarvo               DECIMAL(3, 1),
    vahvistuspaivamaara     DATE
);
CREATE INDEX ammatilliset_tutkinnot_versio_tunniste_idx ON ammatilliset_tutkinnot (versio_tunniste);

CREATE TABLE IF NOT EXISTS ammatillisen_tutkinnon_osat (
    tunniste                UUID PRIMARY KEY,
    tutkinto_tunniste       UUID REFERENCES ammatilliset_tutkinnot (tunniste) ON DELETE CASCADE,
    nimi                    VARCHAR NOT NULL,
    koodi                   VARCHAR NOT NULL,
    yto                     BOOLEAN NOT NULL,
    arvosana                VARCHAR,
    arvosanaasteikko        VARCHAR,
    laajuus                 VARCHAR NOT NULL,
    laajuusasteikko         VARCHAR NOT NULL
);
CREATE INDEX ammatillisen_tutkinnon_osat_tutkinto_tunniste_idx ON ammatillisen_tutkinnon_osat (tutkinto_tunniste);

CREATE TABLE IF NOT EXISTS ammatillisen_tutkinnon_osaalueet (
    osa_tunniste            UUID REFERENCES ammatillisen_tutkinnon_osat (tunniste) ON DELETE CASCADE,
    nimi                    VARCHAR NOT NULL,
    koodi                   VARCHAR NOT NULL,
    arvosana                VARCHAR,
    arvosanaasteikko        VARCHAR,
    laajuus                 VARCHAR NOT NULL,
    laajuusasteikko         VARCHAR NOT NULL
);
CREATE INDEX ammatillisen_tutkinnon_osaalueet_osa_tunniste_idx ON ammatillisen_tutkinnon_osaalueet (osa_tunniste);

CREATE TABLE IF NOT EXISTS opiskeluoikeudet (
    tunniste                UUID PRIMARY KEY,
    versio_tunniste         UUID REFERENCES versiot (tunniste),
    tyyppi                  VARCHAR NOT NULL,
    alkupvm                 DATE,
    loppupvm                DATE,
    tila                    VARCHAR,
    UNIQUE (versio_tunniste, tyyppi)
);
CREATE INDEX opiskeluoikeudet_versio_tunniste_idx ON opiskeluoikeudet (versio_tunniste);
