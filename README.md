# Spring Local PostgreSQL

## Description
Provides enhanced configuration of an embedded PostgreSQL database provided by Testcontainers. For use within Spring Boot Applications to support local development in addition to integration test execution.

Requires minimal configuration using Spring conventions, but a variety of optional properties are provided to override default behavior and control the underlying container's configuration by profile.

## Features
- Configure whether the embedded PostgreSQL is active or not. Allows you to control its activation by profile.
- Configure the Docker image to contain the embedded PostgreSQL. Allows you to match your local and test environments with the version of PostgreSQL running in production.
- Configure the embedded PostgreSQL to run on a defined, fixed port. Useful for local development so that developers can connect to the database with their client of choice with consistent configuration.
- Configure whether to follow the containers log output. Useful for troubleshooting.
- Configure the database name to match production.
- Configure a database super user to handle migration scripts and a second user with fewer privileges for the Application to use. 
- Configure an SQL script to run when the PostgreSQL database is spun up.

## Rationale
When developing an Application that uses PostgreSQL in production, an embedded PostgreSQL server provides the benefits of using an in-memory database, like H2, but avoids the downsides. The database is spun up and torn down when the Application starts up and shuts down, but developers are able to utilize PostgreSQL features (that alternatives like H2 may not support) and the test and local environments better resemble production. Development is more effective and reliable.

While Testcontainers is meant to be used exclusively in support of integration tests, this project is designed to support running the Application locally as well, reducing the overhead of maintaining Testcontainers and some other solution that fundamentally does the same thing. There is no need to maintain a local PostgreSQL server nor additional Docker configuration inside or outside the project.

## Requirements
### Java 17 
https://adoptium.net/temurin/releases/?version=17

### Docker
https://www.docker.com/products/docker-desktop/

### PostgreSQL JDBC Driver
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Spring Boot Starter JDBC
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

## Dependencies
- Spring Boot Starter Web 3.2.0
- Spring Boot Configuration Processor 3.2.0
- Spring Boot Starter Test 3.2.0
- Spring Boot Testcontainers 3.2.0
- Testcontainers PostgreSQL 1.19.3

## Usage

### Configuration as a Test Dependency
While it is possible to configure this project as a compile dependency, and control its activation with profiles, it is not good practice. This project should always be configured as a test dependency. 

However, this means that all configuration of this project can only reside in your project's test source, which is fine for integration tests, but= what about running the Application locally?

