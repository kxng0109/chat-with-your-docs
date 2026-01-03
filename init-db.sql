-- Enable required extensions for pgvector and Spring AI
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create the vector_store schema that Spring AI uses
-- Note: Your config uses schema-name=vector_store
CREATE SCHEMA IF NOT EXISTS vector_store;

-- Grant permissions (adjust username as needed)
GRANT ALL PRIVILEGES ON SCHEMA vector_store TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA vector_store TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA vector_store TO postgres;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA vector_store
    GRANT ALL ON TABLES TO postgres;

ALTER DEFAULT PRIVILEGES IN SCHEMA vector_store
    GRANT ALL ON SEQUENCES TO postgres;

-- ========================================
-- Application Tables (public schema)
-- ========================================

-- Chat Sessions Table
CREATE TABLE IF NOT EXISTS public.chat_sessions (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    session_id VARCHAR(255) NOT NULL UNIQUE,
                                                    name VARCHAR(255),
                                                    description VARCHAR(1000),
                                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_session_id ON public.chat_sessions(session_id);
CREATE INDEX IF NOT EXISTS idx_sessions_created_at ON public.chat_sessions(created_at DESC);

-- Session Documents Table
CREATE TABLE IF NOT EXISTS public.session_documents (
                                                        id BIGSERIAL PRIMARY KEY,
                                                        session_id BIGINT NOT NULL,
                                                        original_filename VARCHAR(255) NOT NULL,
                                                        content_type VARCHAR(100),
                                                        file_size BIGINT,
                                                        chunk_count INTEGER,
                                                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                                        error_message VARCHAR(1000),
                                                        processing_time_ms BIGINT,
                                                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                        CONSTRAINT fk_document_session
                                                            FOREIGN KEY (session_id)
                                                                REFERENCES public.chat_sessions(id)
                                                                ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_documents_session_id ON public.session_documents(session_id);
CREATE INDEX IF NOT EXISTS idx_documents_status ON public.session_documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON public.session_documents(created_at DESC);

-- Chat Messages Table
CREATE TABLE IF NOT EXISTS public.chat_messages (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    session_id BIGINT NOT NULL,
                                                    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
                                                    content TEXT NOT NULL,
                                                    sources TEXT,
                                                    processing_time_ms BIGINT,
                                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                    CONSTRAINT fk_message_session
                                                        FOREIGN KEY (session_id)
                                                            REFERENCES public.chat_sessions(id)
                                                            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_messages_session_id ON public.chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON public.chat_messages(created_at ASC);
CREATE INDEX IF NOT EXISTS idx_messages_role ON public.chat_messages(role);

-- ========================================
-- Trigger for Auto-Updating Timestamps
-- ========================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for chat_sessions
DROP TRIGGER IF EXISTS update_chat_sessions_updated_at ON public.chat_sessions;
CREATE TRIGGER update_chat_sessions_updated_at
    BEFORE UPDATE ON public.chat_sessions
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- ========================================
-- Post-Initialization: Metadata Indexes
-- ========================================
-- These indexes will be created AFTER Spring AI creates the vector_store table
-- Run this function after your first application startup:

CREATE OR REPLACE FUNCTION create_vector_store_metadata_indexes()
    RETURNS void AS $$
BEGIN
    -- Only create indexes if the table exists
    IF EXISTS (
        SELECT FROM pg_tables
        WHERE schemaname = 'vector_store'
          AND tablename = 'vector_store'
    ) THEN
        -- Check and create GIN index for metadata filtering
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE schemaname = 'vector_store'
              AND tablename = 'vector_store'
              AND indexname = 'vector_store_metadata_gin_idx'
        ) THEN
            EXECUTE 'CREATE INDEX vector_store_metadata_gin_idx
                     ON vector_store.vector_store
                     USING GIN ((metadata::jsonb))';
            RAISE NOTICE 'Created GIN index for metadata filtering';
        END IF;

        -- Check and create sessionId index (CRITICAL for your use case)
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE schemaname = 'vector_store'
              AND tablename = 'vector_store'
              AND indexname = 'vector_store_session_id_idx'
        ) THEN
            EXECUTE 'CREATE INDEX vector_store_session_id_idx
                     ON vector_store.vector_store
                     (((metadata->>''sessionId'')::text))';
            RAISE NOTICE 'Created sessionId index for session-based filtering';
        END IF;

        RAISE NOTICE 'All metadata indexes created successfully!';
    ELSE
        RAISE WARNING 'vector_store.vector_store table does not exist yet. Run this function after first app startup.';
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Create a helper function to check vector store status
CREATE OR REPLACE FUNCTION check_vector_store_status()
    RETURNS TABLE(
                     table_exists boolean,
                     embedding_dimensions integer,
                     total_vectors bigint,
                     index_type text,
                     has_metadata_index boolean,
                     has_session_index boolean
                 ) AS $$
BEGIN
    RETURN QUERY
        SELECT
            EXISTS(SELECT FROM pg_tables WHERE schemaname = 'vector_store' AND tablename = 'vector_store') as table_exists,
            (SELECT atttypmod - 4 FROM pg_attribute
             WHERE attrelid = 'vector_store.vector_store'::regclass
               AND attname = 'embedding')::integer as embedding_dimensions,
            (SELECT COUNT(*) FROM vector_store.vector_store)::bigint as total_vectors,
            (SELECT indexdef FROM pg_indexes
             WHERE schemaname = 'vector_store'
               AND tablename = 'vector_store'
               AND indexname LIKE '%embedding%'
             LIMIT 1) as index_type,
            EXISTS(SELECT 1 FROM pg_indexes
                   WHERE schemaname = 'vector_store'
                     AND tablename = 'vector_store'
                     AND indexname = 'vector_store_metadata_gin_idx') as has_metadata_index,
            EXISTS(SELECT 1 FROM pg_indexes
                   WHERE schemaname = 'vector_store'
                     AND tablename = 'vector_store'
                     AND indexname = 'vector_store_session_id_idx') as has_session_index;
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- Verification Queries
-- ========================================

-- Verify extensions are installed
SELECT extname, extversion
FROM pg_extension
WHERE extname IN ('vector', 'hstore', 'uuid-ossp');

-- Verify schemas exist
SELECT schema_name
FROM information_schema.schemata
WHERE schema_name IN ('public', 'vector_store');

-- Show all tables in both schemas
SELECT
    table_schema,
    table_name,
    (SELECT COUNT(*)
     FROM information_schema.columns c
     WHERE c.table_schema = t.table_schema
       AND c.table_name = t.table_name) as column_count
FROM information_schema.tables t
WHERE table_schema IN ('public', 'vector_store')
ORDER BY table_schema, table_name;

-- ========================================
-- USAGE INSTRUCTIONS
-- ========================================
-- 1. This script creates extensions, schemas, and application tables
-- 2. On first app startup, Spring AI creates vector_store.vector_store
--    with the correct dimensions for your embedding model
-- 3. After first startup, run: SELECT create_vector_store_metadata_indexes();
-- 4. Check status anytime with: SELECT * FROM check_vector_store_status();
-- ========================================