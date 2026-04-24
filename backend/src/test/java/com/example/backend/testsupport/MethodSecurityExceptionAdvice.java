package com.example.backend.testsupport;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * WebMvc slice tests with {@code @PreAuthorize} and {@code addFilters = false} do not
 * install default access-denied handling; map to 403 for permission assertions.
 */
@RestControllerAdvice
public class MethodSecurityExceptionAdvice {

    @ExceptionHandler(AuthorizationDeniedException.class)
    ResponseEntity<Void> accessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
