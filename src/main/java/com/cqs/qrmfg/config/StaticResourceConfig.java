package com.cqs.qrmfg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Handle React static files (CSS, JS, etc.)
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/static/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
                .resourceChain(false);
        
        // Disabled - handled by WebConfig.java
        // registry.addResourceHandler("/qrmfg/static/**")
        //         .addResourceLocations("classpath:/static/static/")
        //         .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
        //         .resourceChain(false);
                
        // Explicit handler for JS files to ensure correct MIME type
        registry.addResourceHandler("/qrmfg/static/js/**")
                .addResourceLocations("classpath:/static/static/js/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
                .resourceChain(false);
                
        registry.addResourceHandler("/static/js/**")
                .addResourceLocations("classpath:/static/static/js/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
                .resourceChain(false);
        
        // Handle React public files (manifest.json, favicon.ico, etc.)
        registry.addResourceHandler("/manifest.json")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(false);
        
        // Disabled - handled by WebConfig.java
        // registry.addResourceHandler("/qrmfg/manifest.json")
        //         .addResourceLocations("classpath:/static/")
        //         .setCacheControl(CacheControl.noCache())
        //         .resourceChain(false);
        
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .resourceChain(false);
        
        // Disabled - handled by WebConfig.java
        // registry.addResourceHandler("/qrmfg/favicon.ico")
        //         .addResourceLocations("classpath:/static/")
        //         .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
        //         .resourceChain(false);
        
        // Handle logo files
        registry.addResourceHandler("/logo192.png", "/logo512.png")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .resourceChain(false);
        
        registry.addResourceHandler("/qrmfg/logo192.png", "/qrmfg/logo512.png")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .resourceChain(false);
        
        // Handle robots.txt
        registry.addResourceHandler("/robots.txt")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .resourceChain(false);
        
        registry.addResourceHandler("/qrmfg/robots.txt")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .resourceChain(false);
        
        // Handle asset-manifest.json
        registry.addResourceHandler("/asset-manifest.json")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(false);
        
        registry.addResourceHandler("/qrmfg/asset-manifest.json")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(false);
        

    }
    
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .favorParameter(false)
            .favorPathExtension(false)
            .ignoreAcceptHeader(false)
            .defaultContentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .mediaType("js", org.springframework.http.MediaType.valueOf("application/javascript"))
            .mediaType("css", org.springframework.http.MediaType.valueOf("text/css"))
            .mediaType("json", org.springframework.http.MediaType.APPLICATION_JSON)
            .mediaType("png", org.springframework.http.MediaType.IMAGE_PNG)
            .mediaType("ico", org.springframework.http.MediaType.valueOf("image/x-icon"))
            .mediaType("html", org.springframework.http.MediaType.TEXT_HTML);
    }
}