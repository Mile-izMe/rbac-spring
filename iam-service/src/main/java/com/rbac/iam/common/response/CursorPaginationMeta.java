package com.rbac.iam.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CursorPaginationMeta {

    private String nextCursor;
    private boolean hasMore;
    private int limit;
}