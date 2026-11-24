-- Spring Boot 4 / Spring Security 7 -päivityksen ja opintopolku-cas-servlet-filterin poiston jälkeen vanhoja
-- serialisoituja sessioita ei voi enää deserialisoida, joten poistetaan ne. Käyttäjät joutuvat kirjautumaan uudelleen.
-- Poisto kaskadoituu tauluihin spring_session_attributes ja cas_client_session.
DELETE FROM spring_session;
