package com.rbac.iam.module.role;

public record RoleResponse (
       Long id,
       String code,
       String name,
       boolean isSystem,
       boolean isActive
) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.isSystem(),
                role.isActive()
        );
    }
}
