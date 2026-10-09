package dev.viktorstrohush.orderflow.inventory.domain.model;

/**
 * Códigos estables del motivo por el que una reserva no retiene stock. Viajan en el evento
 * StockResult y order-service los guarda tal cual; el texto lo pone la interfaz en su idioma.
 */
public final class RejectionReason {

    public static final String UNKNOWN_PRODUCT = "UNKNOWN_PRODUCT";
    public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    public static final String CANCELLED_BEFORE_RESERVING = "CANCELLED_BEFORE_RESERVING";
    public static final String RELEASED_BY_CANCELLATION = "RELEASED_BY_CANCELLATION";

    private RejectionReason() {
    }
}
