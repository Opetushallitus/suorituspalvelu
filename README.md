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

#### Palvelimen ja käyttölittymän ajaminen yhdessä

Koko sovelluksen ajaminen yhdessä on toteutettu docker composella. Voit käynnistää koko sovelluksen yhdessä komennolla

./docker-compose.sh up

Komento käynnistää backendin, käyttöliittymän, postgreSQL-tietokannan ja nginx-proxyn. Ympäristömuuttujat luetaan .env.docker ja .env.docker.local-tiedostosta. Kopioi itsellesi .env.docker tiedosts .env.docker.local-tiedostoon ja ylikirjoita haluamasi ympäristömuuttujat (jos esim. haluat ajaa sovellusta jotakin toista ympäristöä vasten).

docker-compose.sh on kääreskripti, joka asettaa ympäristömuuttujia ja ajaa komennon "docker compose". Voit antaa sille samoja komentoriviparametreja kuin "docker compose":lle. 

Sovelluksen käyttöliittymä on käytettävissä osoitteessa http:/localhost/suorituspalvelu. 