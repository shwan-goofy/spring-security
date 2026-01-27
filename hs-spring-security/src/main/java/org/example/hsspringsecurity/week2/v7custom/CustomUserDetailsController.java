package org.example.hsspringsecurity.week2.v7custom;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * V7: Custom UserDetailsService 컨트롤러
 * 
 * 테스트 시나리오:
 * 1. curl http://localhost:8080/v7/custom/public
 *    → 200 OK (인증 불필요)
 * 
 * 2. curl -u admin@example.com:admin123 http://localhost:8080/v7/custom/secured
 *    → 200 OK (Customer Repository에서 조회)
 * 
 * 3. curl -u admin@example.com:admin123 http://localhost:8080/v7/custom/admin
 *    → 200 OK (ADMIN role 보유)
 * 
 * 4. curl -u user@example.com:user123 http://localhost:8080/v7/custom/admin
 *    → 403 Forbidden (USER role만 보유, ADMIN 권한 없음)
 * 
 * 5. curl -u user@example.com:user123 http://localhost:8080/v7/custom/user
 *    → 200 OK (USER role 보유)
 * 
 * 학습 포인트:
 * - 커스텀 도메인 모델로 유연한 인증 구현
 * - Role-based 권한 부여 (hasRole, hasAnyRole)
 * - V6(고정 스키마)보다 실무에 적합
 */
@RestController
@RequestMapping("/v7/custom")
public class CustomUserDetailsController {
    
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V7");
        response.put("message", "Custom UserDetailsService 공개 엔드포인트");
        response.put("description", "인증 없이 접근 가능합니다");
        response.put("approachType", "커스텀 도메인 모델 (Customer)");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        response.put("principal", authentication != null ? authentication.getPrincipal() : "null");
        response.put("authenticated", authentication != null && authentication.isAuthenticated());
        
        return response;
    }
    
    @GetMapping("/secured")
    public Map<String, Object> securedEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V7");
        response.put("message", "Custom UserDetailsService 보호된 엔드포인트");
        response.put("description", "인증된 사용자만 접근 가능합니다");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("authenticationProvider", "DaoAuthenticationProvider (자동 생성)");
        response.put("userDetailsService", "CustomUserDetailsService");
        
        return response;
    }
    
    @GetMapping("/user")
    public Map<String, Object> userEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V7");
        response.put("message", "USER 권한 엔드포인트");
        response.put("description", "USER 또는 ADMIN 권한이 필요합니다");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("accessRule", "hasAnyRole('USER', 'ADMIN')");
        
        return response;
    }
    
    @GetMapping("/admin")
    public Map<String, Object> adminEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V7");
        response.put("message", "ADMIN 권한 엔드포인트");
        response.put("description", "ADMIN 권한만 접근 가능합니다");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("accessRule", "hasRole('ADMIN')");
        
        response.put("learningNote", "CustomUserDetailsService는 비즈니스 요구사항에 맞는 유연한 인증을 제공합니다.");
        
        return response;
    }
}

