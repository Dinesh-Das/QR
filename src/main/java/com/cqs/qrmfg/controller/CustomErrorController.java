package com.cqs.qrmfg.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        Map<String, Object> errorDetails = new HashMap<>();
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            // For 404 errors on non-API routes, forward to SPA
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                if (requestUri != null && !requestUri.startsWith("/qrmfg/api/") && !requestUri.startsWith("/api/")) {
                    // This is a frontend route, return a redirect instruction
                    errorDetails.put("redirect", "/index.html");
                    errorDetails.put("status", 200);
                    return ResponseEntity.ok(errorDetails);
                }
            }
            
            errorDetails.put("status", statusCode);
            errorDetails.put("error", getErrorMessage(statusCode));
            errorDetails.put("message", "An error occurred while processing the request");
            errorDetails.put("path", requestUri);
            errorDetails.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.status(statusCode).body(errorDetails);
        }
        
        errorDetails.put("status", 500);
        errorDetails.put("error", "Internal Server Error");
        errorDetails.put("message", "An error occurred while processing the request");
        errorDetails.put("path", requestUri);
        errorDetails.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.status(500).body(errorDetails);
    }
    

    
    private String getErrorMessage(int statusCode) {
        switch (statusCode) {
            case 400:
                return "Bad Request";
            case 401:
                return "Unauthorized";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 500:
                return "Internal Server Error";
            default:
                return "Unknown Error";
        }
    }
}