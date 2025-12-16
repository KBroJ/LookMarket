-- V1__create_users_table.sql
-- LookMarket 사용자 테이블 생성 마이그레이션

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 ID (PK)',
    email VARCHAR(255) NOT NULL UNIQUE COMMENT '이메일 (로그인 ID, 유니크)',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt 암호화된 비밀번호',
    name VARCHAR(100) NOT NULL COMMENT '사용자 이름',
    phone_number VARCHAR(20) COMMENT '전화번호',
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER' COMMENT '역할 (CUSTOMER, SELLER, ADMIN)',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '계정 상태 (ACTIVE, INACTIVE, SUSPENDED)',
    created_at DATETIME NOT NULL COMMENT '생성일시',
    updated_at DATETIME NOT NULL COMMENT '수정일시',

    INDEX idx_user_email (email),
    INDEX idx_user_status (status),
    INDEX idx_user_role (role),

    CONSTRAINT chk_role CHECK (role IN ('CUSTOMER', 'SELLER', 'ADMIN')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 테이블';
