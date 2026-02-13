CREATE TABLE IF NOT EXISTS harkinnanvaraisuus_yliajot (
    id                       SERIAL PRIMARY KEY,
    virkailija_oid           VARCHAR,
    hakemus_oid              VARCHAR,
    hakukohde_oid            VARCHAR,
    harkinnanvaraisuuden_syy VARCHAR,
    selite                   VARCHAR,
    voimassaolo              TSTZRANGE NOT NULL,
    EXCLUDE USING gist (hakemus_oid WITH =, hakukohde_oid WITH =, voimassaolo WITH &&)
);

CREATE INDEX IF NOT EXISTS harkinnanvaraisuus_yliajot_hakemus_idx ON harkinnanvaraisuus_yliajot (hakemus_oid);
