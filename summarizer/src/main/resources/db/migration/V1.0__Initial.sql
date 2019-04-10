CREATE TABLE review_summaries(
    id VARCHAR PRIMARY KEY,
    count INTEGER NOT NULL,
    sum REAL NOT NULL,
    avg REAL NOT NULL,
    min REAL NOT NULL,
    max REAL NOT NULL
);
