package dev.viktorstrohush.orderflow.order.domain.exception;

/**
 * Stable codes for the domain errors. They travel in the error response so that whoever
 * consumes the API can show the text in their own language without relying on the message.
 */
public enum OrderError {
    ORDER_NOT_FOUND,
    ORDER_NOT_PENDING,
    CUSTOMER_REQUIRED,
    ORDER_EMPTY,
    DUPLICATE_SKU,
    SKU_REQUIRED,
    QUANTITY_NOT_POSITIVE,
    QUANTITY_ABOVE_MAX,
    NEGATIVE_PRICE,
    UNKNOWN_PRODUCT
}
