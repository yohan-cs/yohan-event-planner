# GraphQL API Documentation

This document provides examples and usage patterns for the Event Planner GraphQL API.

## Overview

The GraphQL API provides queries for retrieving user profiles and event data, plus mutations for managing users, events, badges, and event recaps. The API uses custom scalar types for dates and implements a flexible partial update pattern.

## Custom Scalars

- `Date`: ISO-8601 date format (e.g., `2025-06-23`)
- `DateTime`: ISO-8601 date-time with timezone offset (e.g., `2025-06-23T14:30:00-07:00`)

## Label Color System

Labels use a predefined color enum (`LabelColor`) with the following values:
- `RED`, `ORANGE`, `YELLOW`, `GREEN`, `TEAL`, `BLUE`, `PURPLE`, `PINK`, `GRAY`

Each color provides three hex variants for different event states:
- **Base**: Default appearance
- **Pastel**: For incomplete/draft events  
- **Metallic**: For completed/successful events

Colors are mandatory when creating labels and optional when updating them.

## Example Queries

### Get User Profile
```graphql
query GetUserProfile($username: String!) {
  userProfile(username: $username) {
    isSelf
    header {
      username
      firstName
      lastName
      bio
      profilePictureUrl
    }
    badges {
      id
      name
      sortOrder
      timeStats {
        minutesToday
        minutesThisWeek
        minutesThisMonth
      }
      labels {
        id
        name
        color
      }
    }
    weekView(anchorDate: "2025-06-23") {
      days {
        date
        events {
          id
          name
          startTimeUtc
          endTimeUtc
          durationMinutes
          isCompleted
          label {
            id
            name
            color
          }
        }
      }
    }
  }
}
```

### Get Event Recap
```graphql
query GetEventRecap($eventId: ID!) {
  eventRecap(eventId: $eventId) {
    id
    eventName
    username
    date
    durationMinutes
    labelName
    notes
    media {
      id
      mediaUrl
      mediaType
      durationSeconds
      mediaOrder
    }
  }
}
```

## Example Mutations

### Update User Header
```graphql
mutation UpdateUserHeader($input: UpdateUserHeaderInput!) {
  updateUserHeader(input: $input) {
    username
    firstName
    lastName
    bio
    profilePictureUrl
  }
}

# Variables:
{
  "input": {
    "bio": "Software developer passionate about productivity tools",
    "profilePictureUrl": "https://example.com/avatar.jpg"
  }
}
```

### Update Event (Partial Update)
```graphql
mutation UpdateEvent($id: ID!, $input: UpdateEventInput!) {
  updateEvent(id: $id, input: $input) {
    id
    name
    description
    startTimeUtc
    endTimeUtc
    isCompleted
    label {
      id
      name
      color
    }
  }
}

# Variables (only updates name and completion status):
{
  "id": "123",
  "input": {
    "name": { "value": "Updated Event Name" },
    "isCompleted": { "value": true }
  }
}
```

### Create Event Recap with Media
```graphql
mutation AddEventRecap($input: AddEventRecapInput!) {
  addEventRecap(input: $input) {
    id
    eventName
    notes
    media {
      id
      mediaUrl
      mediaType
      mediaOrder
    }
  }
}

# Variables:
{
  "input": {
    "eventId": "456",
    "recapName": "Morning Workout Session",
    "notes": "Great cardio session at the gym",
    "isUnconfirmed": false,
    "media": [
      {
        "mediaUrl": "https://example.com/workout-photo.jpg",
        "mediaType": "IMAGE",
        "mediaOrder": 1
      },
      {
        "mediaUrl": "https://example.com/workout-video.mp4",
        "mediaType": "VIDEO",
        "durationSeconds": 45,
        "mediaOrder": 2
      }
    ]
  }
}
```

### Create Label with Color
```graphql
mutation CreateLabel($input: CreateLabelInput!) {
  createLabel(input: $input) {
    id
    name
    color
  }
}

# Variables:
{
  "input": {
    "name": "Work Tasks",
    "color": "BLUE"
  }
}
```

### Update Label (Partial Update)
```graphql
mutation UpdateLabel($id: ID!, $input: UpdateLabelInput!) {
  updateLabel(id: $id, input: $input) {
    id
    name
    color
  }
}

# Variables (update only color):
{
  "id": "123",
  "input": {
    "color": { "value": "RED" }
  }
}

# Variables (update both name and color):
{
  "id": "123", 
  "input": {
    "name": { "value": "Updated Label Name" },
    "color": { "value": "GREEN" }
  }
}
```

### Delete Label
```graphql
mutation DeleteLabel($id: ID!) {
  deleteLabel(id: $id)
}

# Variables:
{
  "id": "123"
}
```

### Reorder Badge Labels
```graphql
mutation ReorderBadgeLabels($badgeId: ID!, $labelOrder: [ID!]!) {
  reorderBadgeLabels(badgeId: $badgeId, labelOrder: $labelOrder)
}

# Variables:
{
  "badgeId": "789",
  "labelOrder": ["label1", "label3", "label2"]
}
```

## Partial Update Pattern

The API uses `UpdateFieldInput` for optional field updates in mutations like `updateEvent`. This allows you to:

- **Omit fields**: Don't include them in the input to leave unchanged
- **Set to null**: Include `{ "value": null }` to explicitly clear a field
- **Update with value**: Include `{ "value": "new value" }` to update

### Examples:

```graphql
# Only update the event name, leave other fields unchanged
{
  "input": {
    "name": { "value": "New Event Name" }
  }
}

# Update name and clear description
{
  "input": {
    "name": { "value": "New Event Name" },
    "description": { "value": null }
  }
}

# Update multiple fields
{
  "input": {
    "name": { "value": "Updated Name" },
    "isCompleted": { "value": true },
    "labelId": { "value": "new-label-id" }
  }
}
```

## Error Handling

GraphQL errors are returned in the standard GraphQL error format:

```json
{
  "errors": [
    {
      "message": "Badge not found",
      "path": ["updateBadge"],
      "extensions": {
        "code": "BADGE_NOT_FOUND"
      }
    }
  ]
}
```

Common error scenarios:
- **Ownership validation**: Users can only modify their own resources
- **Resource not found**: Invalid IDs return not found errors
- **Validation errors**: Invalid input data triggers validation errors
- **Authentication required**: Unauthenticated requests are rejected

## Media Types

The `RecapMediaType` enum supports:
- `IMAGE`: Static image files (jpg, png, gif, etc.)
- `VIDEO`: Video files (mp4, avi, mov, etc.)  
- `AUDIO`: Audio files (mp3, wav, etc.)

For video and audio media, include `durationSeconds` to track content length.