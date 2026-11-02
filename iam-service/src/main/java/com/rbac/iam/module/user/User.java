package com.rbac.iam.module.user;

import com.rbac.iam.common.entity.Auditable;
import com.rbac.iam.module.role.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "iam_user")
@Getter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "iam_user_seq")
    @SequenceGenerator(name = "iam_user_seq", sequenceName = "iam_user_seq", allocationSize = 50)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 70)
    private String fullName;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // BatchSize instead of a fetch join: a list page loads all its roles in one query,
    // and a collection fetch join would make Hibernate apply the page limit in memory
    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "iam_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @BatchSize(size = 100)
    private Set<Role> roles = new HashSet<>();

    // username is not here on purpose: it identifies the user and must stay stable
    public void update(String fullName, String email, boolean isActive, Set<Role> roles) {
        this.fullName = fullName;
        this.email = email;
        this.isActive = isActive;
        this.roles.clear();
        this.roles.addAll(roles);
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
