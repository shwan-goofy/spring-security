package org.example.hsspringsecurity.common.domain;

import lombok.Data;

/**
 * Customer 도메인 모델 (POJO)
 * WEEK 2: 인메모리 Repository 패턴 학습용
 */
@Data
public class Customer {
    
    private Long id;
    
    private String email;  // username으로 사용
    
    private String pwd;    // BCrypt 해시 저장
    
    private String role;   // "ROLE_USER", "ROLE_ADMIN"
    
    private String name;
}

