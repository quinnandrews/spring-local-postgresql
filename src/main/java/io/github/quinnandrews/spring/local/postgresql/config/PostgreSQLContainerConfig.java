package io.github.quinnandrews.spring.local.postgresql.config;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.text.MessageFormat;
import java.util.Optional;

/**
 * <p> Initializes and configures a module from Testcontainers that runs
 * PostgreSQL inside a Docker Container. Requires minimal configuration
 * using Spring conventions, but a variety of optional properties are
 * supported to override default behavior.
 *
 * <p> See the project README for configuration details.
 *
 * @author Quinn Andrews
 */
@ConditionalOnProperty(name="spring.local.postgresql.engaged",
                       havingValue="true",
                       matchIfMissing = true)
@Configuration
public class PostgreSQLContainerConfig {

    /**
     * The Docker Image used by default.
     */
    public static final String POSTGRESQL_DEFAULT_IMAGE = PostgreSQLContainer.IMAGE + ":" + PostgreSQLContainer.DEFAULT_TAG;

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLContainerConfig.class);

    private final String containerImage;
    private final String containerName;
    private final Integer containerPort;
    private final Boolean followContainerLog;
    private final String databaseName;
    private final String username;
    private final String password;
    private final String initScript;
    private final String applicationUsername;
    private final String applicationPassword;

    /**
     * Constructs an instance of this Configuration Class with the given properties.
     *
     * @param containerImage      The Docker Image to use as the Container (optional).
     * @param containerName       The name to use for the Docker Container when started.
     * @param containerPort       The port on the Container that should map to PostgreSQL (optional).
     * @param followContainerLog  Whether to log the output produced by the Container's logs (optional).
     * @param databaseName        The name for the database (optional).
     * @param username            The username for the database super/admin user (optional).
     * @param password            The password for the database super/admin user (optional).
     * @param applicationUsername The username for the database user the Application should connect with,
     *                            if different from the super/admin user (optional).
     * @param applicationPassword The password for the database user the Application should connect with,
     *                            if different from the super/admin user (optional).
     * @param initScript          The path to an SQL script that should be executed when the Container
     *                            starts (optional).
     */
    public PostgreSQLContainerConfig(@Value("${spring.local.postgresql.container.image:#{null}}")
                                     final String containerImage,
                                     @Value("${spring.local.postgresql.container.name:#{null}}")
                                     final String containerName,
                                     @Value("${spring.local.postgresql.container.port:#{null}}")
                                     final Integer containerPort,
                                     @Value("${spring.local.postgresql.container.log.follow:#{false}}")
                                     final Boolean followContainerLog,
                                     @Value("${spring.local.postgresql.database.name:#{null}}")
                                     final String databaseName,
                                     @Value("${spring.local.postgresql.database.username:#{null}}")
                                     final String username,
                                     @Value("${spring.local.postgresql.database.password:#{null}}")
                                     final String password,
                                     @Value("${spring.local.postgresql.database.application.username:#{null}}")
                                     final String applicationUsername,
                                     @Value("${spring.local.postgresql.database.application.password:#{null}}")
                                     final String applicationPassword,
                                     @Value("${spring.local.postgresql.database.init.script:#{null}}")
                                     final String initScript) {
        this.containerImage = containerImage;
        this.containerName = containerName;
        this.containerPort = containerPort;
        this.followContainerLog = followContainerLog;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
        this.applicationUsername = applicationUsername;
        this.applicationPassword = applicationPassword;
        this.initScript = initScript;
    }

    /**
     * Returns a Testcontainers Bean that runs PostgreSQL inside a Docker Container
     * with the given configuration.
     *
     * @return PostgreSQLContainer
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgreSQLContainer() {
        final var container = new PostgreSQLContainer<>(
                DockerImageName.parse(Optional.ofNullable(containerImage)
                        .orElse(POSTGRESQL_DEFAULT_IMAGE))
        );
        Optional.ofNullable(containerPort).ifPresent(cp ->
                container.withCreateContainerCmdModifier(cmd -> cmd
                        .withName(containerName)
                        .withHostConfig(
                                new HostConfig().withPortBindings(
                                        new PortBinding(
                                                Ports.Binding.bindPort(cp),
                                                new ExposedPort(PostgreSQLContainer.POSTGRESQL_PORT))
                                ))));
        if (followContainerLog) {
            container.withLogConsumer(new Slf4jLogConsumer(logger));
        }
        Optional.ofNullable(databaseName).ifPresent(container::withDatabaseName);
        Optional.ofNullable(username).ifPresent(container::withUsername);
        Optional.ofNullable(password).ifPresent(container::withPassword);
        Optional.ofNullable(initScript).ifPresent(container::withInitScript);
        container.start();
        logger.info(MessageFormat.format("""
                      
                      
                        *************************************************************************************
                        |+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|
                        
                            Running PostgreSQLContainer for development and testing.
                        
                            Container: {0}
                            Image: {1}
                            Port Mapping: {2}:{3}
                        
                            Use the credentials below to connect with your client of choice (DBeaver,
                            IntelliJ IDEA, etc.):
                        
                            JDBC URL: {4}
                            Admin User Username: {5}
                            Admin User Password: {6}
                            Application User Username: {7}
                            Application User Password: {8}
                        
                        |+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|+|
                        *************************************************************************************
                        """,
                container.getContainerName(),
                container.getDockerImageName(),
                String.valueOf(container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)),
                String.valueOf(PostgreSQLContainer.POSTGRESQL_PORT),
                container.getJdbcUrl(),
                container.getUsername(),
                container.getPassword(),
                Optional.ofNullable(applicationUsername)
                        .orElse("[not configured]"),
                Optional.ofNullable(applicationPassword)
                        .orElse("[not configured]")));
        return container;
    }

    /**
     * Initializes a Spring Bean connecting the Application to the PostgreSQLContainer.
     *
     * @param applicationUsername The username for the database user the Application should connect with,
     *                            if different from the super/admin user (optional).
     * @param applicationPassword The password for the database user the Application should connect with,
     *                            if different from the super/admin user (optional).
     * @param container The instance of PostgreSQLContainer to build the DataSource with.
     * @return DataSource
     */
    @Bean
    public JdbcConnectionDetails jdbcConnectionDetails(@Value("${spring.local.postgresql.database.application.username:#{null}}")
                                                       final String applicationUsername,
                                                       @Value("${spring.local.postgresql.database.application.password:#{null}}")
                                                       final String applicationPassword,
                                                       final PostgreSQLContainer<?> container) {
        return new LocalPostgreSQLConnectionDetails(applicationUsername, applicationPassword, container);
    }


    public static class LocalPostgreSQLConnectionDetails implements JdbcConnectionDetails {

        private final String applicationUsername;
        private final String applicationPassword;
        private final PostgreSQLContainer<?> container;

        public LocalPostgreSQLConnectionDetails(final String applicationUsername,
                                                final String applicationPassword,
                                                final PostgreSQLContainer<?> container) {
            this.applicationUsername = applicationUsername;
            this.applicationPassword = applicationPassword;
            this.container = container;
        }

        @Override
        public String getUsername() {
            return Optional.ofNullable(applicationUsername)
                    .orElse(container.getUsername());
        }

        @Override
        public String getPassword() {
            return Optional.ofNullable(applicationPassword)
                    .orElse(container.getPassword());
        }

        @Override
        public String getJdbcUrl() {
            return container.getJdbcUrl();
        }

        @Override
        public String getDriverClassName() {
            return container.getDriverClassName();
        }
    }
}
