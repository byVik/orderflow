package dev.viktorstrohush.orderflow.order.application.port.out;

/** The product catalog is unavailable; it is not a business error but a dependency failure. */
public class CatalogUnavailableException extends RuntimeException {
    public CatalogUnavailableException(Throwable cause) {
        super("The product catalog is unavailable", cause);
    }
}
