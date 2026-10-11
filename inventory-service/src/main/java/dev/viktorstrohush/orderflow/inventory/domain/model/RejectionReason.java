package dev.viktorstrohush.orderflow.inventory.domain.model;

/**
 * Stable codes for why a reservation holds no stock. They travel in the StockResult event
 * and order-service stores them as is; the UI supplies the text in its own language.
 */
public final class RejectionReason {

    public static final String UNKNOWN_PRODUCT = "UNKNOWN_PRODUCT";
    public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    public static final String CANCELLED_BEFORE_RESERVING = "CANCELLED_BEFORE_RESERVING";
    public static final String RELEASED_BY_CANCELLATION = "RELEASED_BY_CANCELLATION";

    private RejectionReason() {
    }
}
