package team9499.commitbody.mock;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = CustomContextHolder.class)
public @interface MockUser {

    String loginId() default "test";
    long id() default 1L;
    boolean isWithDrawn() default false;
}
