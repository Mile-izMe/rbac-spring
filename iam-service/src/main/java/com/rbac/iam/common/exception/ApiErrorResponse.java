package com.rbac.iam.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rbac.iam.common.web.TraceIdFilter;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String errorCode;
    private String message;
    private List<FieldErrorDetail> details;
    private String path;
    private String traceId;

    public static ApiErrorResponse of(ErrorCode errorCode, String message, List<FieldErrorDetail> details, String path) {
        return ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.getStatus().getReasonPhrase())
                .errorCode(errorCode.getCode())
                .message(message)
                .details(details)
                .path(path)
                .traceId(TraceIdFilter.currentTraceId())
                .build();
    }
}
