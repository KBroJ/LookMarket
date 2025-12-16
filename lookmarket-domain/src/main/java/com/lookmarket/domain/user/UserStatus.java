package com.lookmarket.domain.user;

/**
 * 사용자 계정 상태를 정의하는 Enum
 */
public enum UserStatus {
    /**
     * 활성화 상태 (정상 사용 가능)
     */
    ACTIVE,

    /**
     * 비활성화 상태 (휴면 계정 등)
     */
    INACTIVE,

    /**
     * 정지 상태 (관리자에 의한 계정 정지)
     */
    SUSPENDED
}
