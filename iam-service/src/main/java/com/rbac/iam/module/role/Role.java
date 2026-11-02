package com.rbac.iam.module.role;

import com.rbac.iam.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "iam_role")
@Getter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Role extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "iam_role_seq")
    @SequenceGenerator(name = "iam_role_seq", sequenceName = "iam_role_seq", allocationSize = 50)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Builder.Default
    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // code is not here on purpose: it identifies the role and must stay stable
    public void update(String name, boolean isActive) {
        this.name = name;
        this.isActive = isActive;
    }
}
