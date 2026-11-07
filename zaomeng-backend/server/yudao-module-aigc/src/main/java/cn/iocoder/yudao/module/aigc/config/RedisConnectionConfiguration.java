package cn.iocoder.yudao.module.aigc.config;

import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Empty environment defaults must not send AUTH to legacy passwordless Redis. */
@Configuration(proxyBeanMethods = false)
public class RedisConnectionConfiguration {
    @Bean
    public RedissonAutoConfigurationCustomizer optionalRedisPassword() {
        return config -> {
            // Redisson 4.x stores Spring Boot credentials on the root config.
            if ("".equals(config.getPassword())) config.setPassword(null);
            if (!config.isSingleConfig()) return;
            var server = config.useSingleServer();
            if ("".equals(server.getPassword())) server.setPassword(null);
        };
    }
}
