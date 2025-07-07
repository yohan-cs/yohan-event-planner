# Event Planner API

A comprehensive event management system with advanced scheduling capabilities, built using modern Spring Boot architecture. Features multi-timezone support, recurring events, user management, and real-time calendar generation.

## 🚀 Key Features

### 📅 **Advanced Event Management**
- **Flexible Event Types**: Scheduled, impromptu, untimed, and unconfirmed draft events
- **Multi-timezone Support**: Events stored in UTC with timezone metadata
- **Untimed Events**: Support for open-ended or impromptu activities without end times
- **Event Completion**: Track and mark events as completed with automatic time tracking
- **Event Recaps**: Media attachments (images/videos) with ordering and summaries
- **Media Management**: Full CRUD operations for event recap media with duration tracking
- **Flexible Creation Workflows**: Start immediately, fill details later, or save drafts

### 🔄 **Recurring Events**
- **Complex Recurrence Patterns**: Full RRule support with daily, weekly, and monthly patterns
- **Infinite Recurrence Support**: Handle recurring events that last forever without performance degradation
- **Virtual Event Generation**: Dynamic calendar views without storing individual instances
- **Skip Days Management**: Flexible exception handling for recurring patterns
- **Future Event Propagation**: Changes to recurring events automatically update future instances
- **Conflict Detection**: Automatic validation to prevent scheduling conflicts
- **Efficient Calendar Rendering**: On-demand generation handles infinite sequences gracefully

### 👥 **User Management**
- **Secure Authentication**: JWT-based stateless authentication
- **Role-based Access**: User roles and permissions system
- **Soft Deletion**: User cleanup with scheduled deletion workflow
- **Timezone Preferences**: Per-user timezone configuration

### 🏷️ **Organization & Analytics Features**
- **Labels**: Categorize and organize events with custom labels and automatic "Unlabeled" system
- **Badges**: Multi-label collections that aggregate time statistics across related activities
- **Badge Analytics**: Track time spent today, this week, this month, last week, last month, and all-time
- **Time Analytics**: Automatic time tracking with daily/weekly/monthly bucketing
- **Statistics**: Today, this week, this month analytics with historical comparisons
- **Visual Time Tracking**: Calendar heat maps showing productivity patterns by label
- **Smart Calendar Views**: Day, week, and month calendar generation with label-specific analytics
- **Calendar Analytics**: Monthly views show days with completed events and total time spent per label
- **My Events**: Personalized event views with cursor-based pagination
- **Draft Management**: Separate handling and bulk operations for unconfirmed events

### 🔍 **Advanced Querying & Search**
- **GraphQL API**: Flexible data fetching with custom scalars and nested queries
- **Blaze-Persistence**: High-performance JPA queries with complex filtering
- **Advanced Search**: Time-based filtering (ALL, PAST_ONLY, FUTURE_ONLY, CUSTOM)
- **Label-based Filtering**: Search and filter by event categories
- **Cursor-based Pagination**: Efficient handling of large datasets
- **Real-time Calendar**: Dynamic virtual event generation for calendar views
- **Conflict Detection**: Intelligent scheduling conflict resolution

### ⚙️ **System Features & Automation**
- **Background Jobs**: Automated token cleanup and user deletion processing
- **Soft Deletion**: 30-day grace period for user account deletion
- **System Protection**: Automatic "Unlabeled" label creation and protection
- **Token Management**: Automatic cleanup of expired and revoked tokens
- **Time Bucket Processing**: Real-time analytics aggregation
- **Conflict Validation**: Automatic overlap detection for events
- **Timezone Handling**: Intelligent conversion and storage

## 🛠️ Technology Stack

### **Core Framework**
- **Java 21** - Latest LTS with modern language features
- **Spring Boot 3.4.5** - Enterprise-grade application framework
- **Spring Security** - Comprehensive security and authentication
- **Spring Data JPA** - Data persistence with Hibernate

### **Database & Persistence**
- **PostgreSQL 17** - Advanced relational database
- **Flyway** - Database schema version control
- **Blaze-Persistence** - High-performance JPA query optimization
- **Hibernate** - Object-relational mapping

### **API & Integration**
- **REST API** - Traditional RESTful endpoints
- **GraphQL** - Flexible query language for client applications
- **OpenAPI/Swagger** - Comprehensive API documentation
- **MapStruct** - Type-safe object mapping

