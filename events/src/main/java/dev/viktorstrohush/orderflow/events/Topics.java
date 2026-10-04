package dev.viktorstrohush.orderflow.events;

/** Nombres de los topics de Kafka usados por los servicios. */
public final class Topics {
    public static final String ORDER_PLACED = "orders.order-placed.v1";
    public static final String ORDER_CANCELLED = "orders.order-cancelled.v1";
    public static final String STOCK_RESULT = "inventory.stock-result.v1";

    /** Sufijo del dead-letter topic de cada topic consumido. */
    public static final String DLT_SUFFIX = "-dlt";

    private Topics() {
    }
}
