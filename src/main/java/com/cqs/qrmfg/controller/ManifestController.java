package com.cqs.qrmfg.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@RestController
public class ManifestController {
    
    private static final Logger logger = LoggerFactory.getLogger(ManifestController.class);

    @GetMapping(value = {"/manifest.json", "/qrmfg/manifest.json"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getManifest() {
        logger.info("ManifestController: Serving manifest.json");
        try {
            Resource resource = new ClassPathResource("static/manifest.json");
            if (resource.exists()) {
                // Java 8 compatible way to read InputStream to String
                String content;
                try (InputStream inputStream = resource.getInputStream();
                     Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                    content = scanner.useDelimiter("\\A").next();
                }
                
                logger.info("ManifestController: Successfully loaded manifest.json with {} characters", content.length());
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setCacheControl("no-cache");
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(content);
            } else {
                logger.warn("ManifestController: manifest.json not found");
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            logger.error("ManifestController: Error reading manifest.json", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}