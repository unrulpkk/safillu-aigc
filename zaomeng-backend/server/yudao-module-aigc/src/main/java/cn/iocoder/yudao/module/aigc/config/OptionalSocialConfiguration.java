package cn.iocoder.yudao.module.aigc.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Password-only creator accounts do not require WeChat developer credentials.
 * Keep optional SDK clients uninstantiated while their routes are not exposed. */
@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(prefix="yudao.aigc",name="social-enabled",havingValue="false",matchIfMissing=true)
public class OptionalSocialConfiguration {
    @Bean static BeanFactoryPostProcessor optionalSocialClients(){
        return factory->{for(String name:new String[]{"wxMpService","wxMaService"})
            if(factory.containsBeanDefinition(name))factory.getBeanDefinition(name).setLazyInit(true);};
    }
}
