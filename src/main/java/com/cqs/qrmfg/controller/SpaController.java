package com.cqs.qrmfg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller to handle Single Page Application (SPA) routing
 * Forwards all non-API requests to index.html so React Router can handle them
 */
@Controller
public class SpaController {

    /**
     * Forward specific React routes to index.html
     * This allows React Router to handle client-side routing
     * Uses explicit paths to avoid interfering with static resources
     * Note: Context path /qrmfg is automatically added by Spring Boot
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String forwardHome() {
        return "forward:/index.html";
    }
    
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String forwardLogin() {
        return "forward:/index.html";
    }
    
    @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
    public String forwardDashboard() {
        return "forward:/index.html";
    }
    
    @RequestMapping(value = "/admin/**", method = RequestMethod.GET)
    public String forwardAdmin() {
        return "forward:/index.html";
    }
    
    @RequestMapping(value = {
        "/jvc",
        "/cqs", 
        "/tech",
        "/plant",
        "/workflows",
        "/workflow-monitoring",
        "/reports",
        "/users",
        "/roles",
        "/sessions",
        "/user-role-management",
        "/auditlogs",
        "/settings"
    }, method = RequestMethod.GET)
    public String forwardOtherRoutes() {
        return "forward:/index.html";
    }
}