package fi.oph.suorituspalvelu.resource;

import java.util.Optional;

/**
 * Suoritus domain-luokka (toistaiseksi pelkkä esimerkki)
 */
public class Suoritus {

  public Optional<String> oppijaNumero;
  public Optional<String> suoritus;

  public Suoritus() {}

  public Suoritus(String oppijaNumero, String suoritus) {
    this.oppijaNumero = Optional.of(oppijaNumero);
    this.suoritus = Optional.of(suoritus);
  }
}
