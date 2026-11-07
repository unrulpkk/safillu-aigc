package cn.iocoder.yudao.module.aigc.config;

import org.junit.jupiter.api.Test;
import org.redisson.config.Config;
import static org.junit.jupiter.api.Assertions.*;

class RedisConnectionConfigurationTest {
    @Test void emptyPasswordsDisableAuthWhileConfiguredPasswordsRemainExact() {
        var customizer=new RedisConnectionConfiguration().optionalRedisPassword();
        for(String password:new String[]{null,""," ","test-only-password"}){
            var config=new Config();
            config.setPassword(password);
            config.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword(password);
            customizer.customize(config);
            assertEquals("".equals(password)?null:password,config.getPassword());
            assertEquals("".equals(password)?null:password,config.useSingleServer().getPassword());
        }
    }
}
