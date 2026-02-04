package org.example.hsspringsecurity.week2.v6jdbc;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JDBC 표준 스키마 시뮬레이션용 인메모리 Repository
 * 
 * JdbcUserDetailsManager가 요구하는 표준 테이블 구조:
 * - users(username, password, enabled)
 * - authorities(username, authority)
 * 
 * WEEK 2 학습 목표:
 * - Spring Security 표준 스키마 이해
 * - JdbcUserDetailsManager의 한계 (고정 스키마) 인식
 */
@Repository
public class InMemoryUserRepository {
    
    private final Map<String, UserData> usersStore = new ConcurrentHashMap<>();
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 초기 테스트 사용자 생성 (JDBC 표준 스키마 형식)
     */
    @PostConstruct
    public void init() {
        // admin 사용자
        UserData admin = new UserData();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("x"));
        admin.setEnabled(true);
        admin.setAuthorities(Arrays.asList("ROLE_ADMIN", "ROLE_USER"));
        usersStore.put("admin", admin);
        
        // user 사용자
        UserData user = new UserData();
        user.setUsername("user");
        user.setPassword(passwordEncoder.encode("user123"));
        user.setEnabled(true);
        user.setAuthorities(Collections.singletonList("ROLE_USER"));
        usersStore.put("user", user);
        
        System.out.println("=== V6 JDBC Style User Repository 초기화 완료 ===");
        System.out.println("Admin: admin / admin123");
        System.out.println("User: user / user123");
    }
    
    /**
     * username으로 사용자 조회
     */
    public Optional<UserData> findByUsername(String username) {
        return Optional.ofNullable(usersStore.get(username));
    }
    
    /**
     * 모든 사용자 조회
     */
    public Collection<UserData> findAll() {
        return usersStore.values();
    }
    
    /**
     * JDBC 표준 스키마의 users + authorities 테이블을 표현하는 데이터 클래스
     */
    @Data
    public static class UserData {
        private String username;
        private String password;
        private boolean enabled;
        private List<String> authorities;  // authorities 테이블의 authority 컬럼
    }
}

