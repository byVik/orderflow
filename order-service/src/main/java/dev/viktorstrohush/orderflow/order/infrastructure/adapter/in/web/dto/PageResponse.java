package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web.dto;

import dev.viktorstrohush.orderflow.order.application.port.PageResult;

import java.util.List;

/**
 * One page of a list, as the API returns it. It is a DTO of its own and not Spring Data's Page:
 * that class serializes internal fields and its JSON is not a stable contract.
 */
public record PageResponse<T>(List<T> content, int page, int size, boolean hasNext) {

    public static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(result.content(), result.page(), result.size(), result.hasNext());
    }
}
