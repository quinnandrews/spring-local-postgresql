# Spring Local PostgreSQL

## Description
Decorates the Testcontainers PostgreSQL module with enhanced configuration for Spring Boot Applications. 

Requires minimal configuration using Spring conventions, but a variety of optional properties are provided to override default behavior by profile, supporting local development in addition to test execution.

## Features
- Configure whether the Testcontainers PostgreSQL module is active or not. Allows you to control its activation by profile.
- Configure the Docker Image to use with the Testcontainers PostgreSQL module. Allows you to match the PostgreSQL version used in local and test environments with the version in production.
- Configure the Testcontainers PostgreSQL module to run with a fixed container name. Useful for local development so that developers can easily find the running container.
- Configure the Testcontainers PostgreSQL module to run with a fixed port. Useful for local development so that developers can connect using their JDBC client of choice with consistent, predictable configuration.
- Configure whether to follow the Docker Container's log output. Useful for troubleshooting in some cases.
- Configure the database name to match production.
- Configure a database admin user to handle migration scripts and a second "application user" with restricted privileges, which the Application will use after migration is completed. 
- Configure an SQL script to run when the database in the container starts up.

## Rationale
When developing an Application that uses PostgreSQL in production, an embedded PostgreSQL server provides the benefits of using an in-memory database, like H2, but avoids the downsides. The database is spun up and torn down when the Application starts up and shuts down, but developers are able to utilize PostgreSQL features (that alternatives like H2 may not support) while test and local environments better resemble production. Development and testing become more effective and reliable.

While Testcontainers is designed exclusively to support Integration Tests, this project is designed to support running the Application locally as well, reducing the overhead that would come with maintaining Testcontainers in addition to some other solution that fundamentally does the same thing. There is no need to maintain a local PostgreSQL server nor additional Docker configuration inside or outside the project.

## Requirements
### Java 17 
https://adoptium.net/temurin/releases/?version=17

### Docker
https://www.docker.com/products/docker-desktop/ <br>
https://rancherdesktop.io/

NOTE: Rancher Desktop may not work correctly if Docker Desktop had been previously installed.

### PostgreSQL JDBC Driver
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Spring Boot Starter JDBC or Spring Boot Start Data JPA
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

## Transitive Dependencies
- Spring Boot Starter Web 3.2.0
- Spring Boot Configuration Processor 3.2.0
- Spring Boot Starter Test 3.2.0
- Spring Boot Testcontainers 3.2.0
- Testcontainers PostgreSQL 1.19.3

## Usage
### Configuration as a Test Dependency
While it is possible to declare `spring-local-postgresql` as a compile dependency, and control its activation with profiles, it is better practice to declare it as a test dependency. 

This means, however, that all configuration for `spring-local-postgresql` (for both Integration Tests *and* for running the Application locally) can only reside in your project's test source. For Integration Tests this is common practice, but for running the Application locally it may seem unusual or perhaps difficult to do. However, by implementing the approach surfaced in this [article](https://bsideup.github.io/posts/local_development_with_testcontainers/) by Sergei Egorov, configuring a local profile in your project's test source becomes a simple process that will likely become a preferred practice as well.  

### Add Spring Local PostgreSQL
Add the `spring-local-postgresql` artifact to your project as a test dependency:
```xml
<dependency>
    <groupId>io.github.quinnandrews</groupId>
    <artifactId>spring-local-postgresql</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```
(NOTE: The `spring-local-postgresql` artifact is NOT yet available in Maven Central, but is available from GitHub Packages, which requires additional configuration in your pom.xml file.)

### Configure a Local Profile
Create a properties files in your test resources directory to configuration for a `local` profile. 

Configure the `local` profile with a fixed container name, so that developers can quickly and consistently identify the running container.

