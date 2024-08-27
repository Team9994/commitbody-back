package team9499.commitbody.global.config;

import org.hibernate.boot.model.FunctionContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateConfig{
    @Bean
    public FunctionContributor customFunctionContributor() {
        return new CustomMySQLDialect();
    }
}
