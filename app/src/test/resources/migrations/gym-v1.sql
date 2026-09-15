PRAGMA user_version = 1;

CREATE TABLE Program (
    _id INTEGER PRIMARY KEY,
    _type TEXT,
    name TEXT,
    segments TEXT
);

CREATE TABLE Segment (
    _id INTEGER PRIMARY KEY,
    _type TEXT,
    difficulty TEXT,
    distance INTEGER,
    duration INTEGER,
    strokes INTEGER,
    energy INTEGER,
    speed INTEGER,
    strokeRate INTEGER,
    pulse INTEGER,
    power INTEGER
);

CREATE TABLE Workout (
    _id INTEGER PRIMARY KEY,
    _type TEXT,
    program INTEGER,
    location TEXT,
    start INTEGER,
    duration INTEGER,
    distance INTEGER,
    strokes INTEGER,
    energy INTEGER,
    evaluate INTEGER
);

CREATE TABLE Snapshot (
    _id INTEGER PRIMARY KEY,
    _type TEXT,
    workout INTEGER,
    difficulty TEXT,
    distance INTEGER,
    strokes INTEGER,
    energy INTEGER,
    speed INTEGER,
    pulse INTEGER,
    strokeRate INTEGER,
    strokeRatio INTEGER,
    power INTEGER
);

INSERT INTO Segment VALUES
    (1, NULL, 'HARD', 2000, 0, 0, 0, 0, 26, 0, 0),
    (2, NULL, 'REST', 0, 60, 0, 0, 0, 0, 0, 0),
    (3, NULL, 'HARD', 0, 300, 0, 0, 0, 0, 0, 180);

INSERT INTO Program VALUES
    (1, NULL, 'Legacy 2K', '{1}'),
    (2, NULL, 'Legacy intervals', '{3}{2}{3}{2}');

INSERT INTO Workout VALUES
    (1, NULL, 1, NULL, 1700000000000, 500, 2000, 220, 160, 1),
    (2, NULL, 2, NULL, 1700100000000, 720, 2500, 300, 210, 1);

INSERT INTO Snapshot VALUES
    (1, NULL, 1, 'HARD', 4, 1, 0, 400, 140, 26, 45, 180),
    (2, NULL, 1, 'HARD', 2000, 220, 160, 400, 155, 28, 48, 210),
    (3, NULL, 2, 'REST', 1000, 120, 80, 0, 135, 10, 20, 0);
