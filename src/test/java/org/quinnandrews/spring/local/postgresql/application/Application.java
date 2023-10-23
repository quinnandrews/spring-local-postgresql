package org.quinnandrews.spring.local.postgresql.application;

import org.quinnandrews.spring.local.postgresql.config.EnableLocalPostgreSQL;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableLocalPostgreSQL
@SpringBootApplication
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
