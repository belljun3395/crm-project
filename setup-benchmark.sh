#!/bin/bash

# Setup script for benchmark testing
# This script creates test users and campaigns in the database

echo "Setting up test data for benchmarking..."

# Database connection details
DB_HOST="localhost"
DB_PORT="13306"
DB_USER="root"
DB_PASSWORD="root"
DB_NAME="crm"

# Function to execute SQL
execute_sql() {
    mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD $DB_NAME -e "$1" 2>/dev/null
}

# Create test users if they don't exist
echo "Creating test users..."
for i in {1..5}; do
    execute_sql "INSERT INTO users (external_id, name, email, created_at) 
                 VALUES ('test-user-$i', 'Test User $i', 'test$i@example.com', NOW()) 
                 ON DUPLICATE KEY UPDATE external_id=external_id;"
done

# Create benchmark campaign if it doesn't exist
echo "Creating benchmark campaign..."
execute_sql "INSERT INTO campaigns (name, properties, created_at) 
             VALUES ('benchmark-campaign', '[{\"key\":\"action\",\"value\":\"\"},{\"key\":\"timestamp\",\"value\":\"\"},{\"key\":\"value\",\"value\":\"\"}]', NOW()) 
             ON DUPLICATE KEY UPDATE name=name;"

echo "Test data setup complete!"
echo ""
echo "Created:"
echo "  - 5 test users (test-user-1 to test-user-5)"
echo "  - 1 benchmark campaign (benchmark-campaign)"
echo ""
