package dev.viktorstrohush.orderflow.events;

/** Names of the Kafka topics used by the services. */
public final class Topics {
    public static final String ORDER_PLACED = "orders.order-placed.v1";
    public static final String ORDER_CANCELLED = "orders.order-cancelled.v1";
    public static final String STOCK_RESULT = "inventory.stock-result.v1";

    /** Suffix of the dead-letter topic for each consumed topic. */
    public static final String DLT_SUFFIX = "-dlt";

    private Topics() {
    }
}
