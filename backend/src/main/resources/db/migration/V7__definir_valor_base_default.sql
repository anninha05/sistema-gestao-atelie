ALTER TABLE pecas ALTER COLUMN valor_base SET DEFAULT 0;

UPDATE pecas SET valor_base = 0 WHERE valor_base IS NULL;