--
-- Migration to update timestamp precision and sync schema with current domain model
--

-- Update existing events table to match current domain model structure
ALTER TABLE public.events 
    ALTER COLUMN starttime DROP NOT NULL,
    ALTER COLUMN endtime DROP NOT NULL,
    ALTER COLUMN starttimezone DROP NOT NULL,
    ALTER COLUMN endtimezone DROP NOT NULL,
    ALTER COLUMN name DROP NOT NULL;

-- Add missing columns from current domain model
ALTER TABLE public.events 
    ADD COLUMN IF NOT EXISTS durationminutes integer,
    ADD COLUMN IF NOT EXISTS label_id bigint,
    ADD COLUMN IF NOT EXISTS recurring_event_id bigint,
    ADD COLUMN IF NOT EXISTS iscompleted boolean DEFAULT false NOT NULL,
    ADD COLUMN IF NOT EXISTS unconfirmed boolean DEFAULT true NOT NULL;

-- Update timestamp precision from timestamp(6) to timestamp(0) to remove microseconds
ALTER TABLE public.events 
    ALTER COLUMN starttime TYPE timestamp(0) with time zone,
    ALTER COLUMN endtime TYPE timestamp(0) with time zone;

-- Truncate existing timestamp data to minute precision
UPDATE public.events 
SET starttime = date_trunc('minute', starttime)
WHERE starttime IS NOT NULL;

UPDATE public.events 
SET endtime = date_trunc('minute', endtime)
WHERE endtime IS NOT NULL;

-- Create missing tables that exist in current domain model
CREATE TABLE IF NOT EXISTS public.labels (
    id bigint NOT NULL,
    creator_id bigint NOT NULL,
    name character varying(50) NOT NULL,
    color character varying(7) NOT NULL,
    CONSTRAINT labels_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.recurring_events (
    id bigint NOT NULL,
    creator_id bigint NOT NULL,
    name character varying(50) NOT NULL,
    rrule character varying(255),
    CONSTRAINT recurring_events_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.badges (
    id bigint NOT NULL,
    creator_id bigint NOT NULL,
    name character varying(50) NOT NULL,
    description character varying(255),
    CONSTRAINT badges_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.event_recaps (
    id bigint NOT NULL,
    event_id bigint NOT NULL,
    recap_text character varying(1000),
    CONSTRAINT event_recaps_pkey PRIMARY KEY (id),
    CONSTRAINT event_recaps_event_id_key UNIQUE (event_id)
);

CREATE TABLE IF NOT EXISTS public.recap_media (
    id bigint NOT NULL,
    event_recap_id bigint NOT NULL,
    media_url character varying(500) NOT NULL,
    media_type character varying(50) NOT NULL,
    CONSTRAINT recap_media_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.label_time_buckets (
    id bigint NOT NULL,
    label_id bigint NOT NULL,
    bucket_date date NOT NULL,
    total_minutes integer NOT NULL DEFAULT 0,
    CONSTRAINT label_time_buckets_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.password_reset_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token character varying(255) NOT NULL,
    expires_at timestamp(0) with time zone NOT NULL,
    used boolean DEFAULT false NOT NULL,
    CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT password_reset_tokens_token_key UNIQUE (token)
);

-- Add sequences for new tables
CREATE SEQUENCE IF NOT EXISTS public.labels_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS public.recurring_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS public.badges_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS public.event_recaps_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS public.recap_media_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS public.label_time_buckets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS public.password_reset_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Link sequences to tables
ALTER TABLE public.labels ALTER COLUMN id SET DEFAULT nextval('public.labels_id_seq');
ALTER TABLE public.recurring_events ALTER COLUMN id SET DEFAULT nextval('public.recurring_events_id_seq');
ALTER TABLE public.badges ALTER COLUMN id SET DEFAULT nextval('public.badges_id_seq');
ALTER TABLE public.event_recaps ALTER COLUMN id SET DEFAULT nextval('public.event_recaps_id_seq');
ALTER TABLE public.recap_media ALTER COLUMN id SET DEFAULT nextval('public.recap_media_id_seq');
ALTER TABLE public.label_time_buckets ALTER COLUMN id SET DEFAULT nextval('public.label_time_buckets_id_seq');
ALTER TABLE public.password_reset_tokens ALTER COLUMN id SET DEFAULT nextval('public.password_reset_tokens_id_seq');

-- Add foreign key constraints
ALTER TABLE public.events 
    ADD CONSTRAINT IF NOT EXISTS fk_events_label_id FOREIGN KEY (label_id) REFERENCES public.labels(id),
    ADD CONSTRAINT IF NOT EXISTS fk_events_recurring_event_id FOREIGN KEY (recurring_event_id) REFERENCES public.recurring_events(id);

ALTER TABLE public.labels 
    ADD CONSTRAINT IF NOT EXISTS fk_labels_creator_id FOREIGN KEY (creator_id) REFERENCES public.users(id);

ALTER TABLE public.recurring_events 
    ADD CONSTRAINT IF NOT EXISTS fk_recurring_events_creator_id FOREIGN KEY (creator_id) REFERENCES public.users(id);

ALTER TABLE public.badges 
    ADD CONSTRAINT IF NOT EXISTS fk_badges_creator_id FOREIGN KEY (creator_id) REFERENCES public.users(id);

ALTER TABLE public.event_recaps 
    ADD CONSTRAINT IF NOT EXISTS fk_event_recaps_event_id FOREIGN KEY (event_id) REFERENCES public.events(id);

ALTER TABLE public.recap_media 
    ADD CONSTRAINT IF NOT EXISTS fk_recap_media_event_recap_id FOREIGN KEY (event_recap_id) REFERENCES public.event_recaps(id);

ALTER TABLE public.label_time_buckets 
    ADD CONSTRAINT IF NOT EXISTS fk_label_time_buckets_label_id FOREIGN KEY (label_id) REFERENCES public.labels(id);

ALTER TABLE public.password_reset_tokens 
    ADD CONSTRAINT IF NOT EXISTS fk_password_reset_tokens_user_id FOREIGN KEY (user_id) REFERENCES public.users(id);