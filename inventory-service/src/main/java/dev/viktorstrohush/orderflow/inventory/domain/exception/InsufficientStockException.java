package dev.viktorstrohush.orderflow.inventory.domain.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String sku, int requested, int available) {
        super("Stock insuficiente de " + sku + ": solicitado " + requested + ", disponible " + available);
    }
}
