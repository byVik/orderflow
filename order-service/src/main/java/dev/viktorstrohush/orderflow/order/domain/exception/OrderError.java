package dev.viktorstrohush.orderflow.order.domain.exception;

/**
 * Códigos estables de los errores de dominio. Viajan en la respuesta de error para que quien
 * consume la API pueda mostrar el texto en su idioma sin depender del mensaje.
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
