package com.example.backend.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {
    String action() default "";

    /** Persist to audit_log table when available. */
    boolean persist() default true;

    /**
     * Parameter names that should be masked in args summaries.
     * Matching is case-insensitive.
     */
    String[] maskParams() default {"password", "token", "authorization"};
}

