-- Update labels table schema
-- 1. Increase name length from 50 to 100 characters
-- 2. Remove unused color column

-- Increase name column length to 100 characters
ALTER TABLE public.labels ALTER COLUMN name TYPE character varying(100);

-- Remove the unused color column
ALTER TABLE public.labels DROP COLUMN IF EXISTS color;