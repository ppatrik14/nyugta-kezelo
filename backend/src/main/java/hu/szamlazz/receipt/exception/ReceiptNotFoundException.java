package hu.szamlazz.receipt.exception;

/**
 * Nem található nyugta – 404-es válaszhoz.
 */
public class ReceiptNotFoundException extends RuntimeException {

    public ReceiptNotFoundException(Long id) {
        super("Nyugta nem található a következő azonosítóval: " + id);
    }
}
