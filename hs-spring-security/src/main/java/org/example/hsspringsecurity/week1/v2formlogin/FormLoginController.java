package org.example.hsspringsecurity.week1.v2formlogin;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * V2 - 폼 로그인 컨트롤러
 * 
 * 테스트 시나리오:
 * 1. GET /v2/formlogin/public → 200 OK (인증 불필요)
 * 2. GET /v2/formlogin/secured → 302 Redirect to /login
 * 3. 브라우저에서 /login 접속 → 기본 로그인 페이지 표시
 * 4. username=admin, password=12345 입력 후 로그인
 * 5. GET /v2/formlogin/secured → 200 OK (세션 유지)
 * 6. GET /v2/formlogin/user → 200 OK
 * 7. GET /v2/formlogin/admin → 200 OK
 * 
 * 예상 결과:
 * - 인증되지 않은 상태에서 보호된 엔드포인트 접근 시 로그인 페이지로 리디렉션
 * - 로그인 성공 후 세션이 유지되어 재인증 불필요
 * - 로그인 성공 후 원래 요청한 페이지로 자동 리디렉션 (RequestCache)
 * 
 * 주요 학습 포인트:
 * - UsernamePasswordAuthenticationFilter가 POST /login 처리
 * - SecurityContextHolderFilter가 세션에서 SecurityContext 로드
 * - ExceptionTranslationFilter가 AuthenticationException 감지 시 로그인 페이지로 리디렉션
 * - RequestCache가 인증 전 요청 URL을 저장했다가 로그인 후 복원
 */
@RestController
@RequestMapping("/v2/formlogin")
public class FormLoginController {

    /**
     * 공개 엔드포인트 - 인증 불필요
     */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V2 - 공개 엔드포인트 (폼 로그인)");
        response.put("description", "인증 없이 접근 가능합니다");
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("principal", auth != null ? auth.getName() : "anonymous");
        
        return response;
    }

    /**
     * 보호된 엔드포인트 - 인증 필요
     * 
     * 예상 결과: 
     * - 미인증 시: 302 Redirect to /login
     * - 인증 후: 200 OK + 사용자 정보 반환
     */
    @GetMapping("/secured")
    public Map<String, Object> securedEndpoint(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V2 - 보호된 엔드포인트 (폼 로그인)");
        response.put("description", "로그인 성공! 인증된 사용자만 접근 가능");
        response.put("username", userDetails.getUsername());
        response.put("authorities", userDetails.getAuthorities());
        response.put("sessionBased", true);
        response.put("note", "세션에 SecurityContext가 저장되어 재인증 불필요");
        
        return response;
    }

    /**
     * 사용자 엔드포인트 - 인증 필요
     */
    @GetMapping("/user")
    public Map<String, Object> userEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V2 - 사용자 엔드포인트 (폼 로그인)");
        response.put("username", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("authenticationType", auth.getClass().getSimpleName());
        
        return response;
    }

    /**
     * 관리자 엔드포인트 - 인증 필요
     * 
     * 참고: 현재는 인증만 확인하고 권한(ROLE_ADMIN)은 검사하지 않음
     * WEEK 3에서 .hasRole("ADMIN") 추가 예정
     */
    @GetMapping("/admin")
    public Map<String, Object> adminEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V2 - 관리자 엔드포인트 (폼 로그인)");
        response.put("username", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("note", "WEEK 3에서 ROLE_ADMIN 권한 검사 추가 예정");
        
        return response;
    }

    /**
     * 세션 정보 확인 엔드포인트
     */
    @GetMapping("/session-info")
    public Map<String, Object> sessionInfo(@AuthenticationPrincipal UserDetails userDetails) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V2 - 세션 정보");
        response.put("authenticated", auth.isAuthenticated());
        response.put("username", userDetails.getUsername());
        response.put("authType", auth.getClass().getSimpleName());
        response.put("description", "SecurityContext가 HttpSession에 저장되어 있음");
        response.put("filterChain", "SecurityContextHolderFilter가 세션에서 SecurityContext를 로드");
        
        return response;
    }
}

