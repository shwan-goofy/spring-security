package org.example.hsspringsecurity.week2.v6jdbc;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * V6: JDBC 스타일 인증 컨트롤러
 * 
 * 테스트 시나리오:
 * 1. curl http://localhost:8080/v6/jdbc/public
 *    → 200 OK (인증 불필요)
 * 
 * 2. curl -u admin:admin123 http://localhost:8080/v6/jdbc/secured
 *    → 200 OK (JDBC 스타일 users 테이블에서 조회)
 * 
 * 3. curl -u user:user123 http://localhost:8080/v6/jdbc/secured
 *    → 200 OK
 * 
 * 학습 포인트:
 * - JdbcUserDetailsManager의 표준 스키마 동작 이해
 * - 고정 스키마의 한계 (비즈니스 요구사항에 맞춘 테이블 설계 어려움)
 */
@RestController
@RequestMapping("/v6/jdbc")
public class JdbcController {
    
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V6");
        response.put("message", "JDBC 스타일 공개 엔드포인트");
        response.put("description", "인증 없이 접근 가능합니다");
        response.put("schemaType", "JDBC 표준 스키마 (users + authorities)");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        response.put("principal", authentication != null ? authentication.getPrincipal() : "null");
        response.put("authenticated", authentication != null && authentication.isAuthenticated());
        
        return response;
    }
    
    @GetMapping("/secured")
    public Map<String, Object> securedEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V6");
        response.put("message", "JDBC 스타일 보호된 엔드포인트");
        response.put("description", "인증된 사용자만 접근 가능합니다");
        response.put("schemaType", "JDBC 표준 스키마 (users + authorities)");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("authenticationProvider", "DaoAuthenticationProvider (자동 생성)");
        
        response.put("learningNote", "JdbcUserDetailsManager는 고정 스키마를 요구하므로 유연성이 부족합니다.");
        
        return response;
    }
}

