package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.UserSession;
import com.cqs.qrmfg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findBySessionToken(String sessionToken);
    List<UserSession> findByUser(User user);
} 