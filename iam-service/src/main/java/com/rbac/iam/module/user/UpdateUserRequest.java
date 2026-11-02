package com.rbac.iam.module.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @NotBlank
        @Size(max = 70)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        // Boolean, not boolean: a missing value must fail validation instead of becoming false
        @NotNull
        Boolean isActive,

        // Required: this replaces the user's roles, so a missing field must not remove them all
        @NotNull
        @Size(max = 100)
        Set<@NotNull Long> roleIds,

        // Optional: null keeps the current password
        @Size(min = 8, max = 72)
        String password
) {
}
