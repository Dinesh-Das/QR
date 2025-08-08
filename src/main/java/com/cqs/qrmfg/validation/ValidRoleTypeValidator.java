package com.cqs.qrmfg.validation;

import com.cqs.qrmfg.model.Role;
import com.cqs.qrmfg.enums.RoleType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator to ensure role name matches role type
 */
public class ValidRoleTypeValidator implements ConstraintValidator<ValidRoleType, Role> {

    @Override
    public void initialize(ValidRoleType constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Role role, ConstraintValidatorContext context) {
        if (role == null) {
            return true; // Let @NotNull handle null validation
        }

        // If either name or roleType is null, let other validators handle it
        if (role.getName() == null || role.getRoleType() == null) {
            return true;
        }

        // Check if the role name matches the role type
        boolean isValid = role.getRoleType().getRoleName().equals(role.getName());
        
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Role name '%s' does not match role type '%s'", 
                    role.getName(), role.getRoleType().getRoleName()))
                .addConstraintViolation();
        }
        
        return isValid;
    }
}