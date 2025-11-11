## Suorituspalvelu

Suorituspalvelu päivittää oppijoiden suoritustietoja muista järjestelmistä (KOSKI, YTR, VIRTA), jalostaa niitä ja
tarjoilee edelleen muille järjestelmille (Hakemuspalvelu, Valintalaskenta).

### Arkkitehtuuri

Komponenttikuvaus on osoitteessa: https://wiki.eduuni.fi/spaces/KEIJO/pages/574036482/SUPA+Komponenttimalli

### Lokaali ympäristö

Lokaalin ympäristön käyttöönotto

1. Käynnistä lokaali sovellus ajamalla luokka fi.oph.suorituspalvelu.DevApp. Käynnistyksen
   yhteydessä käynnistetään myös postgres-kanta
2. Mene osoitteeseen: https://localhost:8443/suorituspalvelu/swagger (uudelleenohjaa kirjautumiseen hahtuvan cas:iin), kaikkia kutsuja
   pitäisi pystyä kokeilemaan esimerkkiparametreilla
3. Järjestelmän tilaa voi seurata kannasta (salasana on "app"): psql -U app --host localhost --port 55432 -d suorituspalvelu

#### Palvelimen ja käyttölittymän ajaminen yhdessä

Koko sovelluksen ajaminen yhdessä on toteutettu docker composella.

Docker-compose.sh on kääreskripti, joka mahdollistaa `docker compose`-komennon ajamisen samalla UID:GID-yhdistelmällä kuin host-käyttäjällä, jotta konttien luomien tiedostojen oikeudet ovat vastaavat kuin host-koneella. Voit antaa skriptille samoja komentoriviparametreja kuin `docker compose`-komennolle.

Maven-riippuvuuksia ei asenneta automaattisesti uudelleen joka kerta sovelluksen käynnistyksen yhteydessä, joten asenna Maven-riippuvuudet ensimmäisellä kerralla ja kun riippuvuudet ovat muuttuneet:

```
mvn install -DskipTests
```

Jos riippuvuuksia ei ole tarpeen asentaa uudelleen, sovelluksen voi käynnistää nopeammin komennolla:

```
./docker-compose.sh up
```

Komento käynnistää backendin, käyttöliittymän, postgreSQL-tietokannan ja nginx-proxyn. Ympäristömuuttujat luetaan `.env.docker` ja `.env.docker.local`-tiedostosta. Kopioi itsellesi `.env.docker` tiedosto `.env.docker.local`-tiedostoon ja ylikirjoita haluamasi ympäristömuuttujat (jos esim. haluat ajaa sovellusta jotakin toista ympäristöä vasten).

Sovelluksen käyttöliittymä löytyy käynnistyksen jälkeen osoitteesta http:/localhost/suorituspalvelu.

### Käyttöliittymäkehitys

#### TS-tyyppien generointi

UI-endpointtien käyttämien tyyppien Typescript-vastineet generoidaan ajamalla [TypeScriptGenerator](./suorituspalvelu-service/src/main/scala/fi/oph/suorituspalvelu/ui/TypeScriptGenerator.scala)-objekti tai vaihtoehtoisesti komentoriviltä ajamalla [generate-backend-types.sh](./suorituspalvelu-ui/scripts/generate-backend-types.sh). Tyypit täytyy generoida uudelleen kun UIResponses-luokassa olevia tyyppejä on muutettu.

#### Playwright-testien ajaminen lokaalisti

Playwright-testejä voi ajaa lokaalisti komennolla:

`npx playwright test --ui --project=chromium`

Tätä ennen täytyy käynnistää ui komennolla:

`npm run dev:test`

Komennot ajetaan suorituspalvelu-ui -hakemistossa. Testien käyttämä mock-data löytyy `suorituspalvelu-ui/playwright/fixtures` -hakemistosta.