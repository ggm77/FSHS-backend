package org.iptime.raspinas.FSHS.v1.global.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "org.iptime.raspinas.FSHS.v1",
        entityManagerFactoryRef = "v1EntityManagerFactory",
        transactionManagerRef = "v1TransactionManager"
)
public class V1DatabaseConfig {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.v1.datasource")
    public DataSource v1DataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean v1EntityManagerFactory(EntityManagerFactoryBuilder builder) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");
        properties.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");

        properties.put("hibernate.implicit_naming_strategy",
                "org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl");

        return builder
                .dataSource(v1DataSource())
                .packages("org.iptime.raspinas.FSHS.v1")
                .persistenceUnit("v1")
                .properties(properties)
                .build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager v1TransactionManager(
            @Qualifier("v1EntityManagerFactory")EntityManagerFactory v1EntityManagerFactory
    ) {
        return new JpaTransactionManager(v1EntityManagerFactory);
    }
}