### **Development & Testing**
- **JUnit 5** - Modern testing framework
- **Mockito** - Mock object framework for unit testing
- **TestContainers** - Integration testing with real databases
- **Maven** - Dependency management and build automation

### **DevOps & Configuration**
- **Docker & Docker Compose** - Containerization and orchestration
- **dotenv-java** - Environment variable management
- **Jackson** - JSON processing with custom serializers

## 🏗️ Architecture

### **Layered Architecture**
```
Controllers (REST/GraphQL) 
    ↓
Services (Orchestration)
    ↓  
Business Objects (Domain Logic)
    ↓
Repositories (Data Access)
    ↓
Database (PostgreSQL)
```

### **Key Design Patterns**
- **Service Layer Pattern**: Clean separation of concerns
- **Repository Pattern**: Data access abstraction
- **DTO Pattern**: Data transfer with MapStruct mapping
- **Command Pattern**: Complex business operations
- **Strategy Pattern**: Flexible recurrence rule handling

### **Domain Model**
- **User**: Authentication, preferences, soft deletion, automatic unlabeled label
- **Event**: Flexible events (scheduled/impromptu/untimed/draft) with completion lifecycle
- **RecurringEvent**: RRule-based templates with skip days and virtual generation
- **Label**: Event categorization with time tracking and system protection
- **Badge**: Multi-label collections with comprehensive time analytics (today/week/month/historical/all-time)
- **EventRecap**: Post-event documentation with media attachments and ordering
- **LabelTimeBucket**: Automatic time analytics aggregation (day/week/month)
- **RecapMedia**: Media management with type classification and duration tracking

## 🚀 Quick Start

### **Prerequisites**
- Java 21+
- Docker & Docker Compose
- PostgreSQL 17 (or use Docker)

### **1. Clone & Setup**
```bash
git clone <repository-url>
cd event-planner
```

### **2. Environment Configuration**
Create a `.env` file in the project root:
```env
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/eventplanner
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password

# Test Database
SPRING_DATASOURCE_TEST_URL=jdbc:postgresql://localhost:5432/eventplanner_test
SPRING_DATASOURCE_TEST_USERNAME=your_test_username
SPRING_DATASOURCE_TEST_PASSWORD=your_test_password

# JWT Configuration
SPRING_APP_JWT_SECRET=your_base64_encoded_secret
SPRING_APP_JWT_SECRET_TEST=your_test_jwt_secret
SPRING_APP_JWT_EXPIRATION_MS=3600000
SPRING_APP_JWT_REFRESH_EXPIRATION_MS=604800000

# Docker PostgreSQL
POSTGRES_DB=eventplanner
POSTGRES_USER=your_username
POSTGRES_PASSWORD=your_password
```

### **3. Run with Docker**
```bash
# Start PostgreSQL and application
docker-compose up

# Or run PostgreSQL only and start app locally
docker-compose up postgres
./mvnw spring-boot:run
```

### **4. Local Development**
```bash
# Build the project
./mvnw clean compile

# Run tests
./mvnw test

# Start application
./mvnw spring-boot:run
```

## 📋 Development Commands

### **Maven Commands**
```bash
# Build project
./mvnw clean compile

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=EventServiceImplTest

# Package application
./mvnw clean package

# Run application
./mvnw spring-boot:run
```

### **Docker Commands**
```bash
# Start all services
docker-compose up

# Rebuild and start
docker-compose up --build

# Start in background
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f app
```

### **Database Commands**
```bash
# Generate migration from test schema
pg_dump --schema-only --no-owner \
  --file=src/main/resources/db/migration/V3__latest_changes.sql \
  eventplanner_test

# Connect to database
psql -h localhost -U your_username eventplanner
```

## 📚 API Documentation

### **Interactive Documentation**
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

### **GraphQL Playground**
- **GraphQL Endpoint**: http://localhost:8080/graphql
- **Schema**: Available at `src/main/resources/graphql/schema.graphqls`

### **Key Endpoints**

#### **Authentication**
- `POST /auth/register` - User registration
- `POST /auth/login` - User authentication
- `POST /auth/refresh` - Refresh access token using refresh token
- `POST /auth/logout` - Revoke refresh token and logout

