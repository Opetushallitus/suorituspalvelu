package fi.oph.suorituspalvelu.mankeli.ataru;

import java.util.Optional;

public class AvainArvoDTO {
    private String avain;
    private String arvo;

    public AvainArvoDTO() {
    }

    public AvainArvoDTO(String avain, String arvo) {
        this.avain = avain;
        this.arvo = arvo;
    }

    public String getAvain() {
        return this.avain;
    }

    public void setAvain(String avain) {
        this.avain = avain;
    }

    public String getArvo() {
        return this.arvo;
    }

    public void setArvo(String arvo) {
        this.arvo = arvo;
    }

    public String toString() {
        return "AvainArvoDTO(" + this.avain + ", " + this.arvo + ")";
    }

    public int hashCode() {
        return ((String)Optional.ofNullable(this.avain).orElse("")).hashCode() + 27 * ((String)Optional.ofNullable(this.arvo).orElse("")).hashCode();
    }

    public boolean equals(Object obj) {
        if (obj != null && obj instanceof AvainArvoDTO var2) {
            return ((String)Optional.ofNullable(this.avain).orElse("")).equals(var2.avain) && ((String)Optional.ofNullable(this.arvo).orElse("")).equals(var2.arvo);
        } else {
            return false;
        }
    }
}
