package com.rbac.iam.module.resource;

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
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ApiSuccessResponse<List<ResourceResponse>> findAll(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return ApiSuccessResponse.ofCursorPage(resourceService.findPage(cursor, limit), limit, "Resources retrieved");
    }

    @GetMapping("/{id}")
    public ApiSuccessResponse<ResourceResponse> findById(@PathVariable Long id) {
        return ApiSuccessResponse.of(resourceService.findById(id), "Resource retrieved");
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<ResourceResponse>> create(
            @Valid @RequestBody CreateResourceRequest request) {
        ResourceResponse resource = resourceService.create(request);
        return ResponseEntity
                .created(URI.create("/api/resources/" + resource.id()))
                .body(ApiSuccessResponse.of(resource, "Resource created"));
    }

    @PutMapping("/{id}")
    public ApiSuccessResponse<ResourceResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateResourceRequest request) {
        return ApiSuccessResponse.of(resourceService.update(id, request), "Resource updated");
    }

    @DeleteMapping("/{id}")
    public ApiSuccessResponse<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return ApiSuccessResponse.of(null, "Resource deleted");
    }
}
