package com.rbac.iam.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rbac.iam.common.response.pagination.CursorPageResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiSuccessResponse<T> {

    @Builder.Default
    private boolean success = true;

    private String message;

    private T data;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private CursorPaginationMeta meta;

    public static <T> ApiSuccessResponse<T> of(T data, String message) {
        return ApiSuccessResponse.<T>builder()
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiSuccessResponse<List<T>> ofCursorPage(
            CursorPageResponse<T> page, int limit, String message) {
        return ApiSuccessResponse.<List<T>>builder()
                .message(message)
                .data(page.items())
                .meta(CursorPaginationMeta.builder()
                        .nextCursor(page.nextCursor())
                        .hasMore(page.hasMore())
                        .limit(limit)
                        .build())
                .build();
    }

}
