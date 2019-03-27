package io.github.resilience4j.bulkhead.annotation;

import io.github.resilience4j.recovery.DefaultRecoveryFunction;
import io.github.resilience4j.recovery.RecoveryFunction;

import java.lang.annotation.*;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface Bulkhead {
    /**
     * Name of the bulkhead.
     *
     * @return the name of the bulkhead
     */
    String name();

    Class<? extends RecoveryFunction> recovery() default DefaultRecoveryFunction.class;
}
