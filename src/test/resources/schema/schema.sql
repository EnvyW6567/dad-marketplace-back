DROP TABLE IF EXISTS users;

CREATE TABLE IF NOT EXISTS users
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    discord_id   VARCHAR(20) UNIQUE NOT NULL,
    username     VARCHAR(32)        NOT NULL,
    display_name VARCHAR(32),
    email        VARCHAR(100),
    avatar_url   VARCHAR(255),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_discord_id (discord_id)
);
