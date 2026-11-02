package com.rbac.iam.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // --- Common Error System ---
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS-500", "System error not defined!"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "SYS-400", "Input not valid!"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "SYS-401", "Authentication is required"),
    PATH_NOT_FOUND(HttpStatus.NOT_FOUND, "SYS-404", "Requested path not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "SYS-405", "HTTP method not supported for this path"),
    CONFLICT_ERROR(HttpStatus.CONFLICT, "SYS-409", "Data has conflicts!"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "SYS-415", "Content type not supported"),

    // --- Business Error ---
    // --- Auth / user errors
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USR-001", "User not found in system"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USR-002", "Email already existed in system"),
    PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT, "USR-003", "Phone already existed in system"),
    INVALID_VERIFY_TOKEN(HttpStatus.BAD_REQUEST, "USR-004", "Token is invalid"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "USR-005", "Email or password is incorrect"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "USR-006", "Email not verified yet"),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "USR-007", "Invalid access token"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "USR-008", "You do not have permission to this function"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "USR-009", "Invalid refresh token"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "USR-010", "Refresh token has expired"),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USR-011", "Username already existed in system"),

    // --- Role errors
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROL-001", "Role not found in system"),
    ROLE_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "ROL-002", "Role code already existed in system"),
    SYSTEM_ROLE_IMMUTABLE(HttpStatus.CONFLICT, "ROL-003", "System role cannot be modified or deleted"),
    ROLE_IN_USE(HttpStatus.CONFLICT, "ROL-004", "Role is still assigned to users"),
    ROLE_REFERENCE_NOT_FOUND(HttpStatus.BAD_REQUEST, "ROL-005", "One or more roleIds do not exist"),

    // --- Resource errors
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-001", "Resource not found in system"),
    RESOURCE_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "RES-002", "Resource code already existed in system"),
    RESOURCE_REFERENCE_NOT_FOUND(HttpStatus.BAD_REQUEST, "RES-003", "Parent or dependency resource not found"),
    RESOURCE_INVALID_CONFIG(HttpStatus.BAD_REQUEST, "RES-004", "Resource configuration is not valid"),
    RESOURCE_HAS_CHILDREN(HttpStatus.CONFLICT, "RES-005", "Resource still has child resources"),
    RESOURCE_PERMISSION_IN_USE(HttpStatus.CONFLICT, "RES-006", "Resource or scope is still granted to roles"),
    RESOURCE_DEPENDED_ON(HttpStatus.CONFLICT, "RES-007", "Other resources still depend on this resource");

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
