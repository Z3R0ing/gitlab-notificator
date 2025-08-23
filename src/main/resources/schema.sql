CREATE TABLE IF NOT EXISTS user_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    telegram_id BIGINT NOT NULL UNIQUE,
    gitlab_user_id BIGINT NOT NULL UNIQUE,
    role VARCHAR(10) NOT NULL CHECK (role IN ('DEV', 'LEAD'))
);

CREATE TABLE IF NOT EXISTS analytics_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_id BIGINT NOT NULL UNIQUE,
    time_of_day TIME NOT NULL,
    timezone VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_mapping_telegram_id ON user_mapping(telegram_id);
CREATE INDEX IF NOT EXISTS idx_user_mapping_gitlab_user_id ON user_mapping(gitlab_user_id);
CREATE INDEX IF NOT EXISTS idx_analytics_schedule_chat_id ON analytics_schedule(chat_id);