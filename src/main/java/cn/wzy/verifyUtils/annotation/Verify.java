package cn.wzy.verifyUtils.annotation;

import java.lang.annotation.*;

/**
 * @author wangzy
 * @version 2019/3/1 11:31
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Verify {

    String[] notNull() default {};

    String[] sizeLimit() default {};

    String[] numberLimit() default {};

    String[] regex() default {};
}
