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
    hakuoid                     VARCHAR,
    data_json                   JSONB,
    data_xml                    XML,
    virkailija_salli_overlap    VARCHAR GENERATED ALWAYS AS (CASE
                                       WHEN lahde='VIRKAILIJA' THEN tunniste::text
                                       ELSE oppijanumero
                                     END) STORED,
    EXCLUDE USING gist (virkailija_salli_overlap WITH =, oppijanumero WITH =, lahde WITH =, voimassaolo WITH &&),
    CHECK ((lahde='KOSKI'       AND data_json IS NOT NULL   AND data_xml IS NULL) OR
           (lahde='YTR'         AND data_json IS NOT NULL   AND data_xml IS NULL) OR
           (lahde='VIRTA'       AND data_json IS NULL       AND data_xml IS NOT NULL) OR
           (lahde='VIRKAILIJA'  AND data_json IS NOT NULL   AND data_xml IS NULL)),
    CHECK (lahde='VIRKAILIJA' OR hakuoid IS NULL)
);

COMMENT ON COLUMN versiot.virkailija_salli_overlap is 'EXCLUDE rajoite käyttää tätä saraketta jotta voidaan sallia päällekkäisiä voimassaoloaikoja virkailijoiden syöttämille tiedoille';

CREATE TABLE IF NOT EXISTS perusopetuksen_oppimaarat (
    tunniste                UUID PRIMARY KEY,
    versio_tunniste         UUID REFERENCES versiot (tunniste) ON DELETE CASCADE,
    vahvistuspaivamaara     DATE
);

CREATE TABLE IF NOT EXISTS perusopetuksen_oppiaineet (
    oppimaara_tunniste      UUID REFERENCES perusopetuksen_oppimaarat (tunniste) ON DELETE CASCADE,
    nimi                    VARCHAR NOT NULL,
    koodi                   VARCHAR NOT NULL,
    arvosana                VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS ammatilliset_tutkinnot (
    tunniste                UUID PRIMARY KEY,
    versio_tunniste         UUID REFERENCES versiot (tunniste) ON DELETE CASCADE,
    nimi                    VARCHAR NOT NULL,
    koodi                   VARCHAR NOT NULL,
    vahvistuspaivamaara     DATE
);

CREATE TABLE IF NOT EXISTS ammatillisen_tutkinnon_osat (
    tutkinto_tunniste       UUID REFERENCES ammatilliset_tutkinnot (tunniste) ON DELETE CASCADE,
    nimi                    VARCHAR NOT NULL,
    koodi                   VARCHAR NOT NULL,
    arvosana                VARCHAR
);

CREATE TABLE IF NOT EXISTS opiskeluoikeudet (
    tunniste                UUID PRIMARY KEY,
    versio_tunniste         UUID REFERENCES versiot (tunniste),
    tyyppi                  VARCHAR NOT NULL,
    alkupvm                 DATE,
    loppupvm                DATE,
    tila                    VARCHAR,
    UNIQUE (versio_tunniste, tyyppi)
);

-- halutaanko sallia sama suoritus eri kohdissa hierarkiaa
-- haluataanko sallia useampi virkailijan tekemä korjaussetti (esim. eri voimassaoloajat, hakuspesifi ja yleinen jne.)
-- miten mätchätään virkailijan tekemä korjaus allaolevaan suoritukseen? Pelkällä koodiarvolla vai koodiarvolla ja paikalla hierarkiassa?
-- käytetäänkö samaa vai eri tietomallia lähdejärjestelmistä tuleville ja virkailijan syöttämille suorituksille?

-- suorituksiin oppilaitos

-- toimiiko yksi malli kaikille suorituksille?

-- ilmeisesti kannattaisi esittää suoritukset ja opiskeluoikeudet erikseen?

-- miten tunnistetaan ja merkataan että hakija ei enää ole aktiivinen


-- kun päivitetään lähdejärjestelmästä tuleva suoritus, poistetaan ensin järjestelmässä olevat suoritukset/opiskeluoikeudet