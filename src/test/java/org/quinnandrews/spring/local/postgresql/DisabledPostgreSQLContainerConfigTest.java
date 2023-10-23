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
@ActiveProfiles("disabled")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = Application.class)
public class DisabledPostgreSQLContainerConfigTest {

    @Autowired(required = false)
    private PostgreSQLContainer<?> postgreSQLContainer;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private GuitarPedalRepository guitarPedalRepository;

    @Test
    @Order(1)
    void container_notInitialized() {
        // given the application is initialized
        // and the 'disabled' profile is active
        // then the container is not initialized
        assertNull(postgreSQLContainer);
    }

    @Test
    @Order(2)
    void dataSource_initialized() {
        // given the application is initialized
        // and the 'disabled' profile is active
        // and the container is not initialized
        // then the datasource is initialized
        assertNotNull(dataSource);
        final var hikariDataSource = (HikariDataSource) dataSource;
        assertTrue(hikariDataSource.isRunning());
        // and the datasource matches the 'disabled' configuration
        assertEquals("jdbc:h2:mem:testdb", hikariDataSource.getJdbcUrl());
        assertEquals("sa", hikariDataSource.getUsername());
        assertEquals("password", hikariDataSource.getPassword());
        assertEquals("org.h2.Driver", hikariDataSource.getDriverClassName());
    }

    @Test
    @Order(3)
    void guitarPedalRepository_initialized_dataAccessible() {
        // given the application is initialized
        // and the 'disabled' profile is active
        // and the container is not initialized
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
    void sqlInitScript_notExecuted() {
        // given the application is initialized
        // and the 'disabled' profile is active
        // and the container is not initialized
        // and the datasource is initialized
        // and the database user 'sa' was added with 'disabled' configuration via H2
        // and no other user was added, because no initScript was configured
        // and the guitarPedalRepository is initialized
        // then the database contains only 'sa'
        final var users = guitarPedalRepository.getH2Users();
        assertEquals(1, users.size());
        assertEquals("sa", users.get(0));
    }
}
