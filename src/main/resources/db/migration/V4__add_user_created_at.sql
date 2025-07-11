-- Migration to add created_at timestamp to users table
-- This field enables tracking when user accounts were created for cleanup operations

-- Add the created_at column to the users table
ALTER TABLE users 
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;

-- Set created_at to current timestamp for existing users
-- This ensures existing users are not immediately eligible for cleanup
UPDATE users 
SET created_at = NOW() 
WHERE created_at IS NULL;

-- Make the column NOT NULL after setting values for existing records
ALTER TABLE users 
ALTER COLUMN created_at SET NOT NULL;

-- Add an index on created_at for efficient cleanup queries
CREATE INDEX idx_users_created_at ON users(created_at);

-- Add an index on the combination of email_verified and created_at for cleanup job performance
CREATE INDEX idx_users_email_verified_created_at ON users(email_verified, created_at);