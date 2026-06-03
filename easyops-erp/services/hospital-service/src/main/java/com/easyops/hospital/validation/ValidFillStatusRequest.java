package com.easyops.hospital.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = FillStatusUpdateRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFillStatusRequest {

    String message() default "Invalid fill status request";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
