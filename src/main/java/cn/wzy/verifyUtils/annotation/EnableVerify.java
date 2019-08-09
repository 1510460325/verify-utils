package cn.wzy.verifyUtils.annotation;

import cn.wzy.verifyUtils.config.VerifyAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author wangzy
 * @version 2019/3/1 11:31
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(VerifyAutoConfiguration.class)
public @interface EnableVerify {

}
