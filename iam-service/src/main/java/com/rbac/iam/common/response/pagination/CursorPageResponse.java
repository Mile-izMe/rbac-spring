package com.rbac.iam.common.response.pagination;

import java.util.List;

public record CursorPageResponse<T>(List<T> items, String nextCursor, boolean hasMore) {

    public static <T> CursorPageResponse<T> of(List<T> pagePlusOne, int limit, CursorCodec codec,
                                               java.util.function.Function<T, CursorCodec.Cursor> cursorExtractor) {

        boolean hasMore = pagePlusOne.size() > limit;
        List<T> items = hasMore ? pagePlusOne.subList(0, limit) : pagePlusOne;
        String nextCursor = null;
        if (hasMore && !items.isEmpty()) {
            CursorCodec.Cursor last = cursorExtractor.apply(items.get(items.size() - 1));
            nextCursor = codec.encode(last.createdAt(), last.id());
        }
        return new CursorPageResponse<>(items, nextCursor, hasMore);
    }

    // The cursor is built from the entity, then the items are converted to DTOs
    public <R> CursorPageResponse<R> map(java.util.function.Function<T, R> mapper) {
        return new CursorPageResponse<>(items.stream().map(mapper).toList(), nextCursor, hasMore);
    }

}
