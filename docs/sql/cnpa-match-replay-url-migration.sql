-- CNPA: Match replay URL for public schedule CTA.
-- Additive only. Stores an external http/https video link.
SET NAMES utf8mb4;

ALTER TABLE asket_match
  ADD COLUMN 
eplay_url VARCHAR(500) DEFAULT NULL
    COMMENT 'match replay external url, http/https only' AFTER 
emark;
