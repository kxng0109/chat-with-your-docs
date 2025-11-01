-- Enable the vector extension for similarity search
CREATE EXTENSION IF NOT EXISTS vector;

-- Create the schema that Spring AI will use
CREATE SCHEMA IF NOT EXISTS vector_store;

-- Optional: Verify the extension is installed
SELECT * FROM pg_extension WHERE extname = 'vector';