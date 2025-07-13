# Event Planner Backend API

A comprehensive event management system with advanced scheduling capabilities, built using modern Spring Boot architecture. Features multi-timezone support, recurring events, user management, real-time calendar generation, and **impromptu event pinning** for dashboard reminders.

> **📱 Flutter Integration Ready**: This backend is designed for seamless integration with Flutter mobile applications. Complete REST/GraphQL APIs, JWT authentication, and data contracts optimized for mobile development patterns.

## 🚀 Key Features

### 📅 **Advanced Event Management**
- **Flexible Event Types**: Scheduled, impromptu, untimed, and unconfirmed draft events
- **Impromptu Event Pinning**: Pin impromptu events to user dashboard as reminders with auto-unpin safeguards
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
- **Labels**: Categorize and organize events with custom labels, visual color coding (base/pastel/metallic variants), and automatic "Unlabeled" system
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
- **Rate Limiting**: Per-IP protection against abuse with configurable policies

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

## 📱 Flutter Integration Guide

### **Quick Start for Flutter Developers**

This backend provides a complete REST API and GraphQL interface optimized for Flutter mobile applications. All endpoints return JSON with consistent error handling and proper HTTP status codes.

### **Base Configuration**

```dart
// lib/config/api_config.dart
class ApiConfig {
  static const String baseUrl = 'http://localhost:8080'; // Development
  static const String prodUrl = 'https://your-api-domain.com'; // Production
  
  // Endpoints
  static const String authEndpoint = '/auth';
  static const String eventsEndpoint = '/events';
  static const String graphqlEndpoint = '/graphql';
}
```

### **Authentication Flow for Flutter**

#### **1. User Registration & Login**
```dart
// lib/services/auth_service.dart
class AuthService {
  static const String _accessTokenKey = 'access_token';
  static const String _refreshTokenKey = 'refresh_token';

  // Register new user
  Future<AuthResponse> register(String username, String firstName, String lastName, String password, String timezone) async {
    final response = await http.post(
      Uri.parse('${ApiConfig.baseUrl}/auth/register'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'username': username,
        'firstName': firstName,
        'lastName': lastName,
        'password': password,
        'timezone': timezone, // e.g., "America/New_York"
      }),
    );
    
    if (response.statusCode == 201) {
      final data = jsonDecode(response.body);
      await _storeTokens(data['accessToken'], data['refreshToken']);
      return AuthResponse.fromJson(data);
    } else {
      throw AuthException(response.body);
    }
  }

  // Login existing user
  Future<AuthResponse> login(String username, String password) async {
    final response = await http.post(
      Uri.parse('${ApiConfig.baseUrl}/auth/login'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'username': username,
        'password': password,
      }),
    );
    
    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      await _storeTokens(data['accessToken'], data['refreshToken']);
      return AuthResponse.fromJson(data);
    } else {
      throw AuthException(response.body);
    }
  }

  // Auto-refresh tokens
  Future<String?> refreshAccessToken() async {
    final refreshToken = await _getRefreshToken();
    if (refreshToken == null) return null;

    final response = await http.post(
      Uri.parse('${ApiConfig.baseUrl}/auth/refresh'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'refreshToken': refreshToken}),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      await _storeTokens(data['accessToken'], data['refreshToken']);
      return data['accessToken'];
    }
    return null;
  }
}
```

#### **2. HTTP Client with Auto-Refresh**
```dart
// lib/services/api_client.dart
class ApiClient {
  final http.Client _client = http.Client();
  final AuthService _authService = AuthService();

  Future<http.Response> get(String endpoint) async {
    return _makeRequest(() => _client.get(
      Uri.parse('${ApiConfig.baseUrl}$endpoint'),
      headers: await _getHeaders(),
    ));
  }

  Future<http.Response> post(String endpoint, Map<String, dynamic> body) async {
    return _makeRequest(() => _client.post(
      Uri.parse('${ApiConfig.baseUrl}$endpoint'),
      headers: await _getHeaders(),
      body: jsonEncode(body),
    ));
  }

  Future<http.Response> _makeRequest(Future<http.Response> Function() request) async {
    var response = await request();
    
    // Auto-refresh on 401
    if (response.statusCode == 401) {
      final newToken = await _authService.refreshAccessToken();
      if (newToken != null) {
        response = await request(); // Retry with new token
      }
    }
    
    return response;
  }

  Future<Map<String, String>> _getHeaders() async {
    final token = await _authService.getAccessToken();
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }
}
```

