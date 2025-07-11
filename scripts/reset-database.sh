#!/bin/bash

# Database Reset Script for Event Planner Development
# This script completely resets the database by recreating the Docker containers

echo "🔄 Resetting Event Planner Database..."

# Stop all containers
echo "Stopping containers..."
docker-compose down

# Remove database volume (optional - uncomment if you want to clear volume data)
# echo "Removing database volume..."
# docker volume rm event-planner_postgres_data 2>/dev/null || true

# Start database container first
echo "Starting database container..."
docker-compose up -d db

# Wait for database to be ready
echo "Waiting for database to be ready..."
sleep 5

# Check if database is ready
until docker-compose exec db pg_isready -U postgres; do
    echo "Database is not ready yet. Waiting..."
    sleep 2
done

echo "Database is ready!"

# Start the application
echo "Starting application..."
docker-compose up -d app

echo "✅ Database reset complete!"
echo "🚀 Application should be available at http://localhost:8080"
echo "📊 Database is fresh and ready for testing"