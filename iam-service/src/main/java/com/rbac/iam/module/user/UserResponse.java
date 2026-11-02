package com.rbac.iam.module.user;

import com.rbac.iam.module.role.RoleResponse;

import java.util.Comparator;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        boolean isActive,
        List<RoleResponse> roles
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.isActive(),
                user.getRoles().stream()
                        .map(RoleResponse::from)
                        .sorted(Comparator.comparing(RoleResponse::code))
                        .toList()
        );
    }
}
