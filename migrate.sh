#!/bin/bash
# Run Flyway migrations manually
echo "Running Flyway migrations..."
docker-compose exec app java -Dspring.flyway.enabled=true -jar app.jar --flyway.migrate
echo "Migrations completed!"