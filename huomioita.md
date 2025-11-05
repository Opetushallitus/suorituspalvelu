# Kehityksen aikana tulleita huomoita

/api/ui/valintadata palauttaa virheen (500), jos suorituspalvelusta löytyy useita vahvistettuja perusopetuksen oppimääriä.
Tilanne voi tulla esimerkiksi, jos on tallennettu käsin perusopetuksen oppimäärä ja suorituspalvelussa on myös Koskesta ladattu perusopetuksen oppimäärä.
Kyseessä on käytännössä aina virhetilanne, joka täytyy ratkaista ennen kuin esim. opiskelijavalintaan siirtyviä tietoja ("muplautin") voi nähdä tai muokata.

