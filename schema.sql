-- Campus Complaint & Escalation Tracker
-- Run this in MySQL Workbench / CLI before starting the app

CREATE DATABASE IF NOT EXISTS complaint_tracker;
USE complaint_tracker;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',  -- STUDENT, ADMIN, or AUTHORITY
    designation VARCHAR(100) NULL,
    authority_category VARCHAR(50) NULL,        -- mess, hostel, ... or "all" (Principal)
    authority_level INT NULL,                   -- 0, 1, or 2
    email VARCHAR(120) NULL,
    phone VARCHAR(30) NULL
);

CREATE TABLE IF NOT EXISTS complaints (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    student_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    submitted_date DATETIME NOT NULL,
    deadline DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority_score INT NOT NULL,
    priority_tier VARCHAR(10) NOT NULL,
    escalation_level INT NOT NULL DEFAULT 0,
    assigned_designation VARCHAR(100) NULL,     -- current authority holder (maps to escalation chain)
    level0_designation VARCHAR(100) NULL,       -- Level 0 authority for this complaint's category
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS escalation_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    complaint_id INT NOT NULL,
    escalated_to VARCHAR(50) NOT NULL,
    escalated_date DATETIME NOT NULL,
    reason VARCHAR(255) NOT NULL,
    FOREIGN KEY (complaint_id) REFERENCES complaints(id)
);