Configure the `local` profile with a fixed port, so that developers can connect to the database with a consistent and predictable port (otherwise, their client's configuration will be invalidated every time the Application starts).

Set other configuration properties as desired, or not at all to use default settings. 

application-local.properties:
```properties
spring.local.postgresql.container.image=postgres:15
spring.local.postgresql.container.name=local_postgresql
spring.local.postgresql.container.port=15432
spring.local.postgresql.database.name=pedals
spring.local.postgresql.database.username=fuzz_local
spring.local.postgresql.database.password=flange_local
spring.local.postgresql.database.application.username=overdrive_local
spring.local.postgresql.database.application.password=reverb_local
spring.local.postgresql.database.init.script=data/init-local.sql
```

In the example above, in addition to a fixed container name and port, the `local` profile has the following settings:
- The `postgres:15` Docker Image is set to use a more recent version of PostgreSQL than the default (version 9) to match  production.
- The `pedals` database name is set to match production, providing consistency for developers.
- The `fuzz_local/flange_local` username/password pair is set as the admin user for migrations (using tools like Flyway) to match production behavior, but can also be used by developers with their client of choice.
- The `overdrive_local/reverb_local` username/password pair is set as the application user to match production behavior, a user with limited privileges that the Application will use when executing queries with JPA and Hibernate. Developers can connect their client of choice with these credentials as well, to be consistent with the Application.  
- The `data/init.sql` path is set to run a script in the resources directory that will create the application user and grant its schema privileges to match privileges of the corresponding user in production.<br/> Example:
    ```sql 
    CREATE USER overdrive_local WITH PASSWORD 'reverb_local';
    GRANT USAGE ON SCHEMA public to overdrive_local;
    GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public to overdrive_local;
    ``` 

### Implement a Spring Boot Application Class to Run the Application with the Local Profile
Add a Spring Boot Application Class named `LocalDevApplication` to your project's test source, preferably in the same package as the Spring Boot Application Class in the main source, to mirror the convention of Test Classes residing in the same package as the Classes they test.    

Annotate `LocalDevApplication` with `@EnableLocalPostgreSQL` and `@Profile("local")`. The `@EnableLocalPostgreSQL` activates configuration of the Testcontainers PostgreSQL module while `@Profile("local")` ensures that configuration declared within the `LocalDevApplication` is only scanned and initialized if the `local` profile is active. 

Inside the body of the `main` method, instantiate an instance of `SpringApplication` with the Application Class residing in the main source, to ensure that configuration in the main source is scanned. Then activate the `local` profile programmatically by calling `setAdditionalProfiles`. This will allow you to run `LocalDevApplication` in IntelliJ IDEA by simply right-clicking on the Class in the Project Panel and selecting `Run 'LocalDevApplication'` without having to add the `local` profile to the generated Spring Boot Run Configuration.

LocalDevApplication.java:
```java
@EnableLocalPostgreSQL
@Profile("local")
@SpringBootApplication
public class LocalDevApplication {

    public static void main(final String[] args) {
        final var springApplication = new SpringApplication(Application.class);
        springApplication.setAdditionalProfiles("local");
        springApplication.run(args);
    }
}
```
### Configure a Test Profile

Configure the `test` profile to use a random container name and port by leaving their properties undeclared so that default settings will be used. Random names and ports are best practice for Integration Tests, and means that Integration Tests can be executed while the Application is running locally with the `local` profile.

Set other configuration properties as desired, or not at all to use default settings.

application-test.properties:
```properties
spring.local.postgresql.container.image=postgres:15
spring.local.postgresql.database.name=pedals
spring.local.postgresql.database.username=fuzz_test
spring.local.postgresql.database.password=flange_test
spring.local.postgresql.database.application.username=overdrive_test
spring.local.postgresql.database.application.password=reverb_test
spring.local.postgresql.database.init.script=data/init-test.sql
```
In the example above, the `test` profile has the following settings:
- The `postgres:15` Docker Image is set to use a more recent version of PostgreSQL than the default (version 9) to match production.
- The `pedals` database name is set to match production, providing consistency for tests.
- The `fuzz_test/flange_test` username/password pair is set as the admin user for migrations (using tools like Flyway) to match production.
- The `overdrive_test/reverb_test` username/password pair is set as the application user to match production, a user with limited privileges that Integration Tests will use when executing queries with JPA and Hibernate, making them more effective and reliable.
- The `data/init.sql` path is set to run a script in the resources directory that will create the application user and grant its schema privileges to match privileges of the corresponding user in production.<br/> Example:
    ```sql 
    CREATE USER overdrive_test WITH PASSWORD 'reverb_test';
    GRANT USAGE ON SCHEMA public to overdrive_test;
    GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public to overdrive_test;
    ```
NOTE: One can, of course, configure the test profile and local profile to use the same username/password pairs, but using distinct pairs is recommended, in order to isolate environments and lower the risk of potential issues.

#### Implement an Integration Test

Add an Integration Test Class. Annotate with `@EnableLocalPostgreSQL`, `@ActiveProfiles("test")` and `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`. The `@EnableLocalPostgreSQL` activates configuration of the Testcontainers PostgreSQL module. The `@ActiveProfiles("test")` will activate the `test` profile when executed. And the `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`.

Write a test. The example below uses the RestAssured framework to call a REST endpoint backed by the PostgreSQL Container.

Example:

```java
@EnableLocalPostgreSQL
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GuitarPedalControllerTest {

    @LocalServerPort
    private Integer port;

    @Test
    void getAllGuitarPedals()  {
        given().port(port)
                .when().get("/guitar-pedals")
                .then().statusCode(HttpStatus.OK.value())
                .and().contentType(ContentType.JSON)
                .and().body("size()", equalTo(4))
                .and().body("guitarPedalName", hasItems(
                        "Big Muff Fuzz",
                        "Deco: Tape Saturation and Double Tracker",
                        "Soft Focus Reverb",
                        "Sneak Attack: Attack/Decay and Tremolo"))
                .and().body("dateSold", hasItems(null, null, null, "2023-03-21"));
    }
}
```
NOTE: It is, of course, possible to declare `@EnableLocalPostgreSQL` in a Spring Boot Application Class, named `TestApplication`, for example, so that one does not have to add `@EnableLocalPostgreSQL` to every test Class, and that may be appropriate in some cases, but in general it is recommended that each test Class controls the declaration of the resources it needs. After all, some test Classes may need both PostgreSQL and Kafka, for instance, while other test Classes may only need one or the other. In such a case, initializing PostgreSQL and Kafka containers for all test Classes would waste resources and prolong the time it takes for all test Classes to run. 

## Supported Configuration Properties
**spring.local.postgresql.engaged**<br/>
Whether the containerized PostgreSQL database should be configured and started when the Application starts. By default, it is set to `true`. To disengage, set to `false`.
    
**spring.local.postgresql.container.image**<br/>
The Docker Image with the chosen version of PostgreSQL (example: `postgres:15`). If undefined, Testcontainers will use its default (`postgres:9.6.12`). 

**spring.local.postgresql.container.name**<br/>
The name to use for the Docker Container when started. If undefined, a random name is used. Random names are preferred for Integration Tests, but when running the Application locally, a fixed name is useful, since it allows developers to find the running container with a consistent, predictable name.
    
**spring.local.postgresql.container.port**<br/>
The port on the Docker Container to map with the PostgreSQL port inside the container. If undefined, a random port is used. Random ports are preferred for Integration Tests, but when running the Application locally, a fixed port is useful, since it allows developers to configure any connecting, external tools or apps with a consistent, predictable port.

**spring.local.postgresql.container.log.follow**<br/>
Whether the Application should log the output produced by the container's log. By default, container logs are not followed. Set with `true` to see their output.

**spring.local.postgresql.database.name**<br/>
The name to use for the PostgreSQL database. If undefined, Testcontainers will use its default (`test`).

**spring.local.postgresql.database.username**<br/>
The username of an admin or superuser in the PostgreSQL database. If `spring.local.postgresql.database.application.username` is not defined, this will also be the username the Application uses to connect. If undefined, Testcontainers will use its default (`test`).

**spring.local.postgresql.database.password**<br/>
The password that goes with the username of the admin or superuser in the PostgreSQL database. If `spring.local.postgresql.database.application.username` is not defined, this will also be the password for the username the Application uses to connect. If undefined, Testcontainers will use its default (`test`).

**spring.local.postgresql.database.application.username**<br/>
In most cases the database user used by the Application should not have admin or superuser privileges. This property provides the ability to define the username of an "application user" for use during testing and local development. The Application will use this username to connect to the PostgreSQL database. If undefined, the value defined by `spring.local.postgresql.database.username` will be used instead. NOTE: The application user will NOT be created automatically. An init-script is required to create the user and grant their initial privileges.

**spring.local.postgresql.database.application.password**<br/>
In most cases the database user used by the Application should not have admin or superuser privileges. This property provides the ability to define the password for the username of an "application user" for use during testing and local development. The Application will use this password to connect to the PostgreSQL database. If undefined, the value defined by `spring.local.postgresql.database.password` will be used instead. NOTE: The application user will NOT be created automatically. An init-script is required to create the user and grant their initial privileges.

**spring.local.postgresql.database.init.script**<br/>
The path to an SQL file (with the `resources` directory as the root) that should be executed when the Docker Container starts. Executes before migrations. Useful for administrative tasks, like creating additional users, for example. If undefined, no script is executed.
