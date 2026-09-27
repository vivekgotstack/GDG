CREATE TABLE members (
    id VARCHAR(40) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    color VARCHAR(20) NOT NULL
);
CREATE TABLE availability (
    member_id VARCHAR(40) NOT NULL REFERENCES members(id),
    day_of_week VARCHAR(12) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CHECK (start_time < end_time)
);
CREATE TABLE rooms (
    id VARCHAR(40) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    location VARCHAR(100) NOT NULL,
    CHECK (open_time < close_time)
);
CREATE TABLE room_bookings (
    id VARCHAR(40) PRIMARY KEY,
    room_id VARCHAR(40) NOT NULL REFERENCES rooms(id),
    day_of_week VARCHAR(12) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    source VARCHAR(20) NOT NULL,
    CHECK (start_time < end_time)
);
CREATE INDEX bookings_room_day ON room_bookings(room_id, day_of_week);
