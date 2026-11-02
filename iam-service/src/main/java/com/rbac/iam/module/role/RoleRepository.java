package com.rbac.iam.module.role;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByCode(String code);

    List<Role> findAllByOrderByCreatedAtDescIdDesc(Limit limit);

    @Query("""
            select r from Role r
            where r.createdAt < :createdAt
               or (r.createdAt = :createdAt and r.id < :id)
            order by r.createdAt desc, r.id desc
            """)
    List<Role> findPageAfter(Instant createdAt, Long id, Limit limit);

    @Query(value = "select count(*) from iam_user_role where role_id = :roleId", nativeQuery = true)
    long countAssignedUsers(Long roleId);
}
