package com.cqs.qrmfg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an invalid material code is provided
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidMaterialCodeException extends ProjectServiceException {

    public InvalidMaterialCodeException(String projectCode, String materialCode) {
        super("Invalid material code: " + materialCode + " for project: " + projectCode, 
              "INVALID_MATERIAL_CODE", "MATERIAL_VALIDATION");
    }

    public InvalidMaterialCodeException(String projectCode, String materialCode, Throwable cause) {
        super("Invalid material code: " + materialCode + " for project: " + projectCode, 
              cause, "INVALID_MATERIAL_CODE", "MATERIAL_VALIDATION");
    }
}