To do this we implement an approach surfaced in this  [article](https://bsideup.github.io/posts/local_development_with_testcontainers/) by Sergei Egorov.

First, Add this project's artifact to your project as a test dependency:
```xml
<dependency>
    <groupId>io.github.quinnandrews</groupId>
    <artifactId>spring-local-postgresql</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```
(NOTE: This project's artifact is NOT yet available in Maven Central, but is available from GitHub Packages.)

Next, create properties files in your the test resources directory for `local` and `test` profiles with your configuration of choice. We recommend that the `local` profile is configured with a fixed port while the `test` profile is configured to use a random port, which is considered best practice for integration tests. And you may want do things like enable following of the container logs for the `local` profile as well:

application-local.properties
```properties
spring.local.postgresql.container.image=postgres:15
spring.local.postgresql.container.port=15432
spring.local.postgresql.container.log.follow=true
spring.local.postgresql.database.name=pedals
spring.local.postgresql.database.username=fuzz
spring.local.postgresql.database.password=echo
spring.local.postgresql.database.application.username=overdrive
spring.local.postgresql.database.application.password=reverb
spring.local.postgresql.database.init.script=data/init.sql
```

application-test.properties
```properties
spring.local.postgresql.container.image=postgres:15
spring.local.postgresql.database.name=pedals
spring.local.postgresql.database.username=fuzz
spring.local.postgresql.database.password=echo
spring.local.postgresql.database.application.username=overdrive
spring.local.postgresql.database.application.password=reverb
spring.local.postgresql.database.init.script=data/init.sql
```

Then add a Spring Boot Application Class to your project's test source that includes the `@EnableLocalPostgreSQL` Annotation and passes the Application Class in your project's main source to the `run` method (so that Configuration in the main source is initialized):
```java
@EnableLocalPostgreSQL
@SpringBootApplication
public class LocalDevApplication {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

Now you can reference `LocalApplication.class` in your integration tests to enable the embedded PostgreSQL by using the `classes` attribute on `@SpringBootTest`:
```java
@ActiveProfiles("test")
@SpringBootTest(classes = LocalApplication.class)
class IntegrationTest {

}
```

Or, alternatively, your integration tests can include the embedded PostgreSQL directly by using `@EnableLocalPostgreSQL` (NOTE: You may need to declare the Application Class in your main source using `classes` attribute to avoid conflicts):
```java
@EnableLocalPostgreSQL
@ActiveProfiles("test")
@SpringBootTest
class IntegrationTest {

}
```

Then you can create a `Run Configuration` in IntelliJ IDEA, for example, setting `LocalApplication.class` as the `Main Class` and defining `local` as the active profile. 

Now when you run the `Run Configuration` or your integration tests, the embedded PostgreSQL will be initialized.

**_But there is a way to make this even more convenient._**

If your integration tests reference `LocalApplication.class`, it's worth noting that the `main` method isn't actually executed when the tests run. Spring is using the Class for reference and doing something different in place of executing the `main` method. 

What this means is that you can activate the `local` profile explicitly in the body of the `main` method:
```java
@EnableLocalPostgreSQL
@SpringBootApplication
public class LocalApplication {

    public static void main(final String[] args) {
        final var springApplication = new SpringApplication(Application.class);
        springApplication.setAdditionalProfiles("local");
        springApplication.run(args);
    }
}
```

Now you can run the Application locally in IntelliJ IDEA by simply right-clicking on `LocalApplication.class` in the Project Panel and selecting `Run 'LocalApplication'` from the Context Menu, which will also create a `Run Configuration` for later use, but you don't need to do make any changes to the `Run Configuration`. Its default setting work just fine.  

### Supported Configuration Properties

<dl>
<dt>spring.local.postgresql.engaged</dt>
<dd>
    Whether the embedded, containerized PostgreSQL database should be configured to start when the Application starts. By default, it is engaged. To explicitly engage, set to `true`. To disengage, set to 'false'. 
</dd>
    
<dt>spring.local.postgresql.container.image</dt>
<dd>
    The name of a Docker Image containing a given version of PostgreSQL (example: `postgres:15`). If undefined, the Testcontainers default of `postgres:9.6.12` is used.
</dd>
    
<dt>spring.local.postgresql.container.port</dt>
<dd>
    The port on the Docker Container that should map to the port used by PostgreSQL inside the container. If undefined, a random port is used, which is preferred for integration tests, but when running the Application locally, defining a fixed port is useful. It gives developers the ability to configure a JDBC client with a consistent port. Otherwise, the port in the client's configuration must be updated if the Application had been restarted since the client was last used.
</dd>

<dt>spring.local.postgresql.container.log.follow</dt>
<dd>
    Whether the Application should log the log output produced by the Container. By default, container logs are not followed. Set with `true` to see their output.
</dd>

<dt>spring.local.postgresql.database.name</dt>
<dd>
    The name of the PostgreSQL database the Application will connect to. If undefined, defaults to the Testcontainers default of `test`.
</dd>

<dt>spring.local.postgresql.database.username</dt>
<dd>
    The username of an admin or superuser in the PostgreSQL database. If no `spring.local.postgresql.database.application.username` is defined, this will also be the username the Application uses to connect. If undefined, defaults to the Testcontainers default of `test`.
</dd>

<dt>spring.local.postgresql.database.password</dt>
<dd>
    The password that goes with the username of the admin or superuser in the PostgreSQL database. If no `spring.local.postgresql.database.application.username` is defined, this will also be the password for the username the Application uses to connect. If undefined, defaults to the Testcontainers default of `test`.
</dd>

<dt>spring.local.postgresql.database.application.username</dt>
<dd>
    In most cases the database user used by the Application should not have admin or superuser privileges. This property provides the ability to define the username of an "application user" for use during testing and local development. The Application will use this username to connect to the PostgreSQL database. If undefined, the value defined by `spring.local.postgresql.database.username` will be used instead. NOTE: This application user is NOT automatically created. An init-script is required to create the user and grant their initial privileges.
</dd>

<dt>spring.local.postgresql.database.application.password</dt>
<dd>
    In most cases the database user used by the Application should not have admin or superuser privileges. This property provides the ability to define the password for the username of an "application user" for use during testing and local development. The Application will use this password to connect to the PostgreSQL database. If undefined, the value defined by `spring.local.postgresql.database.password` will be used instead. NOTE: This application user is NOT automatically created. An init-script is required to create the user and grant their initial privileges.
</dd>

<dt>spring.local.postgresql.database.init.script</dt>
<dd>
    The path to an SQL file (beginning with the `resources` directory as the root)  that should be executed when the Docker Container starts. Executes before migrations. Useful for administrative tasks, like creating additional users, for example. If undefined, no script is executed.
</dd>
</dl>

## Examples

The test source contains an example Application as well as integration tests that test against it. They can be referenced as examples, if needed. 

However, a more robust, true to life example will be provided in the near future. 

## Roadmap
1) **Provide Robust Example**
2) **Support Option to Activate pgAdmin.**
3) **Support Testcontainers Reuse Property.**
