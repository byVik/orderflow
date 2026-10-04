package dev.viktorstrohush.orderflow.order.application.port.out;

/** El catálogo de productos no está disponible; no es un error de negocio sino de una dependencia. */
public class CatalogUnavailableException extends RuntimeException {
    public CatalogUnavailableException(Throwable cause) {
        super("El catálogo de productos no está disponible", cause);
    }
}
