package com.rbac.iam.module.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        // Boolean, not boolean: a missing value must fail validation instead of becoming false
        @NotNull
        Boolean isActive
) {
}