#### **Events**
- `GET /events` - List user's events with filtering
- `POST /events` - Create new event (scheduled/impromptu/draft)
- `PATCH /events/{id}` - Update event (partial updates supported)
- `DELETE /events/{id}` - Delete event
- `POST /events/{id}/confirm` - Confirm draft event
- `GET /events/{id}/recap` - Get event recap with media
- `POST /events/{id}/recap` - Create event recap
- `PATCH /events/{id}/recap` - Update event recap
- `DELETE /events/{id}/recap` - Delete event recap
- `POST /events/{id}/recap/confirm` - Confirm event recap

#### **Recurring Events**
- `GET /recurring-events` - List recurring events
- `POST /recurring-events` - Create recurring event pattern
- `PATCH /recurring-events/{id}` - Update recurring event
- `POST /recurring-events/{id}/confirm` - Confirm recurring event
- `DELETE /recurring-events/{id}` - Delete recurring event
- `POST /recurring-events/{id}/skipdays` - Add skip days
- `DELETE /recurring-events/{id}/skipdays` - Remove skip days

#### **Calendar Views**
- `GET /calendar?labelId={id}&year={year}&month={month}` - Monthly calendar with label-specific time analytics
  - Shows days with completed events for the specified label
  - Displays total minutes spent on that label for the month
  - Visual indicators for productive days

#### **My Events**
- `GET /myevents?startTimeCursor={time}&endTimeCursor={time}&limit={n}` - Paginated user events
- `GET /myevents/drafts` - Get all draft events and recurring events
- `DELETE /myevents/drafts` - Delete all drafts (bulk operation)

#### **Search**
- `GET /search/events` - Advanced event search with time/label filtering
- `GET /search/recurringevents` - Search recurring events

#### **User Management**
- `GET /settings` - Get user profile and settings
- `PATCH /settings` - Update user settings (partial updates)
- `DELETE /settings` - Delete user account (soft deletion)
- `GET /usertools` - Get all badges and labels for user

#### **Labels & Badges**
- `GET /labels/{id}` - Get label details
- `POST /labels` - Create new label
- `PATCH /labels/{id}` - Update label
- `DELETE /labels/{id}` - Delete label
- `GET /badges/{id}` - Get badge with comprehensive time statistics across all contained labels
- `POST /badges` - Create new badge (collection of labels)
- `PATCH /badges/{id}` - Update badge and label associations
- `DELETE /badges/{id}` - Delete badge

#### **GraphQL API**
- `Query: userProfile(username: String!)` - User profile with week view
- `Query: eventRecap(eventId: ID!)` - Event recap details
- `Mutation: updateUserHeader` - Update user bio and profile picture
- `Mutation: Badge/Event/Recap Management` - Full CRUD with reordering

## 🧪 Testing

### **Test Structure**
```
src/test/java/
├── business/           # Business object unit tests
├── controller/         # Integration tests for REST/GraphQL
├── service/           # Service layer unit tests
├── security/          # Security and authentication tests
└── util/              # Test utilities and helpers
```

### **Test Categories**
- **Unit Tests**: Fast, isolated tests for business logic
- **Integration Tests**: Full Spring context with test database
- **Security Tests**: Authentication and authorization
- **Repository Tests**: Data access layer validation

### **Running Tests**
```bash
# All tests
./mvnw test

# Specific test categories
./mvnw test -Dtest="**/*IntegrationTest"
./mvnw test -Dtest="**/*BOImplTest"

# With coverage
./mvnw test jacoco:report
```

## 🔧 Configuration

### **Application Profiles**
- **Development**: `create-drop` + Flyway disabled
- **Production**: `validate` + Flyway enabled
- **Test**: `create-drop` + Flyway disabled

### **Key Configuration Files**
- `application.properties` - Main application configuration
- `application-test.properties` - Test-specific settings
- `docker-compose.yml` - Container orchestration

### **Environment Variables**
All sensitive configuration is externalized via environment variables, loaded through dotenv-java for local development.

## 🌐 Multi-Timezone Support

### **Design Principles**
- **UTC Storage**: All times stored in UTC for consistency
- **Timezone Metadata**: Original timezone information preserved
- **User Preferences**: Per-user timezone settings
- **Localized Views**: Calendar views respect user timezone

