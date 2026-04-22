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

#### PostgreSQL:n lokaali konfiguraatio

Lokaalissa ajossa PostgreSQL:n image, tietokannan nimi, käyttäjä ja salasana luetaan projektin juureen sijoitetusta [`.env`](./.env)-tiedostosta.

Tätä samaa konfiguraatiota käyttävät:

1. `fi.oph.suorituspalvelu.DevApp`
2. testien Testcontainers-PostgreSQL
3. `docker-compose.yml`:n `pgdatabase`-palvelu

Testikoodissa käytettävä `DevDbConfig` sijaitsee vain `suorituspalvelu-shared`-moduulin testikoodissa ja välitetään `suorituspalvelu-service`-moduulin testeille Mavenin `test-jar`-riippuvuutena. Näin konfiguraatio on yhdessä paikassa, mutta ei vuoda tuotantokoodiin.

Jos haluat muuttaa PostgreSQL:n lokaaleja asetuksia, päivitä arvot [`.env`](./.env)-tiedostoon. Näin myös lokaalin PostgreSQL:n version päivityksen voi tehdä vain vaihtamalla imagea .env-tiedostossa.

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

Huom: PostgreSQL-kontin image sekä tietokannan tunnukset luetaan lisäksi projektin juuren [`.env`](./.env)-tiedostosta. `docker-compose.yml` käyttää sitä PostgreSQL-palvelun käynnistämiseen, kun taas `.env.docker` ja `.env.docker.local` sisältävät muun sovelluksen ympäristömuuttujia.

Sovelluksen käyttöliittymä löytyy käynnistyksen jälkeen osoitteesta http:/localhost/suorituspalvelu.

### Käyttöliittymäkehitys

#### TS-tyyppien generointi

UI-endpointtien käyttämien tyyppien Typescript-vastineet generoidaan ajamalla [TypeScriptGenerator](./suorituspalvelu-service/src/main/scala/fi/oph/suorituspalvelu/ui/TypeScriptGenerator.scala)-objekti tai vaihtoehtoisesti komentoriviltä ajamalla [generate-backend-types.sh](./suorituspalvelu-ui/scripts/generate-backend-types.sh). Tyypit täytyy generoida uudelleen kun UIResponses-luokassa olevia tyyppejä on muutettu.

#### Playwright-testien ajaminen lokaalisti

Playwright-testejä voi ajaa lokaalisti komennolla:

`pnpm exec playwright test --ui --project=chromium`

Tätä ennen täytyy käynnistää ui komennolla:

`pnpm run dev:test`

Komennot ajetaan suorituspalvelu-ui -hakemistossa. Testien käyttämä mock-data löytyy `suorituspalvelu-ui/playwright/fixtures` -hakemistosta.
