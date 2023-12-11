package org.quinnandrews.spring.local.postgresql;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.quinnandrews.spring.local.postgresql.application.Application;
import org.quinnandrews.spring.local.postgresql.application.data.guitarpedals.repository.GuitarPedalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext
@ActiveProfiles("custom")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = Application.class)
public class CustomPostgreSQLContainerConfigTest {

    @Autowired(required = false)
    private PostgreSQLContainer<?> postgreSQLContainer;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private GuitarPedalRepository guitarPedalRepository;

    @Test
    @Order(1)
    void container_initialized() {
        // given the application is initialized
        // and the 'custom' profile is active
        // and the container is initialized
        assertNotNull(postgreSQLContainer);
        assertTrue(postgreSQLContainer.isRunning());
        // then the container matches the 'custom' configuration
        assertEquals("postgres:15", postgreSQLContainer.getDockerImageName());
        assertEquals(15432, postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        assertEquals("pedals", postgreSQLContainer.getDatabaseName());
        assertEquals("fuzz", postgreSQLContainer.getUsername());
        assertEquals("echo", postgreSQLContainer.getPassword());
    }

    @Test
    @Order(2)
    void dataSource_initialized() {
        // given the application is initialized
        // and the 'custom' profile is active
        // and the container is initialized
        // then the datasource is initialized
        assertNotNull(dataSource);
        final var hikariDataSource = (HikariDataSource) dataSource;
        assertTrue(hikariDataSource.isRunning());
        // and the datasource matches the container
        assertEquals(postgreSQLContainer.getJdbcUrl(), hikariDataSource.getJdbcUrl());
        assertEquals(postgreSQLContainer.getUsername(), hikariDataSource.getUsername());
        assertEquals(postgreSQLContainer.getPassword(), hikariDataSource.getPassword());
        assertEquals(postgreSQLContainer.getDriverClassName(), hikariDataSource.getDriverClassName());
    }

    @Test
    @Order(3)
    void guitarPedalRepository_initialized_dataAccessible() {
        // given the application is initialized
        // and the 'custom' profile is active
        // and the container is initialized
        // and the datasource is initialized
        // and three pedals were inserted when data.sql was executed
        // then the guitarPedalRepository is initialized
        assertNotNull(guitarPedalRepository);
        // and the database contains the three pedals
        assertEquals(3, guitarPedalRepository.count());
        final var pedals = guitarPedalRepository.findAll(Sort.by("name"));
        assertEquals(3L, pedals.get(0).getId());
        assertEquals("Catalinbread Soft Focus Reverb", pedals.get(0).getName());
        assertEquals(1L, pedals.get(1).getId());
        assertEquals("Electro-Harmonix Big Muff Fuzz", pedals.get(1).getName());
        assertEquals(2L, pedals.get(2).getId());
        assertEquals("Strymon Deco: Tape Saturation and Double Tracker", pedals.get(2).getName());
    }

    @Test
    @Order(4)
    void sqlInitScript_executed() {
        // given the application is initialized
        // and the 'custom' profile is active
        // and the container is initialized
        // and the datasource is initialized
        // and the database user 'fuzz' was added with 'custom' configuration via PostgreSQLConfig
        // and the database user 'overdrive' was added with the 'custom' initScript via PostgreSQLConfig
        // and the guitarPedalRepository is initialized
        // then the database contains both 'fuzz' and 'overdrive'
        final var users = guitarPedalRepository.getPostgreSQLUsers();
        assertEquals(2, users.size());
        assertEquals("fuzz", users.get(0));
        assertEquals("overdrive", users.get(1));
    }
}
