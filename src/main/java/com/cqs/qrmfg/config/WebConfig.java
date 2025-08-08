package com.cqs.qrmfg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static resources first (higher priority)
        registry.addResourceHandler("/qrmfg/static/**")
                .addResourceLocations("classpath:/static/static/");
        
        registry.addResourceHandler("/qrmfg/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico");
        
        registry.addResourceHandler("/qrmfg/manifest.json")
                .addResourceLocations("classpath:/static/manifest.json");
        
        // Handle React Router - serve index.html for all non-API, non-static routes
        registry.addResourceHandler("/qrmfg/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // Skip API routes - let them be handled by controllers
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // If the requested resource exists (static files), serve it
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // For all other requests (React routes), serve index.html
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}