package com.rbac.iam.module.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-z0-9._-]+$", message = "must be lowercase letters, digits, dots, underscores or hyphens")
        String username,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @NotBlank
        @Size(max = 70)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        // Optional: a user can start without any role
        @Size(max = 100)
        Set<@NotNull Long> roleIds
) {
}
