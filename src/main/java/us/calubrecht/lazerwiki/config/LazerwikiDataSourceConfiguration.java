package us.calubrecht.lazerwiki.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class LazerwikiDataSourceConfiguration {

    @Autowired
    Environment env;

    @Bean
    @ConfigurationProperties("lazerwiki.datasource")
    @ConditionalOnProperty(prefix = "lazerwiki.db", name = "engine", havingValue="mysql")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConditionalOnProperty(prefix = "lazerwiki.db", name = "engine", havingValue="sqlite")
    @Primary
    public DataSource dataSourceSqlLite() {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("lazerwiki.datasource.driverClassName"));
        dataSource.setUrl(env.getProperty("lazerwiki.datasource.url"));
        dataSource.setUsername(env.getProperty("lazerwiki.datasource.username"));
        dataSource.setPassword(env.getProperty("lazerwiki.datasource.password"));
        return dataSource;
    }



    @Bean("lazerwikiDataSource")
    @Primary
    @ConditionalOnProperty(prefix = "lazerwiki.db", name = "engine", havingValue="mysql")
    public DataSource dataSource() {
        return dataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }
}