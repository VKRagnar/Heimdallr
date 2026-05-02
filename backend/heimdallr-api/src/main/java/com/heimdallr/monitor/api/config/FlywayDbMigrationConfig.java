package com.heimdallr.monitor.api.config;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("db")
public class FlywayDbMigrationConfig {
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(migrationLocation())
                .baselineOnMigrate(true)
                .load();
    }

    private static String migrationLocation() {
        for (String candidate : new String[]{"db-migration", "../db-migration", "backend/db-migration"}) {
            if (Files.isDirectory(Path.of(candidate))) {
                return "filesystem:" + candidate;
            }
        }
        return "filesystem:db-migration";
    }
}
