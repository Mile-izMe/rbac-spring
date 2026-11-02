package com.rbac.iam.common.exception;

import lombok.Getter;

@Getter
public class RbacException extends RuntimeException {

    // Whenever business logic is wrong (ex: out of desk, wrong password)
    // Throw this class

    private final ErrorCode errorCode;

    public RbacException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public RbacException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}