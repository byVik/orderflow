package dev.viktorstrohush.orderflow.order.application.port;

/**
 * Which page of a list is wanted. It is the application's own type: the ports do not use
 * Spring Data's Pageable, so the core does not depend on the persistence technology.
 *
 * @param page zero-based
 */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 50;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
    }
}
