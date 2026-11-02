package com.rbac.iam.common.response.pagination;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Encodes/decodes an opaque cursor for keyset (cursor) pagination.
 * Cursor payload is "{createdAt}_{id}", base64-encoded so the client
 * cannot easily read or tamper with the raw values.
 * createdAt keeps full precision: cutting it to milliseconds would make
 * the next page repeat or skip rows, because Oracle stores microseconds.
 * <p>
 * Reused by every list endpoint that paginates on (created_at, id).
 */
@Component
public class CursorCodec {

    public record Cursor(
            Instant createdAt,
            Long id
    ) {
    }

    public String encode(Instant createdAt, Long id) {
        String raw = createdAt + "_" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Cursor> decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return Optional.empty();
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("_", 2);
            return Optional.of(new Cursor(Instant.parse(parts[0]), Long.parseLong(parts[1])));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
