package com.tooltrack.tooltrackbackend.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApiException(ApiException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        detail.setTitle(exception.getStatus().getReasonPhrase());
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(400);
        detail.setTitle("Validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("errors", errors);
        return detail;
    }
}
