package hu.szamlazz.receipt.exception;

import lombok.Getter;

/**
 * Számlázz.hu API hívás során fellépő hiba.
 * Tartalmazza a hibakódot és az üzenetet (ha elérhető).
 */
@Getter
public class SzamlazzApiException extends RuntimeException {

    private final Integer hibakod;

    public SzamlazzApiException(String message) {
        super(message);
        this.hibakod = null;
    }

    public SzamlazzApiException(Integer hibakod, String message) {
        super(message);
        this.hibakod = hibakod;
    }

    public SzamlazzApiException(String message, Throwable cause) {
        super(message, cause);
        this.hibakod = null;
    }
}
