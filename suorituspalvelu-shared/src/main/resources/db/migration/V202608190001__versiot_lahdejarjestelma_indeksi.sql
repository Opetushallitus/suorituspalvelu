-- Uudelleenparserointi hakee versiot lähdejärjestelmän perusteella tunnisteen mukaisessa järjestyksessä erissä.
-- Ilman indeksiä tämä on seq scan koko versiot-taulun yli, mikä kaataa haun timeouttiin.
CREATE INDEX IF NOT EXISTS idx_versiot_lahdejarjestelma_tunniste ON versiot(lahdejarjestelma, tunniste);
