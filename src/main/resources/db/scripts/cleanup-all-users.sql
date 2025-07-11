-- Clean All Users Script for Event Planner Development
-- This script removes ALL users and their related data while preserving schema
-- Execute in correct order to handle foreign key constraints

-- WARNING: This will delete ALL user data! Use only in development!

-- Step 1: Delete all media files associated with event recaps
DELETE FROM recap_media 
WHERE event_recap_id IN (
    SELECT id FROM event_recaps 
    WHERE event_id IN (
        SELECT id FROM events 
        WHERE user_id IN (SELECT id FROM users)
    )
);

-- Step 2: Delete all event recaps
DELETE FROM event_recaps 
WHERE event_id IN (
    SELECT id FROM events 
    WHERE user_id IN (SELECT id FROM users)
);

-- Step 3: Delete all events
DELETE FROM events 
WHERE user_id IN (SELECT id FROM users);

-- Step 4: Delete all recurring events
DELETE FROM recurring_events 
WHERE user_id IN (SELECT id FROM users);

-- Step 5: Delete all user badges
DELETE FROM badges 
WHERE user_id IN (SELECT id FROM users);

-- Step 6: Delete all user labels
DELETE FROM labels 
WHERE user_id IN (SELECT id FROM users);

-- Step 7: Delete all label time buckets
DELETE FROM label_time_buckets 
WHERE user_id IN (SELECT id FROM users);

-- Step 8: Delete password reset tokens
DELETE FROM password_reset_tokens 
WHERE user_id IN (SELECT id FROM users);

-- Step 9: Delete refresh tokens
DELETE FROM refresh_tokens 
WHERE user_id IN (SELECT id FROM users);

-- Step 10: Finally, delete all users
DELETE FROM users;

-- Step 11: Reset sequences to start from 1 (optional)
-- ALTER SEQUENCE users_id_seq RESTART WITH 1;
-- ALTER SEQUENCE events_id_seq RESTART WITH 1;
-- ALTER SEQUENCE recurring_events_id_seq RESTART WITH 1;
-- ALTER SEQUENCE badges_id_seq RESTART WITH 1;
-- ALTER SEQUENCE labels_id_seq RESTART WITH 1;
-- ALTER SEQUENCE event_recaps_id_seq RESTART WITH 1;
-- ALTER SEQUENCE recap_media_id_seq RESTART WITH 1;
-- ALTER SEQUENCE label_time_buckets_id_seq RESTART WITH 1;
-- ALTER SEQUENCE password_reset_tokens_id_seq RESTART WITH 1;
-- ALTER SEQUENCE refresh_tokens_id_seq RESTART WITH 1;

-- Display cleanup summary
SELECT 
    'Database cleanup completed successfully!' as message,
    NOW() as timestamp;