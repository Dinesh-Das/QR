package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.model.UserSession;
import com.cqs.qrmfg.repository.UserSessionRepository;
import com.cqs.qrmfg.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserSessionServiceImpl implements UserSessionService {
    @Autowired
    private UserSessionRepository userSessionRepository;

    @Override
    public UserSession save(UserSession session) {
        return userSessionRepository.save(session);
    }

    @Override
    public UserSession update(UserSession session) {
        return userSessionRepository.save(session);
    }

    @Override
    public void delete(Long id) {
        userSessionRepository.deleteById(id);
    }

    @Override
    public Optional<UserSession> findById(Long id) {
        return userSessionRepository.findById(id);
    }

    @Override
    public Optional<UserSession> findBySessionToken(String sessionToken) {
        return userSessionRepository.findBySessionToken(sessionToken);
    }

    @Override
    public List<UserSession> findAll() {
        return userSessionRepository.findAll();
    }

    @Override
    public List<UserSession> findByUserId(Long userId) {
        // Custom query implementation needed
        return null;
    }
} 