### **Event Management for Flutter**

#### **1. Create Impromptu Event (Quick Add)**
```dart
// lib/services/event_service.dart
class EventService {
  final ApiClient _apiClient = ApiClient();

  // Create impromptu event for dashboard pinning
  Future<EventResponse> createImpromptuEvent() async {
    final response = await _apiClient.post('/events/impromptu', {});
    
    if (response.statusCode == 201) {
      return EventResponse.fromJson(jsonDecode(response.body));
    } else {
      throw EventException('Failed to create impromptu event');
    }
  }

  // Create scheduled event
  Future<EventResponse> createEvent({
    required String name,
    required DateTime startTime,
    required DateTime endTime,
    String? description,
    int? labelId,
    bool isDraft = false,
  }) async {
    final response = await _apiClient.post('/events', {
      'name': name,
      'startTime': startTime.toUtc().toIso8601String(),
      'endTime': endTime.toUtc().toIso8601String(),
      'description': description,
      'labelId': labelId,
      'isDraft': isDraft,
    });
    
    if (response.statusCode == 201) {
      return EventResponse.fromJson(jsonDecode(response.body));
    } else {
      throw EventException('Failed to create event');
    }
  }
}
```

#### **2. GraphQL Integration for User Profiles**
```dart
// lib/services/graphql_service.dart
class GraphQLService {
  final ApiClient _apiClient = ApiClient();

  // Get user profile with pinned impromptu event
  Future<UserProfile> getUserProfile(String username) async {
    final query = '''
      query GetUserProfile(\$username: String!) {
        userProfile(username: \$username) {
          isSelf
          header {
            username
            firstName
            lastName
            bio
            profilePictureUrl
          }
          pinnedImpromptuEvent {
            id
            name
            startTimeUtc
            impromptu
            unconfirmed
          }
          badges {
            id
            name
            timeStats {
              minutesToday
              minutesThisWeek
              minutesThisMonth
            }
          }
        }
      }
    ''';

    final response = await _apiClient.post('/graphql', {
      'query': query,
      'variables': {'username': username},
    });

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      return UserProfile.fromJson(data['data']['userProfile']);
    } else {
      throw GraphQLException('Failed to fetch user profile');
    }
  }

  // Unpin impromptu event
  Future<bool> unpinImpromptuEvent() async {
    final mutation = '''
      mutation {
        unpinImpromptuEvent
      }
    ''';

    final response = await _apiClient.post('/graphql', {
      'query': mutation,
    });

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      return data['data']['unpinImpromptuEvent'] == true;
    }
    return false;
  }
}
```

### **Data Models for Flutter**

#### **Core Event Model**
```dart
// lib/models/event_response.dart
class EventResponse {
  final int id;
  final String? name;
  final DateTime? startTimeUtc;
  final DateTime? endTimeUtc;
  final int? durationMinutes;
  final String? startTimeZone;
  final String? endTimeZone;
  final String? description;
  final String creatorUsername;
  final String creatorTimezone;
  final LabelResponse? label;
  final bool isCompleted;
  final bool unconfirmed;
  final bool impromptu;
  final bool isVirtual;

  EventResponse({
    required this.id,
    this.name,
    this.startTimeUtc,
    this.endTimeUtc,
    this.durationMinutes,
    this.startTimeZone,
    this.endTimeZone,
    this.description,
    required this.creatorUsername,
    required this.creatorTimezone,
    this.label,
    required this.isCompleted,
    required this.unconfirmed,
    required this.impromptu,
    required this.isVirtual,
  });

  factory EventResponse.fromJson(Map<String, dynamic> json) {
    return EventResponse(
      id: json['id'],
      name: json['name'],
      startTimeUtc: json['startTimeUtc'] != null ? DateTime.parse(json['startTimeUtc']) : null,
      endTimeUtc: json['endTimeUtc'] != null ? DateTime.parse(json['endTimeUtc']) : null,
      durationMinutes: json['durationMinutes'],
      startTimeZone: json['startTimeZone'],
      endTimeZone: json['endTimeZone'],
      description: json['description'],
      creatorUsername: json['creatorUsername'],
      creatorTimezone: json['creatorTimezone'],
      label: json['label'] != null ? LabelResponse.fromJson(json['label']) : null,
      isCompleted: json['isCompleted'],
      unconfirmed: json['unconfirmed'],
      impromptu: json['impromptu'],
      isVirtual: json['isVirtual'],
    );
  }
}
```

