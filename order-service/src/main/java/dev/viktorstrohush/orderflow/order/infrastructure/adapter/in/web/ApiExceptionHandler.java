package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web;

import dev.viktorstrohush.orderflow.order.application.port.out.CatalogUnavailableException;
import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderException;
import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderStateException;
import dev.viktorstrohush.orderflow.order.domain.exception.OrderException;
import dev.viktorstrohush.orderflow.order.domain.exception.OrderNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Translates exceptions into RFC 7807 (Problem Details) responses. Each response carries a
 * stable {@code code} (and {@code params} if the message has data): the client translates by
 * the code and uses {@code detail}, in English, only as a fallback.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    static final String CODE = "code";
    static final String PARAMS = "params";

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(InvalidOrderException.class)
    ProblemDetail invalid(InvalidOrderException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    ProblemDetail conflict(InvalidOrderStateException ex) {
        return problem(HttpStatus.CONFLICT, ex);
    }

    /** Two simultaneous changes to the same order: cancelling just as the stock result arrives. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concurrentUpdate(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
                "The order changed while the request was being processed. Reload it and try again.", Map.of());
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    ProblemDetail catalogUnavailable(CatalogUnavailableException ex) {
        logger.warn("Catalog unavailable", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_UNAVAILABLE",
                "The catalog is not available right now. Try again in a few seconds.", Map.of());
    }

    /** Replaces the generic "Invalid request content." with the fields that fail. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        String fields = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ex.getBody();
        problem.setDetail("Invalid request. " + fields);
        problem.setProperty(CODE, "VALIDATION_FAILED");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /**
     * The same for constraints on the method parameters themselves ({@code @Min} on a
     * {@code @RequestParam}): Spring reports those with a different exception than a {@code @Valid} body.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        String parameters = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName() + ": " + error.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ex.getBody();
        problem.setDetail("Invalid request. " + parameters);
        problem.setProperty(CODE, "VALIDATION_FAILED");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, OrderException ex) {
        return problem(status, ex.code().name(), ex.getMessage(), ex.params());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail, Map<String, Object> params) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(CODE, code);
        if (!params.isEmpty()) {
            problem.setProperty(PARAMS, params);
        }
        return problem;
    }
}
