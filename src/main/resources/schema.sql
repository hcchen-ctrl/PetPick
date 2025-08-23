-- adopt_posts
CREATE TABLE IF NOT EXISTS adopt_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(120) NOT NULL,
    species VARCHAR(40),
    breed VARCHAR(80),
    sex VARCHAR(10),
    age VARCHAR(20),
    body_type VARCHAR(20),
    color VARCHAR(40),
    city VARCHAR(40),
    district VARCHAR(40),
    description TEXT,
    images JSON NULL, -- 先簡單放字串陣列
    source_type ENUM('user', 'platform') NOT NULL,
    status ENUM(
        'pending',
        'approved',
        'on_hold',
        'closed',
        'cancelled',
        'rejected'
    ) NOT NULL DEFAULT 'pending',
    posted_by_member_id BIGINT NULL,
    posted_by_employee_id BIGINT NULL,
    contact_name VARCHAR(60),
    contact_phone VARCHAR(40),
    contact_line VARCHAR(60),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- adopt_applications（只對 platform 文章）
CREATE TABLE IF NOT EXISTS adopt_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    message VARCHAR(500),
    status ENUM(
        'pending',
        'approved',
        'rejected',
        'cancelled'
    ) NOT NULL DEFAULT 'pending',
    reviewed_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_post FOREIGN KEY (post_id) REFERENCES adopt_posts (id)
);

-- adopt_feedbacks（平台案件通過後才建）
CREATE TABLE IF NOT EXISTS adopt_feedbacks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    adopter_id BIGINT NOT NULL,
    feedback_date DATE,
    note TEXT,
    image VARCHAR(255),
    verified_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fb_post FOREIGN KEY (post_id) REFERENCES adopt_posts (id)
);

-- 聯絡資訊顯示記錄（反爬）
CREATE TABLE IF NOT EXISTS contact_view_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);