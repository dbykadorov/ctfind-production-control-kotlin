# CTfind Production Control Contlin

New Spring Boot + Kotlin attempt for CTfind Production Control.

## Stack

- Kotlin
- Spring Boot
- Gradle Kotlin DSL
- Java 21
- PostgreSQL
- Flyway
- Spring Security
- Spring Data JPA
- Bean Validation
- Actuator

## Local Checks

Requires JDK 21 available on `PATH` or via `JAVA_HOME`.

```bash
./gradlew test
```

## Product Context

The first implementation target is Phase 1:

- users, employees, roles, and access control;
- orders;
- production tasks;
- basic inventory;
- audit log;
- internal notifications.

Phase 2 is expected to add Theory of Constraints concepts on top of the operational facts captured in Phase 1.
