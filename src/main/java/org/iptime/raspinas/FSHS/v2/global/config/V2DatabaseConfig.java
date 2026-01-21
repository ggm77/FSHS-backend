package org.iptime.raspinas.FSHS.v2.global.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "org.iptime.raspinas.FSHS.v2",
        entityManagerFactoryRef = "v2EntityManagerFactory",
        transactionManagerRef = "v2TransactionManager"
)
public class V2DatabaseConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.v2.datasource")
    public DataSource v2DataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean v2EntityManagerFactory(
            final EntityManagerFactoryBuilder builder
    ) {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");

        return builder
                .dataSource(v2DataSource())
                .packages("org.iptime.raspinas.FSHS.v2")
                .persistenceUnit("v2")
                .properties(properties)
                .build();
    }

    @Bean
    public PlatformTransactionManager v2TransactionManager(
            @Qualifier("v2EntityManagerFactory") final EntityManagerFactory v2EntityManagerFactory
    ) {
        return new JpaTransactionManager(v2EntityManagerFactory);
    }
}
