package dev.viktorstrohush.orderflow.order.domain.exception;

import java.util.Map;

/** Base of the domain errors: a stable code and the data that goes with the message. */
public abstract class OrderException extends RuntimeException {

    private final OrderError code;
    private final Map<String, Object> params;

    protected OrderException(OrderError code, String message, Map<String, Object> params) {
        super(message);
        this.code = code;
        this.params = Map.copyOf(params);
    }

    public OrderError code() { return code; }
    public Map<String, Object> params() { return params; }
}
