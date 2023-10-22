package us.calubrecht.lazerwiki.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class LazerwikiDataSourceConfiguration {

    @Bean
    @ConfigurationProperties("lazerwiki.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }



    @Bean("lazerwikiDataSource")
    @Primary
    public DataSource dataSource() {
        return dataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }
}