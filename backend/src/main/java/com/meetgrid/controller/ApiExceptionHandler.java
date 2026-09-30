package com.meetgrid.controller;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ProblemDetail staleChange(Exception ex) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "This record changed elsewhere. Reload before saving again."); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex) {
        var error = ex.getBindingResult().getFieldErrors().stream().findFirst();
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.map(e -> e.getField() + ": " + e.getDefaultMessage()).orElse("Invalid request."));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadable(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request. Use Monday–Sunday and times in HH:mm format.");
    }
}
