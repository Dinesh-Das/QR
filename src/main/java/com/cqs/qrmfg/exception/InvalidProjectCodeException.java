package com.cqs.qrmfg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an invalid project code is provided
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidProjectCodeException extends ProjectServiceException {

    public InvalidProjectCodeException(String projectCode) {
        super("Invalid project code: " + projectCode, "INVALID_PROJECT_CODE", "PROJECT_VALIDATION");
    }

    public InvalidProjectCodeException(String projectCode, Throwable cause) {
        super("Invalid project code: " + projectCode, cause, "INVALID_PROJECT_CODE", "PROJECT_VALIDATION");
    }
}