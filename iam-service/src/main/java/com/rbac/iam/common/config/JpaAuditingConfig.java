package com.rbac.iam.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Fills createdAt/updatedAt in Auditable.
 * createdBy/updatedBy stay null until CustomUserDetails exists; then switch to the draft below.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}

// Draft for when CustomUserDetails exists:
//package com.rbac.iam.common.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.domain.AuditorAware;
//import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//import java.security.Security;
//import java.util.Optional;
//
//@Configuration
//@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
//public class JpaAuditingConfig {
//
//    /**
//     * Resolves the "current user" for created_by/updated_by columns.
//     * Returns empty for anonymous/system actions (e.g. Flyway seed data,
//     * scheduler jobs) so those columns stay NULL instead of throwing.
//     */
//    @Bean
//    public AuditorAware<Long> auditorProvider() {
//        return () -> {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            if (authentication == null || !authentication.isAuthenticated()
//                || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
//                return Optional.empty();
//            }
//            return Optional.ofNullable(principal.getUserId);
//        };
//    }
//}
