package dev.viktorstrohush.orderflow.order.domain.exception;

import java.util.Map;

public class InvalidOrderException extends OrderException {
    public InvalidOrderException(OrderError code, String message) {
        this(code, message, Map.of());
    }

    public InvalidOrderException(OrderError code, String message, Map<String, Object> params) {
        super(code, message, params);
    }
}
