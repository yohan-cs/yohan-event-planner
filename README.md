# Event Planner (Backend)

A backend system for managing users and events, built with Spring Boot, PostgreSQL, and Flyway.  
Supports accurate **multi-timezone event scheduling** and **user-localized time tracking**.

---

## 📦 Tech Stack

- Java 21
- Spring Boot 3
- Spring Data JPA (Hibernate)
- Spring Security
- MapStruct
- PostgreSQL
- Flyway (schema versioning)
- JWT-based authentication
- JUnit 5 (unit & integration testing)
- Mockito (unit test mocking)
- MockMvc (Spring integration testing)

---

## 🚀 Getting Started

1. Clone the repo
git clone https://github.com/yohan-cs/yohan-event-planner.git
cd event-planner

2. Set up your local database
Make sure PostgreSQL is running and create a test database for local development.

Recommended defaults:
Database name: eventplanner_test

Username: your_db_user

Password: your_db_password

🛑 Do not commit your credentials — use a local config file that is excluded by Git.

3. Configure your application
Create the following files locally:

src/main/resources/application.properties
src/main/resources/application-test.properties

These should contain your local database and JWT configuration.

# application-test.properties (example)
spring.datasource.url=jdbc:postgresql://localhost:5432/eventplanner_test
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password

spring.jpa.hibernate.ddl-auto=create-drop
spring.flyway.enabled=false

spring.app.jwtSecret=your_base64_encoded_secret
spring.app.jwtExpirationMs=3600000

🧪 Running Tests
./mvnw test

Tests run against a PostgreSQL test database using create-drop to isolate test data.
Hibernate manages schema setup during tests, and Flyway is disabled in this mode.

🐘 Flyway Migrations
Flyway is used to track schema changes in versioned SQL files under:

src/main/resources/db/migration/

✅ Current baseline:
V1__init.sql: Full schema dump at the end of Version 1

🚧 Migration workflow:
During active development:

spring.jpa.hibernate.ddl-auto=create-drop
spring.flyway.enabled=false

When finalizing a version:

Dump the schema:
pg_dump --schema-only --no-owner --file=src/main/resources/db/migration/V2__init.sql eventplanner_test

Update config:
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
Commit the new migration script.

🌐 Timezone Support
This app is designed to handle multi-timezone event coordination reliably.

Key Features:
Users specify their preferred timezone at registration.
All events are stored in UTC but tracked with original start and end timezones.

API responses include:
UTC times for consistency
Original timezones for client-side localization
Creator’s default timezone for personalized context

Example fields in API responses:
startTime / endTime — UTC
startTimeZone / endTimeZone — Original declared zones
creatorTimezone — The event creator’s home zone

This structure allows clients to display times accurately in any user’s local context while keeping data consistent across time boundaries.

🔐 Security
JWT-based authentication is included. Secrets should be stored in local .properties files or environment variables — never committed to source control.

📂 Project Structure
bash
Copy
Edit
src/main/java/               # Main source code
src/test/java/               # Unit and integration tests
src/main/resources/
└── db/
    └── migration/           # Flyway migration SQLs

📜 License
All rights reserved.
