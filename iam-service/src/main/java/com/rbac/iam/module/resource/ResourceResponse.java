package com.rbac.iam.module.resource;

import java.util.List;

public record ResourceResponse(
        Long id,
        String code,
        String name,
        ResourceType type,
        Long parentId,
        String path,
        String icon,
        int sortOrder,
        boolean isVisible,
        List<Scope> scopes,
        List<Long> dependsOnIds
) {

    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getCode(),
                resource.getName(),
                resource.getType(),
                resource.getParent() != null ? resource.getParent().getId() : null,
                resource.getPath(),
                resource.getIcon(),
                resource.getSortOrder(),
                resource.isVisible(),
                resource.getScopes().stream().sorted().toList(),
                resource.getDependsOn().stream().map(Resource::getId).sorted().toList()
        );
    }
}
