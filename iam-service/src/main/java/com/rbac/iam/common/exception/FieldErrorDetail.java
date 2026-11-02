package com.rbac.iam.common.exception;

public record FieldErrorDetail(
        String field,
        String message
) {}