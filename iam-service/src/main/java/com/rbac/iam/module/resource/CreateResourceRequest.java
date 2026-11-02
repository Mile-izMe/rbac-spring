package com.rbac.iam.module.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateResourceRequest(
        @NotBlank
        @Size(max = 200)
        @Pattern(regexp = "^[a-z0-9]+([-/][a-z0-9]+)*$", message = "must be lowercase words separated by - or /, ex: danh-muc/ki-nang")
        String code,

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        ResourceType type,

        Long parentId,

        @Size(max = 200)
        @Pattern(regexp = "^/.*$", message = "must start with /")
        String path,

        @Size(max = 100)
        String icon,

        @PositiveOrZero
        Integer sortOrder,

        // Optional: when missing, API resources are hidden and the others are visible
        Boolean isVisible,

        Set<@NotNull Scope> scopes,

        Set<@NotNull Long> dependsOnIds
) {

    public CreateResourceRequest {
        scopes = scopes != null ? scopes : Set.of();
        dependsOnIds = dependsOnIds != null ? dependsOnIds : Set.of();
    }
}
