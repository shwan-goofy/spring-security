package org.example.hsspringsecurity.week3.v12authorization;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V12: 역할 기반 권한 부여(Authorization) 컨트롤러
 * 
 * 테스트 시나리오:
 * 1. USER 계정으로 로그인 (user@example.com / user123):
 *    curl -u user@example.com:user123 http://localhost:8080/v12/authorization/user
 *    → 200 OK (ROLE_USER 보유)
 *    
 *    curl -u user@example.com:user123 http://localhost:8080/v12/authorization/admin
 *    → 403 Forbidden (ROLE_ADMIN 미보유)
 *    
 *    curl -u user@example.com:user123 http://localhost:8080/v12/authorization/any
 *    → 200 OK (ROLE_USER 보유)
 * 
 * 2. ADMIN 계정으로 로그인 (admin@example.com / admin123):
 *    curl -u admin@example.com:admin123 http://localhost:8080/v12/authorization/admin
 *    → 200 OK (ROLE_ADMIN 보유)
 *    
 *    curl -u admin@example.com:admin123 http://localhost:8080/v12/authorization/user
 *    → 403 Forbidden (ROLE_USER 미보유 - ADMIN과 USER는 별개 역할)
 *    
 *    curl -u admin@example.com:admin123 http://localhost:8080/v12/authorization/any
 *    → 200 OK (ROLE_ADMIN 보유)
 * 
 * 3. 인증 없이 접근:
 *    curl http://localhost:8080/v12/authorization/user
 *    → 401 Unauthorized
 * 
 * 4. 공개 엔드포인트:
 *    curl http://localhost:8080/v12/authorization/public
 *    → 200 OK (인증 불필요)
 * 
 * 학습 포인트:
 * - hasRole("USER"): ROLE_USER 권한 필요 (접두사 자동 추가)
 * - hasRole("ADMIN"): ROLE_ADMIN 권한 필요
 * - hasAnyRole("USER", "ADMIN"): 둘 중 하나만 있으면 허용
 * - 역할(Role)은 권한(Authority)의 특수한 형태로, ROLE_ 접두사를 가짐
 * - Spring Security는 DB의 "ROLE_USER"를 hasRole("USER")로 매칭
 * - 403 Forbidden: 인증은 됐지만 권한 부족
 * - 401 Unauthorized: 인증 자체가 안 됨
 */
@RestController
@RequestMapping("/v12/authorization")
public class AuthorizationController {

    /**
     * 공개 엔드포인트 - 인증 불필요
     * 
     * 누구나 접근 가능: permitAll()
     */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V12");
        response.put("access", "public");
        response.put("authRequired", false);
        response.put("message", "역할 기반 권한 부여 - 공개 엔드포인트");
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("principal", auth != null ? auth.getName() : "anonymous");
        response.put("authorities", auth != null ? auth.getAuthorities() : null);
        
        return response;
    }

    /**
     * USER 역할 전용 엔드포인트
     * 
     * 필요 권한: ROLE_USER
     * SecurityConfig에서 .hasRole("USER")로 제한
     * 
     * DB에는 "ROLE_USER"로 저장되어 있음
     */
    @GetMapping("/user")
    public Map<String, Object> userEndpoint(@AuthenticationPrincipal UserDetails user) {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V12");
        response.put("access", "user");
        response.put("username", user.getUsername());
        response.put("authorities", user.getAuthorities());
        response.put("requiredRole", "ROLE_USER");
        response.put("message", "USER 역할을 가진 사용자만 접근 가능");
        response.put("testCredentials", "user@example.com / user123");
        response.put("securityConfigRule", "hasRole(\"USER\")");
        
        return response;
    }

    /**
     * ADMIN 역할 전용 엔드포인트
     * 
     * 필요 권한: ROLE_ADMIN
     * SecurityConfig에서 .hasRole("ADMIN")로 제한
     * 
     * DB에는 "ROLE_ADMIN"으로 저장되어 있음
     */
    @GetMapping("/admin")
    public Map<String, Object> adminEndpoint(@AuthenticationPrincipal UserDetails user) {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V12");
        response.put("access", "admin");
        response.put("username", user.getUsername());
        response.put("authorities", user.getAuthorities());
        response.put("requiredRole", "ROLE_ADMIN");
        response.put("message", "ADMIN 역할을 가진 사용자만 접근 가능");
        response.put("testCredentials", "admin@example.com / admin123");
        response.put("securityConfigRule", "hasRole(\"ADMIN\")");
        response.put("note", "일반 USER는 접근 불가 (역할이 다름)");
        
        return response;
    }

    /**
     * USER 또는 ADMIN 역할 중 하나만 있으면 접근 가능
     * 
     * 필요 권한: ROLE_USER 또는 ROLE_ADMIN
     * SecurityConfig에서 .hasAnyRole("USER", "ADMIN")로 설정
     */
    @GetMapping("/any")
    public Map<String, Object> anyEndpoint(@AuthenticationPrincipal UserDetails user) {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V12");
        response.put("access", "any");
        response.put("username", user.getUsername());
        response.put("authorities", user.getAuthorities());
        response.put("allowedRoles", List.of("ROLE_USER", "ROLE_ADMIN"));
        response.put("message", "USER 또는 ADMIN 역할 중 하나만 있으면 접근 가능");
        response.put("securityConfigRule", "hasAnyRole(\"USER\", \"ADMIN\")");
        response.put("testNote", "user@example.com과 admin@example.com 모두 접근 가능");
        
        return response;
    }

    /**
     * 인증만 되면 역할 상관없이 접근 가능
     * 
     * 필요 권한: authenticated()
     * 특정 역할 체크 없음
     */
    @GetMapping("/authenticated")
    public Map<String, Object> authenticatedEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V12");
        response.put("access", "authenticated");
        response.put("username", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("message", "인증만 되면 역할 상관없이 접근 가능");
        response.put("securityConfigRule", "authenticated()");
        response.put("note", "USER든 ADMIN이든 로그인만 하면 OK");
        
        return response;
    }

    /**
     * hasRole vs hasAuthority 비교 설명
     */
    @GetMapping("/role-vs-authority")
    public Map<String, Object> roleVsAuthority() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V12");
        response.put("hasRole", Map.of(
            "usage", "hasRole(\"USER\")",
            "matches", "ROLE_USER",
            "note", "Spring Security가 자동으로 ROLE_ 접두사 추가"
        ));
        response.put("hasAuthority", Map.of(
            "usage", "hasAuthority(\"ROLE_USER\")",
            "matches", "ROLE_USER",
            "note", "접두사 없이 그대로 매칭"
        ));
        response.put("recommendation", "역할(Role)은 hasRole() 사용, 세밀한 권한은 hasAuthority() 사용");
        response.put("example", Map.of(
            "hasRole(\"ADMIN\")", "관리자 역할 체크",
            "hasAuthority(\"DELETE_USER\")", "사용자 삭제 권한 체크"
        ));
        
        return response;
    }
}

