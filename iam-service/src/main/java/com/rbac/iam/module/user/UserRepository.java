package com.rbac.iam.module.user;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<User> findAllByOrderByCreatedAtDescIdDesc(Limit limit);

    @Query("""
            select u from User u
            where u.createdAt < :createdAt
               or (u.createdAt = :createdAt and u.id < :id)
            order by u.createdAt desc, u.id desc
            """)
    List<User> findPageAfter(Instant createdAt, Long id, Limit limit);
}
