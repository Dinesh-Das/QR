package com.cqs.qrmfg.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure role name matches role type
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidRoleTypeValidator.class)
@Documented
public @interface ValidRoleType {
    String message() default "Role name must match the role type";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}