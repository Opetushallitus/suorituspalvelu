CREATE TABLE IF NOT EXISTS siirtotiedostot
(
    id            serial,
    uuid          varchar,
    window_start  TIMESTAMPTZ,
    window_end    TIMESTAMPTZ,
    run_start     TIMESTAMPTZ,
    run_end       TIMESTAMPTZ,
    info          jsonb,   --ainakin tilastot tiedostoihin päätyneistä entiteettimääristä tyypeittäin, esim. {"entityTotals": {"suoritus": 300, "arvosana": 13}}
    success       boolean,
    error_message varchar, -- Tyhjä string, jos mikään ei mennyt vikaan
    PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS siirtotiedosto_id_seq START 1;

COMMENT ON COLUMN siirtotiedostot.window_start IS 'Siirtotiedosto-operaation aikaikkunan (mitkä tiedot mukaan) alkuhetki';
COMMENT ON COLUMN siirtotiedostot.window_end IS 'Siirtotiedosto-operaation aikaikkunan (mitkä tiedot mukaan) loppuhetki';
COMMENT ON COLUMN siirtotiedostot.run_start IS 'Siirtotiedosto-operaation suorituksen alkuhetki';
COMMENT ON COLUMN siirtotiedostot.run_end IS 'Siirtotiedosto-operaation suorituksen alkuhetki';
COMMENT ON COLUMN siirtotiedostot.info IS 'Tietoja tallennetuista entiteeteistä, mm. lukumäärät';
COMMENT ON COLUMN siirtotiedostot.error_message IS 'null, jos mikään ei mennyt vikaan';

INSERT INTO siirtotiedostot(id, uuid, window_start, window_end, run_start, run_end, info, success, error_message)
VALUES (nextval('siirtotiedosto_id_seq'), '57be2612-ba79-429e-a93e-c38346f1d62d',
        TIMESTAMPTZ '2024-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-04-01 00:00:00+00',
        TIMESTAMPTZ '2024-06-26 00:00:00+00',
        TIMESTAMPTZ '2024-06-26 00:00:00+00',
        '{"entityTotals": {}}'::jsonb, true, null)
ON CONFLICT DO NOTHING;
