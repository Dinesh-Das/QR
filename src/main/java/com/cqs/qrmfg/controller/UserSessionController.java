package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.UserSession;
import com.cqs.qrmfg.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/sessions")
public class UserSessionController {
    @Autowired
    private UserSessionService userSessionService;

    @GetMapping
    public List<UserSession> getAllSessions() {
        return userSessionService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserSession> getSessionById(@PathVariable Long id) {
        Optional<UserSession> session = userSessionService.findById(id);
        return session.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public List<UserSession> getSessionsByUser(@PathVariable Long userId) {
        return userSessionService.findByUserId(userId);
    }

    @PostMapping
    public UserSession createSession(@RequestBody UserSession session) {
        return userSessionService.save(session);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserSession> updateSession(@PathVariable Long id, @RequestBody UserSession session) {
        session.setId(id);
        return ResponseEntity.ok(userSessionService.update(session));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        userSessionService.delete(id);
        return ResponseEntity.noContent().build();
    }
} 