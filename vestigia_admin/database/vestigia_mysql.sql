-- =========================================================
-- VESTIGIA - Lost and Found Management System
-- MySQL / MariaDB schema for XAMPP
-- Converted from the PostgreSQL schema (users, sessions,
-- found_items, lost_items, claims)
--
-- Import via phpMyAdmin: create a database named `vestigia`,
-- then Import > choose this file.
-- =========================================================

CREATE DATABASE IF NOT EXISTS vestigia CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE vestigia;

-- =========================================================
-- USERS
-- =========================================================
CREATE TABLE users (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    middle_initial VARCHAR(5)   NULL,
    student_id     VARCHAR(50)  NOT NULL UNIQUE,
    contact_number VARCHAR(30)  NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    photo_path     VARCHAR(255) NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_admin       TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- =========================================================
-- SESSIONS  (used by the Android app's API login, not the web admin panel)
-- =========================================================
CREATE TABLE sessions (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    token      VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- FOUND_ITEMS
-- =========================================================
CREATE TABLE found_items (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    item_name   VARCHAR(150) NOT NULL,
    description TEXT NULL,
    category    VARCHAR(100) NULL,
    location    VARCHAR(150) NOT NULL,
    date_found  DATE NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'Unclaimed',
    image_path  VARCHAR(255) NULL,
    reported_by INT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT found_items_status_check CHECK (status IN ('Unclaimed', 'Claimed')),
    CONSTRAINT found_items_reported_by_fkey FOREIGN KEY (reported_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_found_items_date ON found_items (date_found);
CREATE INDEX idx_found_items_status ON found_items (status);

-- =========================================================
-- LOST_ITEMS
-- =========================================================
CREATE TABLE lost_items (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    item_name           VARCHAR(150) NOT NULL,
    description         TEXT NULL,
    category            VARCHAR(100) NULL,
    last_seen_location  VARCHAR(150) NOT NULL,
    date_lost           DATE NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'Missing',
    image_path          VARCHAR(255) NULL,
    reported_by         INT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT lost_items_status_check CHECK (status IN ('Missing', 'Found', 'Closed')),
    CONSTRAINT lost_items_reported_by_fkey FOREIGN KEY (reported_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_lost_items_date ON lost_items (date_lost);
CREATE INDEX idx_lost_items_status ON lost_items (status);

-- =========================================================
-- CLAIMS  (claims are only filed against found_items)
-- =========================================================
CREATE TABLE claims (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    item_id            INT NOT NULL,
    claimed_by         INT NOT NULL,
    proof_description  TEXT NOT NULL,
    proof_photo_path   VARCHAR(255) NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'Pending',
    claimed_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT claims_status_check CHECK (status IN ('Pending', 'Approved', 'Rejected')),
    CONSTRAINT claims_item_id_fkey FOREIGN KEY (item_id) REFERENCES found_items(id) ON DELETE CASCADE,
    CONSTRAINT claims_claimed_by_fkey FOREIGN KEY (claimed_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- Seed one admin account so you can log in immediately.
-- Login: admin@vestigia.local / Admin123!
-- (password_hash generated with PHP's password_hash(), bcrypt)
-- =========================================================
INSERT INTO users (first_name, last_name, student_id, email, password_hash, is_admin)
VALUES (
    'System', 'Admin', 'ADMIN-0001', 'admin@vestigia.local',
    '$2y$10$upXF1E.vb6zk1PxxOd3eiOjlTkgRw6XqcnLugbHY1wbuDtW1PkPR2', -- Admin123!
    1
);
