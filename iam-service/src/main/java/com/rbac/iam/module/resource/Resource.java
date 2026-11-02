package com.rbac.iam.module.resource;

import com.rbac.iam.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "iam_resource")
@Getter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Resource extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "iam_resource_seq")
    @SequenceGenerator(name = "iam_resource_seq", sequenceName = "iam_resource_seq", allocationSize = 50)
    private Long id;

    @Column(name = "code", nullable = false, length = 200)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Resource parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 10)
    private ResourceType type;

    @Column(name = "path", length = 200)
    private String path;

    @Column(name = "icon", length = 100)
    private String icon;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 999;

    @Builder.Default
    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;

    // BatchSize: a page of resources loads these in one query instead of one query per resource
    @Builder.Default
    @BatchSize(size = 100)
    @ElementCollection
    @CollectionTable(name = "iam_resource_scope", joinColumns = @JoinColumn(name = "resource_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_code", nullable = false, length = 10)
    private Set<Scope> scopes = new HashSet<>();

    @Builder.Default
    @BatchSize(size = 100)
    @ManyToMany
    @JoinTable(
            name = "iam_resource_dependency",
            joinColumns = @JoinColumn(name = "resource_id"),
            inverseJoinColumns = @JoinColumn(name = "depends_on_id"))
    private Set<Resource> dependsOn = new HashSet<>();

    // code and type are not here on purpose: code identifies the resource,
    // and changing type would break the rules its children and permissions rely on
    public void update(String name, Resource parent, String path, String icon, int sortOrder, boolean isVisible,
                       Set<Scope> scopes, Set<Resource> dependsOn) {
        this.name = name;
        this.parent = parent;
        this.path = path;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.isVisible = isVisible;

        // Change the sets in place. Assigning new sets makes Hibernate delete and re-insert every scope row,
        // and the ON DELETE CASCADE on iam_role_permission would wipe the permissions granted to roles.
        this.scopes.retainAll(scopes);
        this.scopes.addAll(scopes);
        this.dependsOn.retainAll(dependsOn);
        this.dependsOn.addAll(dependsOn);
    }
}
