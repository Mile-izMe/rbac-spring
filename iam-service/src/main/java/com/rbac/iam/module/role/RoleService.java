package com.rbac.iam.module.role;

import com.rbac.iam.common.exception.ErrorCode;
import com.rbac.iam.common.exception.RbacException;
import com.rbac.iam.common.response.pagination.CursorCodec;
import com.rbac.iam.common.response.pagination.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final CursorCodec cursorCodec;

    public CursorPageResponse<RoleResponse> findPage(String cursor, int limit) {
        // One extra row tells whether a next page exists
        List<Role> rows = fetchRows(cursor, Limit.of(limit + 1));

        return CursorPageResponse
                .of(rows, limit, cursorCodec, role -> new CursorCodec.Cursor(role.getCreatedAt(), role.getId()))
                .map(RoleResponse::from);
    }

    public RoleResponse findById(Long id) {
        return RoleResponse.from(getRole(id));
    }

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        if (roleRepository.existsByCode(request.code())) {
            throw new RbacException(ErrorCode.ROLE_CODE_ALREADY_EXISTS);
        }

        Role role = Role.builder()
                .code(request.code())
                .name(request.name())
                .build();

        return RoleResponse.from(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse update(Long id, UpdateRoleRequest request) {
        Role role = getRole(id);
        requireNotSystemRole(role);

        role.update(request.name(), request.isActive());

        return RoleResponse.from(role);
    }

    @Transactional
    public void delete(Long id) {
        Role role = getRole(id);
        requireNotSystemRole(role);

        // The FK is ON DELETE CASCADE, so without this check users would silently lose the role
        if (roleRepository.countAssignedUsers(id) > 0) {
            throw new RbacException(ErrorCode.ROLE_IN_USE);
        }

        roleRepository.delete(role);
    }

    private List<Role> fetchRows(String cursor, Limit limit) {
        if (cursor == null || cursor.isBlank()) {
            return roleRepository.findAllByOrderByCreatedAtDescIdDesc(limit);
        }

        CursorCodec.Cursor decoded = cursorCodec.decode(cursor)
                .orElseThrow(() -> new RbacException(ErrorCode.VALIDATION_ERROR, "Invalid cursor"));

        return roleRepository.findPageAfter(decoded.createdAt(), decoded.id(), limit);
    }

    private Role getRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RbacException(ErrorCode.ROLE_NOT_FOUND));
    }

    private void requireNotSystemRole(Role role) {
        if (role.isSystem()) {
            throw new RbacException(ErrorCode.SYSTEM_ROLE_IMMUTABLE);
        }
    }
}
