## Suorituspalvelu

Suorituspalvelu päivittää oppijoiden suoritustietoja muista järjestelmistä (KOSKI, YTR, VIRTA), jalostaa niitä ja
tarjoilee edelleen muille järjestelmille (Hakemuspalvelu, Valintalaskenta).

### Arkkitehtuuri

Komponenttikuvaus on osoitteessa: https://wiki.eduuni.fi/spaces/KEIJO/pages/574036482/SUPA+Komponenttimalli

### Lokaali ympäristö

Lokaalin ympäristön käyttöönotto

1. Käynnistä lokaali sovellus ajamalla luokka fi.oph.suorituspalvelu.DevApp. Käynnistyksen
   yhteydessä käynnistetään myös postgres-kanta
2. Mene osoitteeseen: https://localhost:8443/swagger (uudelleenohjaa kirjautumiseen hahtuvan cas:iin), kaikkia kutsuja
   pitäisi pystyä kokeilemaan esimerkkiparametreilla
3. Järjestelmän tilaa voi seurata kannasta (salasana on "app"): psql -U app --host localhost --port 55432 -d suorituspalvelu

