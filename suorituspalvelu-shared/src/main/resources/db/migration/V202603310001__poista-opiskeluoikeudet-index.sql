-- Poistetaan indeksi opiskeluoikeuksien jsonb-dataan, koska indeksi aiheuttaa satunnaista hitautta kantaan tallennuksessa, eikä sitä käytetä koodissa.
DROP INDEX IF EXISTS idx_versiot_opiskeluoikeudet;

