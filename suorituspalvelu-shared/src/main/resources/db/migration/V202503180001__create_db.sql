CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TYPE lahde AS ENUM ('KOSKI', 'YTR', 'VIRTA', 'VIRKAILIJA');

-- taulu lisätty jotta voidaa lukita yksittäinen oppija
CREATE TABLE IF NOT EXISTS oppijat (
    oppijanumero                VARCHAR PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS versiot (
    tunniste                    UUID PRIMARY KEY,
    use_versio_tunniste         UUID REFERENCES versiot (tunniste),
    oppijanumero                VARCHAR NOT NULL REFERENCES oppijat (oppijanumero),
    voimassaolo                 TSTZRANGE NOT NULL,
    lahde                       lahde NOT NULL,
    data_json                   JSONB,
    data_xml                    XML,
    data_parseroitu             JSONB,
    EXCLUDE USING gist (oppijanumero WITH =, lahde WITH =, voimassaolo WITH &&),
    CHECK ((lahde='KOSKI'       AND data_json IS NOT NULL   AND data_xml IS NULL) OR
           (lahde='YTR'         AND data_json IS NOT NULL   AND data_xml IS NULL) OR
           (lahde='VIRTA'       AND data_json IS NULL       AND data_xml IS NOT NULL) OR
           (lahde='VIRKAILIJA'  AND data_json IS NOT NULL   AND data_xml IS NULL)),
    CHECK ((use_versio_tunniste IS NOT NULL AND data_parseroitu IS NULL) OR
           (use_versio_tunniste IS NULL AND data_parseroitu IS NOT NULL))
);

CREATE INDEX idx_versiot_data_parseroitu ON versiot USING gin (data_parseroitu jsonb_path_ops);

CREATE OR REPLACE FUNCTION get_tyyppi(polku text, tyyppi text)
RETURNS TABLE (
  versio_tunniste UUID,
  data jsonb
) AS $$
select versiot.tunniste, itemdata from versiot, jsonb_path_query(versiot.data_parseroitu, format('%s ? (@.type==$tyyppi)', polku)::jsonpath, jsonb_build_object('tyyppi', tyyppi)) as itemdata where versiot.data_parseroitu @@ format('%s.type=="%s"', polku, tyyppi)::jsonpath;
$$ LANGUAGE SQL STABLE;
