package org.example.hsspringsecurity.week1.v5multichain;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * V5 - Admin 컨트롤러 (Stateful 폼 로그인)
 * 
 * SecurityFilterChain: adminSecurityFilterChain (Order 6)
 * 인증 방식: 폼 로그인
 * 세션 정책: Stateful (기본)
 * CSRF: 활성화
 * 권한: ROLE_ADMIN 필요
 * 
 * 테스트 시나리오:
 * 
 * 1. 브라우저로 접근:
 *    http://localhost:8080/v5/admin/dashboard
 *    → 302 Redirect to /login (인증 필요)
 * 
 * 2. 로그인:
 *    - /login 페이지에서 username=admin, password=12345 입력
 *    → 로그인 성공 후 /v5/admin/dashboard로 리디렉션
 * 
 * 3. user 계정으로 로그인 시도:
 *    - username=user, password=12345
 *    → 로그인 성공하지만 /v5/admin/dashboard 접근 시 403 Forbidden
 *    → ROLE_ADMIN 권한이 없기 때문 (user는 ROLE_USER만 가짐)
 * 
 * 4. admin 계정으로 로그인 후:
 *    http://localhost:8080/v5/admin/dashboard
 *    → 200 OK (세션 유지)
 * 
 * 5. POST 요청 (브라우저/Postman):
 *    - CSRF 토큰 필요
 *    - 세션 쿠키 필요 (JSESSIONID)
 * 
 * 예상 결과:
 * - 폼 로그인 페이지로 리디렉션
 * - admin 계정만 접근 가능 (ROLE_ADMIN)
 * - user 계정은 403 Forbidden (권한 부족)
 * - 세션에 SecurityContext 저장
 * - CSRF 토큰 검증
 */
@RestController
@RequestMapping("/v5/admin")
public class AdminController {

    /**
     * 관리자 대시보드 - ADMIN 권한 필요
     * 
     * 접근 조건:
     * 1. 인증 필요 (authenticated)
     * 2. ROLE_ADMIN 권한 필요 (hasRole("ADMIN"))
     * 
     * 예상 결과:
     * - admin 계정: 200 OK
     * - user 계정: 403 Forbidden
     * - 미인증: 302 Redirect to /login
     */
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V5 - 관리자 대시보드");
        response.put("description", "ROLE_ADMIN 권한이 있는 사용자만 접근 가능");
        response.put("username", userDetails.getUsername());
        response.put("authorities", userDetails.getAuthorities());
        response.put("securityChain", "adminSecurityFilterChain (Order 6)");
        response.put("sessionPolicy", "Stateful - 세션에 SecurityContext 저장");
        response.put("csrfProtection", true);
        response.put("requiredRole", "ROLE_ADMIN");
        
        return response;
    }

    /**
     * 사용자 관리 - ADMIN 권한 필요
     */
    @GetMapping("/users")
    public Map<String, Object> manageUsers(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V5 - 사용자 관리");
        response.put("username", userDetails.getUsername());
        response.put("authorities", userDetails.getAuthorities());
        response.put("note", "관리자만 접근 가능한 사용자 관리 페이지");
        
        return response;
    }

    /**
     * 시스템 설정 - ADMIN 권한 필요
     */
    @GetMapping("/settings")
    public Map<String, Object> systemSettings(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V5 - 시스템 설정");
        response.put("username", userDetails.getUsername());
        response.put("authorities", userDetails.getAuthorities());
        
        return response;
    }

    /**
     * POST 요청 - CSRF 토큰 필요
     * 
     * 주의: 폼 로그인 기반이므로 CSRF 토큰 필수
     * REST 클라이언트(Postman, curl)에서 테스트하려면:
     * 1. GET /login으로 CSRF 토큰 획득
     * 2. POST 요청 시 _csrf 파라미터 또는 X-CSRF-TOKEN 헤더 포함
     */
    @PostMapping("/update-settings")
    public Map<String, Object> updateSettings(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V5 - 설정 업데이트 성공");
        response.put("username", userDetails.getUsername());
        response.put("csrfProtection", true);
        response.put("note", "CSRF 토큰이 검증되었습니다");
        
        return response;
    }

    /**
     * 세션 및 권한 정보 확인
     */
    @GetMapping("/auth-info")
    public Map<String, Object> authInfo(@AuthenticationPrincipal UserDetails userDetails) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V5 - Admin 인증 정보");
        response.put("securityChain", "adminSecurityFilterChain (Order 6)");
        response.put("urlPattern", "/v5/admin/**");
        
        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("username", userDetails.getUsername());
        authDetails.put("authorities", userDetails.getAuthorities());
        authDetails.put("authenticated", auth.isAuthenticated());
        authDetails.put("authType", auth.getClass().getSimpleName());
        
        response.put("인증정보", authDetails);
        
        Map<String, Object> settings = new HashMap<>();
        settings.put("CSRF", "활성화 - CsrfFilter 동작");
        settings.put("세션정책", "Stateful - HttpSession 사용");
        settings.put("인증방식", "폼 로그인 - UsernamePasswordAuthenticationFilter");
        settings.put("권한검사", "hasRole(\"ADMIN\") - ROLE_ADMIN 필요");
        
        response.put("설정", settings);
        
        Map<String, String> filters = new HashMap<>();
        filters.put("CsrfFilter", "활성화 - POST 요청 시 CSRF 토큰 검증");
        filters.put("UsernamePasswordAuthenticationFilter", "활성화 - POST /login 처리");
        filters.put("DefaultLoginPageGeneratingFilter", "활성화 - 기본 로그인 페이지");
        filters.put("SessionManagementFilter", "활성화 - 세션 관리");
        filters.put("AuthorizationFilter", "활성화 - hasRole(\"ADMIN\") 검사");
        
        response.put("주요필터", filters);
        
        return response;
    }

    /**
     * API와 Admin 비교 정보
     */
    @GetMapping("/comparison")
    public Map<String, Object> apiVsAdmin(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V5 - Multi-Chain 비교");
        response.put("username", userDetails.getUsername());
        
        Map<String, Object> apiChain = new HashMap<>();
        apiChain.put("URL패턴", "/v5/api/**");
        apiChain.put("Order", "5");
        apiChain.put("인증방식", "HTTP Basic");
        apiChain.put("세션정책", "STATELESS");
        apiChain.put("CSRF", "비활성화");
        apiChain.put("권한검사", "authenticated() - 인증만 확인");
        apiChain.put("사용처", "REST API, 모바일 앱");
        
        Map<String, Object> adminChain = new HashMap<>();
        adminChain.put("URL패턴", "/v5/admin/**");
        adminChain.put("Order", "6");
        adminChain.put("인증방식", "폼 로그인");
        adminChain.put("세션정책", "Stateful");
        adminChain.put("CSRF", "활성화");
        adminChain.put("권한검사", "hasRole(\"ADMIN\") - ADMIN 권한 필요");
        adminChain.put("사용처", "웹 관리자 페이지");
        
        response.put("API_체인", apiChain);
        response.put("Admin_체인", adminChain);
        response.put("핵심개념", "하나의 애플리케이션에서 여러 보안 전략 동시 운영 가능");
        
        return response;
    }
}

