package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web;

import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderException;
import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderStateException;
import dev.viktorstrohush.orderflow.order.domain.exception.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Traduce las excepciones de dominio a respuestas RFC 7807 (Problem Details). */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidOrderException.class)
    ProblemDetail invalid(InvalidOrderException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    ProblemDetail conflict(InvalidOrderStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
