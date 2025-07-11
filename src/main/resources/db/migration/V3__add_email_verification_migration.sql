-- Migration to handle existing users for email verification system
-- This script ensures all existing users have verified emails so they can continue to log in

-- Mark all existing users as email verified
-- This is safe because these users were created before email verification was required
UPDATE users 
SET email_verified = true, 
    updated_at = NOW()
WHERE email_verified = false;

-- Add index for email verification queries (if not already exists)
CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users (email_verified);

-- Add index for email verification token queries
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_id ON email_verification_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_token ON email_verification_tokens (token);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_expires_at ON email_verification_tokens (expires_at);