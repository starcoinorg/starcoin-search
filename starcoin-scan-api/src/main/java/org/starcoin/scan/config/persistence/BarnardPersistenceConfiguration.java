package org.starcoin.scan.config.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@PropertySource({"classpath:application.properties"})
@EnableJpaRepositories(
        basePackages = "org.starcoin.scan.repository.barnard",
        entityManagerFactoryRef = "barnardEntityManagerFactory",
        transactionManagerRef = "barnardTransactionManager"
)
public class BarnardPersistenceConfiguration {
    @Autowired
    private JpaProperties jpaProperties;
    @Autowired
    @Qualifier("barnardDataSource")
    private DataSource barnardDataSource;

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.barnard")
    public DataSource barnardDataSource() {
        return DataSourceBuilder.create().build();
    }

    private Map<String, String> getVendorProperties() {
        jpaProperties.setDatabase(Database.POSTGRESQL);
        return jpaProperties.getProperties();
    }

    @Bean(name = "barnardEntityManagerFactoryBuilder")
    public EntityManagerFactoryBuilder entityManagerFactoryBuilder() {
        return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(), new HashMap<>(), null);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean barnardEntityManagerFactory(@Qualifier("barnardEntityManagerFactoryBuilder") EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(barnardDataSource)
                .packages("org.starcoin.bean")
                .persistenceUnit("barnardPersistenceUnit")
                .properties(getVendorProperties())
                .build();
    }

    @Bean
    public PlatformTransactionManager barnardTransactionManager(@Qualifier("barnardEntityManagerFactoryBuilder") EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(barnardEntityManagerFactory(builder).getObject());
    }
}
