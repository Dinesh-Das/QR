package com.cqs.qrmfg.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller to handle React Router routes
 * Serves index.html for all client-side routes that don't match API endpoints
 */
@Controller
public class ReactRouterController {

    /**
     * Handle all React Router routes by serving index.html
     * This ensures that client-side routing works when users navigate directly to URLs
     */
    @GetMapping(value = "/qrmfg/questionnaire/{workflowId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> serveReactApp(@org.springframework.web.bind.annotation.PathVariable Long workflowId) {
        try {
            Resource resource = new ClassPathResource("/static/index.html");
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}