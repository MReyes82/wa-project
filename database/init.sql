CREATE DATABASE IF NOT EXISTS f1setups;
USE f1setups;

-- first table of the database
CREATE TABLE IF NOT EXISTS users (
    id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    salt VARCHAR(32) NOT NULL,
    PRIMARY KEY (id)
);
/*
-- insert some dummy data into the users table
INSERT INTO users (username, email, password) VALUES
('user1', 'hello@gmail.com', 'password1'),
('user2', 'johndoe@outlook.com', 'password2'),
('user3', 'lewisham@yahoo.com', 'password3');
 */