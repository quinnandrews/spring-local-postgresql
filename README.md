# Spring Local PostgreSQL

## Description
Provides configuration of an embedded PostgreSQL database (via Testcontainers)
within Spring Boot Applications for local development and testing.

Requires minimal configuration using Spring conventions, but a variety of optional properties are supported to override default behavior.

## Rationale
When developing an Application that uses PostgreSQL in production, an embedded PostgreSQL server provides the benefits of using an in-memory database, like H2, but avoids the downsides. The database is spun up and torn down when the Application starts up and shuts down, but developers are able to utilize PostgreSQL features (that alternatives like H2 may not support) and the test and local environments better resemble production. Development is more effective and reliable.

While Testcontainers is meant to be used exclusively in support of integration tests, this module is designed to support running the Application locally as well, reducing the overhead of maintaining Testcontainers and some other solution that fundamentally does the same thing. There is no need to maintain a local PostgreSQL server nor additional Docker configuration inside or outside the project.

## Requirements
### Java 17 
https://adoptium.net/temurin/releases/?version=17

### Docker
https://www.docker.com/products/docker-desktop/

### PostgreSQL JDBC Driver
https://jdbc.postgresql.org<br>
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Testcontainers PostgreSQL
Version `1.18.3` is included as a transitive dependency.<br>
https://www.testcontainers.org/modules/databases/postgres
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.18.3</version>
</dependency>
```

### Spring Boot 3
A variety of modules from version `3.1.2` are included as transitive dependencies.<br>
https://spring.io/projects/spring-boot
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.1.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
    </dependency>
</dependencies>>
```
## Usage

### Configuration

First, add this module as a dependency in your project:
```xml
<dependency>
    <groupId>io.github.quinnandrews</groupId>
    <artifactId>spring-local-postgresql</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Next, add `@EnableLocalPostreSQL` to your Spring Boot Application Class:
```java
@EnableLocalPostgreSQL
@SpringBootApplication
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

Then define the following property for the profiles that should initialize the embedded PostgreSQL:
```properties
spring.local.postgresql.engaged=true
```

If desired, define any of the following properties to override default behavior (see below for detailed descriptions):

```properties
spring.local.postgresql.container.image=postgres:15
spring.local.postgresql.container.port=15432
spring.local.postgresql.container.log.follow=true
spring.local.postgresql.database.name=local
spring.local.postgresql.database.username=superuser
spring.local.postgresql.database.password=password
spring.local.postgresql.database.application.username=appuser
spring.local.postgresql.database.application.password=password
spring.local.postgresql.database.init.script=data/init.sql
```

### Configuration as a Test Dependency
Understandably, one may prefer this module as a test dependency rather than a compile dependency. But this means that all configuration for this module can only reside in your project's test source, which is fine for integration tests, but how, then, can you run the Application locally?

To do this we implement an approach surfaced in this  [article](https://bsideup.github.io/posts/local_development_with_testcontainers/) by Sergei Egorov.

First, add this module as a test dependency in your project:
```xml
<dependency>
    <groupId>io.github.quinnandrews</groupId>
    <artifactId>spring-local-postgresql</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```
Next, add a Spring Boot Application Class to your project's test source that includes the `@EnableLocalPostgreSQL` Annotation and passes the Application Class in your project's main source to the `run` method (so that Configuration in the main source is initialized):
```java
@EnableLocalPostgreSQL
@SpringBootApplication
public class LocalDevApplication {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

Then you can reference `LocalApplication.class` in your integration tests to enable the embedded PostgreSQL by using the `classes` attribute on `@SpringBootTest`:
```java
@ActiveProfiles("test")
@SpringBootTest(classes = LocalApplication.class)
class IntegrationTest {

}
```

Or, alternatively, your integration tests can ignore `LocalApplication.class` and enable the embedded PostgreSQL by using `@EnableLocalPostgreSQL`:
```java
@EnableLocalPostgreSQL
@ActiveProfiles("test")
@SpringBootTest
class IntegrationTest {

}
```
Then you can create a `Run Configuration` in IntelliJ IDEA, for example, setting `LocalApplication.class` as the `Main Class` and defining `local` as the active profile. 

Now when you run the `Run Configuration` or your integration tests, the embedded PostgreSQL will be initialized, assuming that the `local` and `test` profiles have set the `spring.local.postgresql.engaged` property to `true'.`

**But there is a way to make this even more convenient.**

If your integration tests reference `LocalApplication.class`, it's worth noting that the `main` method isn't actually executed in that case. Spring is doing something different. 

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

Now you can run the Application locally in IntelliJ IDEA by simply right-clicking on `LocalApplication.class` in the Project Panel and selecting `Run 'LocalApplication'` from the Context Menu, which will also create a `Run Configuration` for later use. 

### Supported Configuration Properties

spring.local.postgresql.engaged
: Whether the embedded, containerized PostgreSQL database should be configured to start when the Application starts. By default, it is not engaged. Only engaged if `true`. 

spring.local.postgresql.container.image
: The name of a Docker Image containing a given version of PostgreSQL (example: `postgres:15`). If undefined, the Testcontainers default of `postgres:9.6.12` is used.

spring.local.postgresql.container.port
: The port on the Docker Container that should map to the port used by PostgreSQL inside the container. If undefined, a random port is used, which is preferred for integration tests, but when running the Application locally, defining a fixed port is useful. It gives developers the ability to configure a JDBC client with a consistent port. Otherwise, the port in the client's configuration must be updated if the Application had been restarted since the client was last used.

spring.local.postgresql.container.log.follow
: Whether the Application should log the log output produced by the Container. By default, container logs are not followed. Set with `true` to see their output.

spring.local.postgresql.database.name
: The name of the PostgreSQL database the Application will connect to. If undefined, defaults to the Testcontainers default of `test`.

spring.local.postgresql.database.username
: The username of an admin or superuser in the PostgreSQL database. If no `spring.local.postgresql.database.application.username` is defined, this will also be the username the Application uses to connect. If undefined, defaults to the Testcontainers default of `test`.

spring.local.postgresql.database.password
: The password that goes with the username of the admin or superuser in the PostgreSQL database. If no `spring.local.postgresql.database.application.username` is defined, this will also be the password for the username the Application uses to connect. If undefined, defaults to the Testcontainers default of `test`.

spring.local.postgresql.database.application.username
: In most cases the database user used by the Application should not have admin or superuser privileges. This property provides the ability to define the username of an "application user" for use during testing and local development. The Application will use this username to connect to the PostgreSQL database. If undefined, the value defined by `spring.local.postgresql.database.username` will be used instead. NOTE: This application user is NOT automatically created. An init-script is required to create the user and grant their initial privileges.

spring.local.postgresql.database.application.password
: In most cases the database user used by the Application should not have admin or superuser privileges. This property provides the ability to define the password for the username of an "application user" for use during testing and local development. The Application will use this password to connect to the PostgreSQL database. If undefined, the value defined by `spring.local.postgresql.database.password` will be used instead. NOTE: This application user is NOT automatically created. An init-script is required to create the user and grant their initial privileges.

spring.local.postgresql.database.init.script
: The path to an SQL file (beginning with the `resources` directory as the root)  that should be executed when the Docker Container starts. Executes before migrations. Useful for administrative tasks, like creating additional users, for example. If undefined, no script is executed.

## Examples

The test source contains an example Application as well as integration tests that test against it. They can be referenced as examples, if needed. 

However, a more robust, true to life example will be provided in the near future. 

## Roadmap
1) **Provide Robust Example**
2) **Support Option to Activate pgAdmin.**
3) **Support Testcontainers Reuse Property.**
