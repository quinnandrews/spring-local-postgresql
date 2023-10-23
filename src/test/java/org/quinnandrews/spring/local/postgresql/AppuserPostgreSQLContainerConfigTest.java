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
@ActiveProfiles("appuser")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = Application.class)
public class AppuserPostgreSQLContainerConfigTest {

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
        // and the 'appuser' profile is active
        // and the container is initialized
        assertNotNull(postgreSQLContainer);
        assertTrue(postgreSQLContainer.isRunning());
        // then the container matches the 'appuser' configuration
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
        // and the 'appuser' profile is active
        // and the container is initialized
        // then the datasource is initialized
        assertNotNull(dataSource);
        final var hikariDataSource = (HikariDataSource) dataSource;
        assertTrue(hikariDataSource.isRunning());
        // and the datasource jdbcUrl and driver match the container
        assertEquals(postgreSQLContainer.getJdbcUrl(), hikariDataSource.getJdbcUrl());
        assertEquals(postgreSQLContainer.getDriverClassName(), hikariDataSource.getDriverClassName());
        // but the username and password do not match the container
        assertNotEquals(postgreSQLContainer.getUsername(), hikariDataSource.getUsername());
        assertNotEquals(postgreSQLContainer.getPassword(), hikariDataSource.getPassword());
        // because the username and password match the configured application user instead
        assertEquals("overdrive", hikariDataSource.getUsername());
        assertEquals("reverb", hikariDataSource.getPassword());
    }

    @Test
    @Order(3)
    void guitarPedalRepository_initialized_dataAccessible() {
        // given the application is initialized
        // and the 'appuser' profile is active
        // and the container is initialized
        // and the datasource is initialized
        // and three pedals were inserted when data.sql was executed
        // and the database user 'overdrive' was added with the 'appuser' initScript via PostgreSQLConfig
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
}
