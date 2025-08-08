package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.UserSession;
import java.util.List;
import java.util.Optional;

public interface UserSessionService {
    UserSession save(UserSession session);
    UserSession update(UserSession session);
    void delete(Long id);
    Optional<UserSession> findById(Long id);
    Optional<UserSession> findBySessionToken(String sessionToken);
    List<UserSession> findAll();
    List<UserSession> findByUserId(Long userId);
} 