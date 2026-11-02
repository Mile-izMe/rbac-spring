package com.rbac.iam.module.resource;

import com.rbac.iam.common.exception.ErrorCode;
import com.rbac.iam.common.exception.RbacException;
import com.rbac.iam.common.response.pagination.CursorCodec;
import com.rbac.iam.common.response.pagination.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final CursorCodec cursorCodec;

    public CursorPageResponse<ResourceResponse> findPage(String cursor, int limit) {
        // One extra row tells whether a next page exists
        List<Resource> rows = fetchRows(cursor, Limit.of(limit + 1));

        return CursorPageResponse
                .of(rows, limit, cursorCodec, resource -> new CursorCodec.Cursor(resource.getCreatedAt(), resource.getId()))
                .map(ResourceResponse::from);
    }

    public ResourceResponse findById(Long id) {
        return ResourceResponse.from(getResource(id));
    }

    @Transactional
    public ResourceResponse create(CreateResourceRequest request) {
        if (resourceRepository.existsByCode(request.code())) {
            throw new RbacException(ErrorCode.RESOURCE_CODE_ALREADY_EXISTS);
        }

        Resource parent = getParent(request.parentId());
        Set<Resource> dependsOn = getDependencies(request.dependsOnIds());
        boolean isVisible = request.isVisible() != null ? request.isVisible() : request.type() != ResourceType.API;

        validateConfig(request.type(), request.path(), parent, isVisible, request.scopes(), dependsOn);

        Resource resource = Resource.builder()
                .code(request.code())
                .name(request.name())
                .type(request.type())
                .parent(parent)
                .path(request.path())
                .icon(request.icon())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 999)
                .isVisible(isVisible)
                .scopes(new HashSet<>(request.scopes()))
                .dependsOn(dependsOn)
                .build();

        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, UpdateResourceRequest request) {
        Resource resource = getResource(id);
        Resource parent = getParent(request.parentId());
        Set<Resource> dependsOn = getDependencies(request.dependsOnIds());

        validateConfig(resource.getType(), request.path(), parent, request.isVisible(), request.scopes(), dependsOn);
        requireValidConfig(!dependsOn.contains(resource), "A resource cannot depend on itself");
        requireNotInsideItself(resource, parent);
        requireRemovedScopesUnused(resource, request.scopes());

        resource.update(request.name(), parent, request.path(), request.icon(), request.sortOrder(),
                request.isVisible(), request.scopes(), dependsOn);

        return ResourceResponse.from(resource);
    }

    @Transactional
    public void delete(Long id) {
        Resource resource = getResource(id);

        if (resourceRepository.existsByParentId(id)) {
            throw new RbacException(ErrorCode.RESOURCE_HAS_CHILDREN);
        }
        // These FKs are ON DELETE CASCADE, so without these checks roles and other resources
        // would silently lose what they were given
        if (resourceRepository.countPermissions(id) > 0) {
            throw new RbacException(ErrorCode.RESOURCE_PERMISSION_IN_USE);
        }
        if (resourceRepository.countDependents(id) > 0) {
            throw new RbacException(ErrorCode.RESOURCE_DEPENDED_ON);
        }

        resourceRepository.delete(resource);
    }

    private List<Resource> fetchRows(String cursor, Limit limit) {
        if (cursor == null || cursor.isBlank()) {
            return resourceRepository.findAllByOrderByCreatedAtDescIdDesc(limit);
        }

        CursorCodec.Cursor decoded = cursorCodec.decode(cursor)
                .orElseThrow(() -> new RbacException(ErrorCode.VALIDATION_ERROR, "Invalid cursor"));

        return resourceRepository.findPageAfter(decoded.createdAt(), decoded.id(), limit);
    }

    private Resource getResource(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new RbacException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private Resource getParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return resourceRepository.findById(parentId)
                .orElseThrow(() -> new RbacException(ErrorCode.RESOURCE_REFERENCE_NOT_FOUND));
    }

    private Set<Resource> getDependencies(Set<Long> ids) {
        List<Resource> found = resourceRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new RbacException(ErrorCode.RESOURCE_REFERENCE_NOT_FOUND);
        }
        return new HashSet<>(found);
    }

    private void validateConfig(ResourceType type, String path, Resource parent, boolean isVisible,
                                Set<Scope> scopes, Set<Resource> dependsOn) {
        requireValidConfig((type == ResourceType.MENU) == (path != null),
                "path is required for MENU and not allowed for GROUP or API");
        requireValidConfig((type == ResourceType.GROUP) == scopes.isEmpty(),
                "GROUP cannot have scopes, MENU and API need at least one");
        requireValidConfig(type != ResourceType.API || (parent == null && !isVisible),
                "API is not part of the menu, so it cannot have a parent or be visible");
        requireValidConfig(parent == null || parent.getType() == ResourceType.GROUP,
                "parent must be a GROUP");

        // A dependency means "VIEW on this resource also gives VIEW on that one", so both sides need VIEW
        requireValidConfig(dependsOn.isEmpty() || scopes.contains(Scope.VIEW),
                "dependsOnIds requires the VIEW scope");
        requireValidConfig(dependsOn.stream().allMatch(dependency -> dependency.getScopes().contains(Scope.VIEW)),
                "every dependency must support the VIEW scope");
    }

    private void requireNotInsideItself(Resource resource, Resource parent) {
        for (Resource ancestor = parent; ancestor != null; ancestor = ancestor.getParent()) {
            requireValidConfig(!ancestor.getId().equals(resource.getId()),
                    "A resource cannot be moved under itself or its own children");
        }
    }

    private void requireRemovedScopesUnused(Resource resource, Set<Scope> newScopes) {
        Set<Scope> removed = new HashSet<>(resource.getScopes());
        removed.removeAll(newScopes);
        if (removed.isEmpty()) {
            return;
        }

        List<String> removedCodes = removed.stream().map(Scope::name).toList();
        if (resourceRepository.countPermissionsWithScopes(resource.getId(), removedCodes) > 0) {
            throw new RbacException(ErrorCode.RESOURCE_PERMISSION_IN_USE);
        }
        if (removed.contains(Scope.VIEW) && resourceRepository.countDependents(resource.getId()) > 0) {
            throw new RbacException(ErrorCode.RESOURCE_DEPENDED_ON);
        }
    }

    private void requireValidConfig(boolean valid, String message) {
        if (!valid) {
            throw new RbacException(ErrorCode.RESOURCE_INVALID_CONFIG, message);
        }
    }
}
