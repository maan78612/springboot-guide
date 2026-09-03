-- TUTORIAL 18 — migration V2: reference + demo catalog data.
-- Replaces data.sql (deleted this stage). Note: thanks to V1's
-- DEFAULT FALSE, nothing here mentions the soft-delete columns.
-- The first admin is NOT seeded here - it needs a BCrypt hash, so
-- AdminSeeder (tutorial 15) keeps that job.

INSERT INTO author (name) VALUES
  ('Joshua Bloch'),            -- id 1
  ('Robert C. Martin'),        -- id 2
  ('Andrew Hunt'),             -- id 3
  ('Martin Fowler');           -- id 4

INSERT INTO genre (name) VALUES
  ('Programming'),             -- id 1
  ('Java'),                    -- id 2
  ('Software Design');         -- id 3

INSERT INTO book (title, author_id, price, cost_price) VALUES
  ('Effective Java', 1, 54.99, 31.00),
  ('Clean Code', 2, 42.50, 15.00),
  ('The Pragmatic Programmer', 3, 49.95, 28.50),
  ('Clean Architecture', 2, 39.99, 22.00),
  ('Refactoring', 4, 52.00, 30.10);

INSERT INTO book_genre (book_id, genre_id) VALUES
  (1, 1), (1, 2),
  (2, 1), (2, 3),
  (3, 1),
  (4, 3),
  (5, 1), (5, 3);
