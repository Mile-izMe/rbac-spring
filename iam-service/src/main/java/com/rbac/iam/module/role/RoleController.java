package com.rbac.iam.module.role;

import com.rbac.iam.common.response.ApiSuccessResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ApiSuccessResponse<List<RoleResponse>> findAll(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return ApiSuccessResponse.ofCursorPage(roleService.findPage(cursor, limit), limit, "Roles retrieved");
    }

    @GetMapping("/{id}")
    public ApiSuccessResponse<RoleResponse> findById(@PathVariable Long id) {
        return ApiSuccessResponse.of(roleService.findById(id), "Role retrieved");
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<RoleResponse>> create(@Valid @RequestBody CreateRoleRequest request) {
        RoleResponse role = roleService.create(request);
        return ResponseEntity
                .created(URI.create("/api/roles/" + role.id()))
                .body(ApiSuccessResponse.of(role, "Role created"));
    }

    @PutMapping("/{id}")
    public ApiSuccessResponse<RoleResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return ApiSuccessResponse.of(roleService.update(id, request), "Role updated");
    }

    @DeleteMapping("/{id}")
    public ApiSuccessResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiSuccessResponse.of(null, "Role deleted");
    }
}
