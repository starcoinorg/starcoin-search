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
        basePackages = "org.starcoin.scan.repository.halley",
        entityManagerFactoryRef = "halleyEntityManagerFactory",
        transactionManagerRef = "halleyTransactionManager"
)
public class HalleyPersistenceConfiguration {
    @Autowired
    private JpaProperties jpaProperties;
    @Autowired
    @Qualifier("halleyDataSource")
    private DataSource halleyDataSource;

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.halley")
    public DataSource halleyDataSource() {
        return DataSourceBuilder.create().build();
    }

    private Map<String, String> getVendorProperties() {
        jpaProperties.setDatabase(Database.POSTGRESQL);
        return jpaProperties.getProperties();
    }

    @Bean(name = "halleyEntityManagerFactoryBuilder")
    public EntityManagerFactoryBuilder entityManagerFactoryBuilder() {
        return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(), new HashMap<>(), null);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean halleyEntityManagerFactory(@Qualifier("halleyEntityManagerFactoryBuilder") EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(halleyDataSource)
                .packages("org.starcoin.bean")
                .persistenceUnit("halleyPersistenceUnit")
                .properties(getVendorProperties())
                .build();
    }

    @Bean
    public PlatformTransactionManager halleyTransactionManager(@Qualifier("halleyEntityManagerFactoryBuilder") EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(halleyEntityManagerFactory(builder).getObject());
    }
}
