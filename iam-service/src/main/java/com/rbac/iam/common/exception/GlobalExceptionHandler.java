package com.rbac.iam.common.exception;


import com.rbac.iam.common.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle Business error actively throw (RbacException)
     */
    @ExceptionHandler(RbacException.class)
    public ResponseEntity<ApiErrorResponse> handleRbacException(RbacException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();

        // Log warning (normally business error only need to log INFO or WARN)
        log.warn("[TraceID: {}] Business Exception: {} - {}", getTraceId(), errorCode.getCode(), ex.getMessage());

        return build(errorCode, ex.getMessage(), null, request);
    }

    /**
     * Handle system error (NullPointerException, SQLException,...)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        ErrorCode defaultError = ErrorCode.INTERNAL_SERVER_ERROR;

        // Log ERROR attach stacktrace to debug
        log.error("[TraceID: {}] Internal Server Error: ", getTraceId(), ex);

        // Don't return real message of Exception (ex.getMessage()) to avoid leaking system information
        return build(defaultError, defaultError.getDefaultMessage(), null, request);
    }

    /**
     * Handle error when user sending missing fields
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();

        log.warn("[TraceID: {}] Validation Error at path {}: {} errors found",
                getTraceId(), request.getRequestURI(), details.size());

        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), details, request);
    }

    /**
     * Handle constraint on controller parameter (@RequestParam, @PathVariable), ex: limit=0
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleParameterValidation(
            HandlerMethodValidationException ex, HttpServletRequest request) {

        List<FieldErrorDetail> details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorDetail(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();

        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), details, request);
    }

    /**
     * Handle parameter with wrong type, ex: /api/roles/abc
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        return build(ErrorCode.VALIDATION_ERROR, "Invalid value for parameter '" + ex.getName() + "'", null, request);
    }

    /**
     * Handle body that cannot be parsed (broken JSON, wrong field type)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return build(ErrorCode.VALIDATION_ERROR, "Request body is malformed", null, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePathNotFound(
            NoResourceFoundException ex, HttpServletRequest request) {

        return build(ErrorCode.PATH_NOT_FOUND, ErrorCode.PATH_NOT_FOUND.getDefaultMessage(), null, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        return build(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getDefaultMessage(), null, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE.getDefaultMessage(), null, request);
    }

    /**
     * Handle denial from method security (@PreAuthorize), which would otherwise become a 500
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        return build(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getDefaultMessage(), null, request);
    }

    /**
     * Handle error DB unique constraint
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("[TraceID: {}] Data integrity violation: {}", getTraceId(), ex.getMessage());

        return build(ErrorCode.CONFLICT_ERROR, ErrorCode.CONFLICT_ERROR.getDefaultMessage(), null, request);
    }

    private ResponseEntity<ApiErrorResponse> build(
            ErrorCode errorCode, String message, List<FieldErrorDetail> details, HttpServletRequest request) {

        ApiErrorResponse response = ApiErrorResponse.of(errorCode, message, details, request.getRequestURI());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    private String getTraceId() {
        return TraceIdFilter.currentTraceId();
    }
}
