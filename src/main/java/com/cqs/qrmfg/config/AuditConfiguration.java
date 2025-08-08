package com.cqs.qrmfg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.cqs.qrmfg.model.User;

import java.util.Optional;

/**
 * Configuration for JPA and Envers auditing
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfiguration {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    public static class AuditorAwareImpl implements AuditorAware<String> {
        @Override
        public Optional<String> getCurrentAuditor() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth != null && auth.isAuthenticated()) {
                if (auth.getPrincipal() instanceof User) {
                    User user = (User) auth.getPrincipal();
                    return Optional.of(user.getUsername());
                } else if (auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                    return Optional.of(auth.getName());
                }
            }
            
            return Optional.of("SYSTEM");
        }
    }
}