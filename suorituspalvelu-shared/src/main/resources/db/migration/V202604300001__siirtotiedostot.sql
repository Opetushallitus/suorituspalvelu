CREATE TABLE IF NOT EXISTS siirtotiedostot
(
    id            INTEGER,
    uuid          VARCHAR,
    window_start  TIMESTAMPTZ NOT NULL,
    window_end    TIMESTAMPTZ,
    run_start     TIMESTAMPTZ,
    run_end       TIMESTAMPTZ,
    paivittaiset  BOOLEAN, --Osa tiedostoista muodostetaan vain kerran päivässä, osa jokaisella ajastetulla käynnistyksellä.
    info          JSONB,   --ainakin tilastot tiedostoihin päätyneistä entiteettimääristä tyypeittäin, esim. {"entityTotals": {"suoritus": 300, "arvosana": 13}}
    success       BOOLEAN,
    error_message VARCHAR, -- Tyhjä string, jos mikään ei mennyt vikaan
    PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS siirtotiedosto_id_seq START 1;

COMMENT ON COLUMN siirtotiedostot.window_start IS 'Siirtotiedosto-operaation aikaikkunan (mitkä tiedot mukaan) alkuhetki';
COMMENT ON COLUMN siirtotiedostot.window_end IS 'Siirtotiedosto-operaation aikaikkunan (mitkä tiedot mukaan) loppuhetki';
COMMENT ON COLUMN siirtotiedostot.run_start IS 'Siirtotiedosto-operaation suorituksen alkuhetki';
COMMENT ON COLUMN siirtotiedostot.run_end IS 'Siirtotiedosto-operaation suorituksen loppuhetki';
COMMENT ON COLUMN siirtotiedostot.info IS 'Tietoja tallennetuista entiteeteistä, mm. lukumäärät';
COMMENT ON COLUMN siirtotiedostot.error_message IS 'null, jos mikään ei mennyt vikaan';

--Alustetaan taulu niin, että koko historiaa ei käydä läpi ensimmäisellä käynnistyskerralla.
INSERT INTO siirtotiedostot(id, uuid, window_start, window_end, run_start, run_end, paivittaiset, info, success, error_message)
VALUES (nextval('siirtotiedosto_id_seq'), '57be2612-ba79-429e-a93e-c38346f1d62d',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-05-01 00:00:00+00',
        TIMESTAMPTZ '2024-06-26 00:00:00+00',
        TIMESTAMPTZ '2024-06-26 00:00:00+00',
        false,
        '{"entityTotals": {}}'::jsonb, true, null)
ON CONFLICT DO NOTHING;
