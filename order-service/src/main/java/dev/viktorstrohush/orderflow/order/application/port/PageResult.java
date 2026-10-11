package dev.viktorstrohush.orderflow.order.application.port;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a list. There is no total on purpose: knowing it costs a count over every row,
 * and {@code hasNext} is enough to offer "load more".
 */
public record PageResult<T>(List<T> content, int page, int size, boolean hasNext) {

    public PageResult {
        content = List.copyOf(content);
    }

    /** The same page with each element converted, for instance from domain object to DTO. */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(content.stream().<R>map(mapper).toList(), page, size, hasNext);
    }
}
