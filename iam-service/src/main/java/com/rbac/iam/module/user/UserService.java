package com.rbac.iam.module.user;

import com.rbac.iam.common.exception.ErrorCode;
import com.rbac.iam.common.exception.RbacException;
import com.rbac.iam.common.response.pagination.CursorCodec;
import com.rbac.iam.common.response.pagination.CursorPageResponse;
import com.rbac.iam.module.role.Role;
import com.rbac.iam.module.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final int BCRYPT_MAX_BYTES = 72;

    // Not a bean yet: Spring Boot would then stop adding {noop} to its generated HTTP Basic password,
    // and that login would fail. Move it to SecurityConfig when real login replaces the generated user.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CursorCodec cursorCodec;

    public CursorPageResponse<UserResponse> findPage(String cursor, int limit) {
        // One extra row tells whether a next page exists
        List<User> rows = fetchRows(cursor, Limit.of(limit + 1));

        return CursorPageResponse
                .of(rows, limit, cursorCodec, user -> new CursorCodec.Cursor(user.getCreatedAt(), user.getId()))
                .map(UserResponse::from);
    }

    public UserResponse findById(Long id) {
        return UserResponse.from(getUser(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByUsername(request.username())) {
            throw new RbacException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RbacException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .username(request.username())
                .passwordHash(hashPassword(request.password()))
                .fullName(request.fullName())
                .email(email)
                .roles(resolveRoles(request.roleIds()))
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = getUser(id);
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new RbacException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        user.update(request.fullName(), email, request.isActive(), resolveRoles(request.roleIds()));
        if (request.password() != null) {
            user.changePasswordHash(hashPassword(request.password()));
        }

        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(getUser(id));
    }

    private List<User> fetchRows(String cursor, Limit limit) {
        if (cursor == null || cursor.isBlank()) {
            return userRepository.findAllByOrderByCreatedAtDescIdDesc(limit);
        }

        CursorCodec.Cursor decoded = cursorCodec.decode(cursor)
                .orElseThrow(() -> new RbacException(ErrorCode.VALIDATION_ERROR, "Invalid cursor"));

        return userRepository.findPageAfter(decoded.createdAt(), decoded.id(), limit);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RbacException(ErrorCode.USER_NOT_FOUND));
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new RbacException(ErrorCode.ROLE_NOT_FOUND);
        }
        return new HashSet<>(roles);
    }

    // The unique constraint is case-sensitive, so A@x.com and a@x.com must be stored the same way
    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private String hashPassword(String password) {
        // @Size counts characters, but BCrypt's limit is in bytes and Vietnamese letters take several
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new RbacException(ErrorCode.VALIDATION_ERROR, "Password must not exceed 72 bytes");
        }
        return passwordEncoder.encode(password);
    }
}