#### **User Profile Model**
```dart
// lib/models/user_profile.dart
class UserProfile {
  final bool isSelf;
  final UserHeader header;
  final EventResponse? pinnedImpromptuEvent;
  final List<Badge> badges;

  UserProfile({
    required this.isSelf,
    required this.header,
    this.pinnedImpromptuEvent,
    required this.badges,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      isSelf: json['isSelf'],
      header: UserHeader.fromJson(json['header']),
      pinnedImpromptuEvent: json['pinnedImpromptuEvent'] != null 
          ? EventResponse.fromJson(json['pinnedImpromptuEvent']) 
          : null,
      badges: (json['badges'] as List).map((b) => Badge.fromJson(b)).toList(),
    );
  }
}
```

### **Flutter UI Integration Patterns**

#### **1. Dashboard with Pinned Event**
```dart
// lib/widgets/dashboard_widget.dart
class DashboardWidget extends StatefulWidget {
  @override
  _DashboardWidgetState createState() => _DashboardWidgetState();
}

class _DashboardWidgetState extends State<DashboardWidget> {
  UserProfile? userProfile;
  final GraphQLService _graphqlService = GraphQLService();

  @override
  void initState() {
    super.initState();
    _loadUserProfile();
  }

  Widget build(BuildContext context) {
    return Column(
      children: [
        // Pinned Impromptu Event Card
        if (userProfile?.pinnedImpromptuEvent != null)
          _buildPinnedEventCard(userProfile!.pinnedImpromptuEvent!),
        
        // Other dashboard content...
      ],
    );
  }

  Widget _buildPinnedEventCard(EventResponse event) {
    return Card(
      color: Colors.orange.shade100,
      child: ListTile(
        leading: Icon(Icons.push_pin, color: Colors.orange),
        title: Text(event.name ?? 'Impromptu Event'),
        subtitle: Text('Started: ${_formatTime(event.startTimeUtc)}'),
        trailing: IconButton(
          icon: Icon(Icons.close),
          onPressed: () => _unpinEvent(),
        ),
      ),
    );
  }

  Future<void> _unpinEvent() async {
    final success = await _graphqlService.unpinImpromptuEvent();
    if (success) {
      setState(() {
        userProfile = userProfile?.copyWith(pinnedImpromptuEvent: null);
      });
    }
  }
}
```

#### **2. Quick Add Floating Action Button**
```dart
// lib/widgets/quick_add_fab.dart
class QuickAddFAB extends StatelessWidget {
  final EventService _eventService = EventService();

  @override
  Widget build(BuildContext context) {
    return FloatingActionButton(
      onPressed: _createImpromptuEvent,
      child: Icon(Icons.add),
      tooltip: 'Quick Add Event',
    );
  }

  Future<void> _createImpromptuEvent() async {
    try {
      final event = await _eventService.createImpromptuEvent();
      // Navigate to event details or show success message
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Impromptu event created and pinned!')),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to create event: $e')),
      );
    }
  }
}
```

### **Error Handling for Flutter**

```dart
// lib/models/api_exception.dart
class ApiException implements Exception {
  final String message;
  final int? statusCode;
  final String? errorCode;
  final int? remainingAttempts;
  final int? resetTimeSeconds;

  ApiException(this.message, {this.statusCode, this.errorCode, this.remainingAttempts, this.resetTimeSeconds});

  factory ApiException.fromResponse(http.Response response) {
    final body = jsonDecode(response.body);
    return ApiException(
      body['message'] ?? 'Unknown error',
      statusCode: response.statusCode,
      errorCode: body['errorCode'],
      remainingAttempts: body['remainingAttempts'],
      resetTimeSeconds: body['resetTimeSeconds'],
    );
  }

  bool get isRateLimited => statusCode == 429;
}
```

#### **Rate Limiting Handling**
```dart
// lib/services/auth_service.dart (enhanced with rate limiting)
Future<AuthResponse> login(String username, String password) async {
  try {
    final response = await http.post(
      Uri.parse('${ApiConfig.baseUrl}/auth/login'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'username': username, 'password': password}),
    );
    
    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      await _storeTokens(data['accessToken'], data['refreshToken']);
      return AuthResponse.fromJson(data);
    } else if (response.statusCode == 429) {
      // Rate limited - provide user feedback
      final error = ApiException.fromResponse(response);
      throw RateLimitException(
        'Too many login attempts. ${error.remainingAttempts} attempts remaining. '
        'Try again in ${error.resetTimeSeconds} seconds.',
        remainingAttempts: error.remainingAttempts,
        resetTimeSeconds: error.resetTimeSeconds,
      );
    } else {
      throw ApiException.fromResponse(response);
    }
  } catch (e) {
    throw AuthException('Login failed: $e');
  }
}
```

