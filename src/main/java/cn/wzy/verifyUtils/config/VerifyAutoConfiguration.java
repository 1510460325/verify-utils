package cn.wzy.verifyUtils.config;

import cn.wzy.verifyUtils.Check.VerifyAspect;
import org.springframework.context.annotation.Bean;

/**
 * @author WangZY
 * @since  2019/6/6 10:42
 * @version 1.0
 **/
public class VerifyAutoConfiguration {

    @Bean
    public VerifyAspect getVerifyAspect() {
        return new VerifyAspect();
    }
}
