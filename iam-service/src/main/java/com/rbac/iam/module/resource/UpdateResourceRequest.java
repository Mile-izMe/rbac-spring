package com.rbac.iam.module.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * PUT replaces the whole resource, so a missing parentId, path or icon means "clear it".
 * scopes and dependsOnIds are required: treating a missing list as empty would remove them by accident.
 */
public record UpdateResourceRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        Long parentId,

        @Size(max = 200)
        @Pattern(regexp = "^/.*$", message = "must start with /")
        String path,

        @Size(max = 100)
        String icon,

        @NotNull
        @PositiveOrZero
        Integer sortOrder,

        @NotNull
        Boolean isVisible,

        @NotNull
        Set<@NotNull Scope> scopes,

        @NotNull
        Set<@NotNull Long> dependsOnIds
) {
}
