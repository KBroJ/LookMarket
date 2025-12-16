package com.lookmarket.domain.user;

/**
 * 사용자 역할을 정의하는 Enum
 */
public enum UserRole {
    /**
     * 일반 고객 (상품 구매자)
     */
    CUSTOMER,

    /**
     * 판매자 (상품 등록 및 관리 가능)
     */
    SELLER,

    /**
     * 관리자 (전체 시스템 관리 권한)
     */
    ADMIN
}