### **Implementation Details**
- Events store `startTime`/`endTime` in UTC
- Original timezone IDs preserved separately
- API responses include both UTC and localized times
- Virtual events generated with proper timezone conversion
- Infinite recurring patterns handled with bounded windowing algorithms
- Calendar views efficiently render large date ranges without storing instances

## 📊 Advanced Analytics System

### **Badge-Based Time Tracking**
The application features a sophisticated analytics system where **Badges** serve as collections of related **Labels**, providing comprehensive time tracking across multiple categories:

**Badge Structure**:
- Each badge contains multiple related labels (e.g., "Fitness" badge might include "Gym", "Running", "Yoga" labels)
- Time is automatically aggregated across all labels within a badge
- Provides both granular (per-label) and comprehensive (per-badge) analytics

**Time Period Analytics**:
- **Today**: Minutes spent on badge activities today
- **This Week**: Current week's total time
- **This Month**: Current month's total time  
- **Last Week**: Previous week's comparison data
- **Last Month**: Previous month's comparison data
- **All Time**: Historical total across all time

**Use Cases**:
- **Work Projects**: Group project-related labels for total project time
- **Health & Fitness**: Combine exercise types for comprehensive fitness tracking
- **Learning Goals**: Aggregate study subjects for educational progress
- **Life Categories**: Track work-life balance across different life areas

This system transforms event completion into powerful personal analytics and productivity insights.

## 🔐 Security Features

### **Authentication & Authorization**
- **JWT Access Tokens**: Short-lived tokens for API authentication
- **Refresh Tokens**: Long-lived tokens for seamless token renewal
- **Token Rotation**: Automatic refresh token rotation for enhanced security
- **Token Revocation**: Secure logout with refresh token invalidation
- **Role-based Access**: User roles and permissions
- **Ownership Validation**: Resource-level access control
- **Secure Endpoints**: All non-auth endpoints require authentication

### **Authentication Flow**
The system implements a dual-token authentication strategy for optimal security and user experience:

1. **Login**: User receives both access token (short-lived) and refresh token (long-lived)
2. **API Access**: Access token used for authenticated requests
3. **Token Refresh**: When access token expires, refresh token automatically gets a new access token
4. **Token Rotation**: Each refresh operation issues a new refresh token and revokes the old one
5. **Logout**: Refresh token is revoked and marked as invalid in the database

**Token Lifespans**:
- Access Token: 1 hour (configurable)
- Refresh Token: 7 days (configurable)

**Security Benefits**:
- Reduced exposure of long-lived credentials
- Automatic session renewal without re-authentication
- Secure logout that prevents token reuse
- Protection against token theft and replay attacks

### **Security Best Practices**
- Password hashing with BCrypt
- JWT secret management via environment variables
- Input validation and sanitization
- SQL injection prevention through JPA
- HTTPS enforcement in production

## 🚀 Deployment

### **Production Deployment**
1. **Database Setup**: Configure PostgreSQL with Flyway migrations
2. **Environment Variables**: Set production secrets and configuration
3. **Build**: `./mvnw clean package -Dmaven.test.skip=true`
4. **Run**: `java -jar target/event-planner-0.0.1-SNAPSHOT.jar`

### **Docker Deployment**
```bash
# Build production image
docker build -t event-planner .

# Run with environment file
docker run --env-file .env -p 8080:8080 event-planner
```

## 📈 Performance Features

- **Blaze-Persistence**: Optimized JPA queries with pagination
- **Virtual Events**: Recurring events generated on-demand without database storage
- **Infinite Recurrence Handling**: Efficient algorithms for never-ending recurring events
- **Memory-Efficient Calendar Views**: Generate large date ranges without memory issues
- **Lazy Loading**: Efficient entity relationship loading
- **Connection Pooling**: Database connection optimization
- **Bounded Generation**: Smart windowing for infinite sequences in calendar views
- **Caching**: Strategic caching for frequently accessed data

## 🤝 Contributing

### **Development Setup**
1. Follow the Quick Start guide
2. Run tests before committing: `./mvnw test`
3. Follow existing code patterns and architecture

### **Code Style**
- Java 21 modern features encouraged
- Service layer for orchestration
- Business Objects for domain logic
- Comprehensive test coverage
- Clear, descriptive variable names

## 📄 License

All rights reserved.