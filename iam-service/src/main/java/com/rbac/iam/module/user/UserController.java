package com.rbac.iam.module.user;

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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiSuccessResponse<List<UserResponse>> findAll(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return ApiSuccessResponse.ofCursorPage(userService.findPage(cursor, limit), limit, "Users retrieved");
    }

    @GetMapping("/{id}")
    public ApiSuccessResponse<UserResponse> findById(@PathVariable Long id) {
        return ApiSuccessResponse.of(userService.findById(id), "User retrieved");
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.create(request);
        return ResponseEntity
                .created(URI.create("/api/users/" + user.id()))
                .body(ApiSuccessResponse.of(user, "User created"));
    }

    @PutMapping("/{id}")
    public ApiSuccessResponse<UserResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiSuccessResponse.of(userService.update(id, request), "User updated");
    }

    @DeleteMapping("/{id}")
    public ApiSuccessResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiSuccessResponse.of(null, "User deleted");
    }
}
