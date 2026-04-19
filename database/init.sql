CREATE DATABASE IF NOT EXISTS f1setups;
USE f1setups;

-- User table, storing user-attached information and for authentication purposes. The password is stored as a hash, and the salt is used to enhance security.
CREATE TABLE IF NOT EXISTS users (
    id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    salt VARCHAR(32) NOT NULL,
    PRIMARY KEY (id)
);

-- Game version table catalog, storing the different versions of the F1 game for which users can upload setups.
-- This allows for better organization and filtering of setups based on the game version.
-- All the games comprehended by the business logic have the same title (F1), so we can just store the release year as
-- a unique identifier for each game version.
CREATE TABLE IF NOT EXISTS game (
    id INT NOT NULL AUTO_INCREMENT,
    release_year INT NOT NULL,
    PRIMARY KEY (id)
);
-- Track table catalog, storing the different tracks available in the F1 game.
CREATE TABLE IF NOT EXISTS track (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL,
    country VARCHAR(32) NOT NULL,
    PRIMARY KEY (id)
);
-- Team table catalog, storing the different teams available in the F1 game.
CREATE TABLE IF NOT EXISTS team (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    PRIMARY KEY (id)
);

-- Setup metadata table, storing additional information about each setup, such as the title, description, and creation date. This allows users to provide more context about their setups and helps other users find relevant setups based on their preferences.
-- pending

-- Setup table, storing the details of each F1 setup uploaded by users. Each setup is linked to a user via the user_id foreign key.
CREATE TABLE IF NOT EXISTS setup (
    -- Metadata fields
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    game_version_id INT NOT NULL,
    track_id INT NOT NULL,
    team_id INT NOT NULL,
    title VARCHAR(64) NOT NULL,
    annotation TEXT,
    session_type ENUM('PRACTICE', 'QUALIFYING', 'race', 'TIME_TRIAL') NOT NULL DEFAULT 'PRACTICE',
    controller_type ENUM('GAMEPAD', 'WHEEL') NOT NULL DEFAULT 'GAMEPAD',
    is_wet_weather BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Setup fields
    -- Aero
    front_wing INT NOT NULL,
    rear_wing INT NOT NULL,
    -- Transmission
    diff_on_throttle INT NOT NULL,
    diff_off_throttle INT NOT NULL,
    engine_braking INT NOT NULL, -- Account for f1 24, because it's only present on that year release.
    -- Suspension geometry
    front_camber FLOAT NOT NULL,
    rear_camber FLOAT NOT NULL,
    front_toe FLOAT NOT NULL,
    rear_toe FLOAT NOT NULL,
    -- Suspension
    front_suspension INT NOT NULL,
    rear_suspension INT NOT NULL,
    front_anti_roll_bar INT NOT NULL,
    rear_anti_roll_bar INT NOT NULL,
    front_ride_height INT NOT NULL,
    rear_ride_height INT NOT NULL,
    -- Brakes
    brake_pressure INT NOT NULL,
    brake_bias INT NOT NULL,
    -- Tyres
    front_right_pressure FLOAT NOT NULL,
    front_left_pressure FLOAT NOT NULL,
    rear_right_pressure FLOAT NOT NULL,
    rear_left_pressure FLOAT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (game_version_id) REFERENCES game(id),
    FOREIGN KEY (track_id) REFERENCES track(id),
    FOREIGN KEY (team_id) REFERENCES team(id)
    );