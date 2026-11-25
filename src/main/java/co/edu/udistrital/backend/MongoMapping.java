package co.edu.udistrital.backend;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@Configuration(proxyBeanMethods = false)
class MongoMapping {
    @Bean
    org.springframework.data.mongodb.MongoTransactionManager transactionManager(
            org.springframework.data.mongodb.MongoDatabaseFactory factory) {
        return new org.springframework.data.mongodb.MongoTransactionManager(factory);
    }
    @Bean
    static BeanPostProcessor mongoTypeMapping() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof MappingMongoConverter converter) {
                    converter.setTypeMapper(new DefaultMongoTypeMapper(null));
                }
                return bean;
            }
        };
    }
}
