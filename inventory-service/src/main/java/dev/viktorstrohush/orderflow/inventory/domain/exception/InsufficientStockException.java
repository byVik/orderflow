package dev.viktorstrohush.orderflow.inventory.domain.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String sku, int requested, int available) {
        super("Insufficient stock of " + sku + ": requested " + requested + ", available " + available);
    }
}
