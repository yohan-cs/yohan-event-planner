# Event Planner (Backend)

A backend system for managing users and events, built with Spring Boot, PostgreSQL, and Flyway.  
Supports accurate multi-timezone event scheduling and user-localized time tracking.

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
- OpenAPI (Swagger) for API documentation
- dotenv-java for environment variable management
- Docker and Docker Compose for containerizing the app and PostgreSQL

## 🚀 Getting Started

1. Clone the repo
git clone https://github.com/yohan-cs/yohan-event-planner.git
cd event-planner

2. Set up your local environment
Make sure PostgreSQL is running locally, or use the included Docker Compose setup to run PostgreSQL and the app in containers:

docker-compose up

This will start both the Spring Boot backend and PostgreSQL using Docker.

3. Environment variables
Environment variables are managed with dotenv-java for local development via a .env file (which should be excluded from Git).

Make sure to create a .env file at the project root with your local configuration (database credentials, JWT secrets, etc).

4. Configure your application properties
Create these files if needed:

src/main/resources/application.properties
src/main/resources/application-test.properties

These should configure your local database and JWT settings.

Example application-test.properties

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

📖 API Documentation
The API is documented using OpenAPI/Swagger.

Once the app is running, access the Swagger UI at:
http://localhost:8080/swagger-ui/index.html

🐘 Flyway Migrations
Flyway manages database schema changes in:

src/main/resources/db/migration/

🚧 Migration workflow:
During active development:

spring.jpa.hibernate.ddl-auto=create-drop
spring.flyway.enabled=false

When finalizing a version:

Dump the schema:
pg_dump --schema-only --no-owner --file=src/main/resources/db/migration/V2__init.sql eventplanner_test

Update config:
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=trueAdd commentMore actions
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

## 📂 Project Structure

src/main/java/ # Main source code
src/test/java/ # Unit and integration tests
src/main/resources/
└── db/
└── migration/ # Flyway migration SQLs
Dockerfile # Docker image build for the Spring Boot app
docker-compose.yml # Docker Compose setup for app + PostgreSQL
.env # Local environment variables (excluded from version control)


📜 License
All rights reserved.