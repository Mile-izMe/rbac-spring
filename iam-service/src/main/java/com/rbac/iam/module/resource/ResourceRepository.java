package com.rbac.iam.module.resource;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    boolean existsByCode(String code);

    boolean existsByParentId(Long parentId);

    List<Resource> findAllByOrderByCreatedAtDescIdDesc(Limit limit);

    @Query("""
            select r from Resource r
            where r.createdAt < :createdAt
               or (r.createdAt = :createdAt and r.id < :id)
            order by r.createdAt desc, r.id desc
            """)
    List<Resource> findPageAfter(Instant createdAt, Long id, Limit limit);

    @Query(value = "select count(*) from iam_role_permission where resource_id = :resourceId", nativeQuery = true)
    long countPermissions(Long resourceId);

    @Query(value = """
            select count(*) from iam_role_permission
            where resource_id = :resourceId and scope_code in (:scopeCodes)
            """, nativeQuery = true)
    long countPermissionsWithScopes(Long resourceId, Collection<String> scopeCodes);

    @Query(value = "select count(*) from iam_resource_dependency where depends_on_id = :resourceId", nativeQuery = true)
    long countDependents(Long resourceId);
}