### **Development Setup for Monorepo**

```yaml
# pubspec.yaml dependencies for API integration
dependencies:
  flutter:
    sdk: flutter
  http: ^1.1.0
  shared_preferences: ^2.2.2
  provider: ^6.1.1  # For state management
  graphql_flutter: ^5.1.2  # Optional: For GraphQL integration
```

This backend provides all the necessary endpoints for a complete Flutter event management app with user authentication, event creation, and dashboard functionality.

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
- **Label**: Event categorization with visual color coding system (base/pastel/metallic variants), time tracking, and system protection
- **Badge**: Multi-label collections with comprehensive time analytics (today/week/month/historical/all-time)
- **EventRecap**: Post-event documentation with media attachments and ordering
- **LabelTimeBucket**: Automatic time analytics aggregation (day/week/month)
- **RecapMedia**: Media management with type classification and duration tracking

### **Label Color System**

The application provides a sophisticated color system for visual event categorization. Each label requires a color from a predefined palette, ensuring consistency and accessibility across the UI.

#### **Color Palette**
- **RED** - `#FF4D4F` (base), `#FFD6D7` (pastel), `#D72631` (metallic)
- **ORANGE** - `#FA8C16` (base), `#FFE0B2` (pastel), `#D46B08` (metallic)
- **YELLOW** - `#FADB14` (base), `#FFF7AE` (pastel), `#D4B106` (metallic)
- **GREEN** - `#52C41A` (base), `#C7EFCF` (pastel), `#237804` (metallic)
- **TEAL** - `#13C2C2` (base), `#A6E6E6` (pastel), `#08979C` (metallic)
- **BLUE** - `#1890FF` (base), `#A3D3FF` (pastel), `#0050B3` (metallic)
- **PURPLE** - `#722ED1` (base), `#D3C6F1` (pastel), `#391085` (metallic)
- **PINK** - `#EB2F96` (base), `#FDCFE8` (pastel), `#C41D7F` (metallic)
- **GRAY** - `#8C8C8C` (base), `#D9D9D9` (pastel), `#595959` (metallic)

#### **Color Variants**
Each color provides three hex values for different event states:
- **Base**: Default appearance for normal events
- **Pastel**: Softer appearance for incomplete or draft events
- **Metallic**: Rich appearance for completed or successful events

#### **API Usage**
```json
// Create label with color
POST /labels
{
  "name": "Work Tasks",
  "color": "BLUE"
}

// Update label color
PATCH /labels/123
{
  "color": "RED"
}

// Response includes color
{
  "id": 123,
  "name": "Work Tasks",
  "color": "BLUE",
  "creatorUsername": "john"
}
```

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
- `GET /labels/{id}` - Get label details with color scheme information
- `POST /labels` - Create new label with name and color from predefined palette (RED, ORANGE, YELLOW, GREEN, TEAL, BLUE, PURPLE, PINK, GRAY)
- `PATCH /labels/{id}` - Update label name and/or color
- `DELETE /labels/{id}` - Delete label and remove color assignment
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

### **Rate Limiting & Protection**
- **Per-IP Rate Limiting**: Configurable limits per client IP address
- **Sliding Window Algorithm**: Time-based rate limiting with automatic reset
- **Multi-Operation Protection**: Different limits for registration, login, password reset, and email verification
- **In-Memory Caching**: Fast rate limit tracking with automatic cleanup
- **Brute Force Prevention**: Protection against credential attacks and abuse
- **Resource Protection**: Prevents spam, flooding, and resource exhaustion

#### **Rate Limiting Policies**
- **Registration**: 5 attempts per hour per IP
- **Login**: 10 attempts per 15 minutes per IP  
- **Password Reset**: 3 requests per hour per IP
- **Email Verification**: 5 attempts per 30 minutes per IP

#### **Rate Limiting Features**
- **Automatic Expiry**: Rate limit data expires automatically after time windows
- **Remaining Attempts API**: Clients can check remaining attempts before hitting limits
- **Reset Time Information**: Provides feedback on when limits will reset
- **Configurable Limits**: Adjustable via application properties
- **Production Ready**: Supports Redis cache for multi-instance deployments

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