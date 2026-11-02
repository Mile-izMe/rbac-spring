package com.rbac.iam.module.auth;

import com.rbac.iam.common.exception.ApiErrorResponse;
import com.rbac.iam.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Security filters run before the controllers, so @RestControllerAdvice cannot see their errors.
 * This writes 401/403 in the same format as every other error.
 */
@Component
@RequiredArgsConstructor
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        write(ErrorCode.UNAUTHORIZED, request, response);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(ErrorCode.ACCESS_DENIED, request, response);
    }

    private void write(ErrorCode errorCode, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ApiErrorResponse body = ApiErrorResponse.of(
                errorCode, errorCode.getDefaultMessage(), null, request.getRequestURI());

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
