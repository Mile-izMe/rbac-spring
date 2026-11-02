package com.rbac.iam.module.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "must be uppercase letters, digits or underscores, starting with a letter")
        String code,

        @NotBlank
        @Size(max = 100)
        String name
) {
}
