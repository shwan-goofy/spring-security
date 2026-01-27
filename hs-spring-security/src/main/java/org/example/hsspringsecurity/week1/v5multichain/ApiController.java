package org.example.hsspringsecurity.week1.v5multichain;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * V5 - API 컨트롤러 (Stateless REST API)
 * 
 * SecurityFilterChain: apiSecurityFilterChain (Order 5)
 * 인증 방식: HTTP Basic
 * 세션 정책: STATELESS
 * CSRF: 비활성화
 * 
 * 테스트 시나리오:
 * 
 * 1. 공개 API:
 *    curl http://localhost:8080/v5/api/public
 *    → 200 OK (인증 불필요)
 * 
 * 2. 보호된 API (GET):
 *    curl -u admin:12345 http://localhost:8080/v5/api/data
 *    → 200 OK
 * 
 * 3. POST 요청 (CSRF 토큰 없이):
 *    curl -X POST -u admin:12345 -H "Content-Type: application/json" \
 *         -d '{"data":"test"}' http://localhost:8080/v5/api/data
 *    → 200 OK
 * 
 * 4. 세션 확인:
 *    curl -i -u admin:12345 http://localhost:8080/v5/api/data
 *    → Set-Cookie 헤더 없음 (STATELESS)
 * 
 * 5. user 계정으로 접근:
 *    curl -u user:12345 http://localhost:8080/v5/api/data
 *    → 200 OK (권한 검사 없음)
 * 
 * 예상 결과:
 * - 매 요청마다 Authorization 헤더 필요
 * - POST 요청 시 CSRF 토큰 불필요
 * - 세션 생성 안 함
 * - REST API 클라이언트 (Postman, curl, 모바일 앱)에 적합
 */
@RestController
@RequestMapping("/v5/api")
public class ApiController {

    /**
     * 공개 API - 인증 불필요
     */
    @GetMapping("/public")
    public Map<String, Object> publicApi() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V5 - 공개 API (Multi-Chain)");
        response.put("description", "인증 없이 접근 가능한 REST API");
        response.put("securityChain", "apiSecurityFilterChain (Order 5)");
        response.put("sessionPolicy", "STATELESS");
        response.put("csrfProtection", false);
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("testCommand", "curl http://localhost:8080/v5/api/public");
        
        return response;
    }

    /**
     * 보호된 API - 인증 필요 (GET)
     */
    @GetMapping("/data")
    public Map<String, Object> getData(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V5 - API 데이터 조회");
        response.put("username", userDetails.getUsername());
        response.put("authorities", userDetails.getAuthorities());
        response.put("authMethod", "HTTP Basic");
        response.put("securityChain", "apiSecurityFilterChain");
        response.put("sessionPolicy", "STATELESS - 매 요청마다 인증");
        response.put("testCommand", "curl -u admin:12345 http://localhost:8080/v5/api/data");
        
        return response;
    }

    /**
     * POST 요청 - CSRF 토큰 없이 성공
     */
    @PostMapping("/data")
    public Map<String, Object> createData(@AuthenticationPrincipal UserDetails userDetails,
                                           @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V5 - API 데이터 생성 성공");
        response.put("username", userDetails.getUsername());
        response.put("requestBody", body != null ? body : "No body");
        response.put("csrfProtection", false);
        response.put("note", "Stateless API이므로 CSRF 토큰 불필요");
        response.put("testCommand", "curl -X POST -u admin:12345 -H \"Content-Type: application/json\" -d '{\"data\":\"test\"}' http://localhost:8080/v5/api/data");
        
        return response;
    }

    /**
     * PUT 요청
     */
    @PutMapping("/data/{id}")
    public Map<String, Object> updateData(@PathVariable String id,
                                           @AuthenticationPrincipal UserDetails userDetails,
                                           @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V5 - API 데이터 수정 성공");
        response.put("id", id);
        response.put("username", userDetails.getUsername());
        response.put("requestBody", body != null ? body : "No body");
        
        return response;
    }

    /**
     * DELETE 요청
     */
    @DeleteMapping("/data/{id}")
    public Map<String, Object> deleteData(@PathVariable String id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V5 - API 데이터 삭제 성공");
        response.put("id", id);
        response.put("username", userDetails.getUsername());
        
        return response;
    }

    /**
     * API 설정 정보 확인
     */
    @GetMapping("/config")
    public Map<String, Object> apiConfig(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V5 - API SecurityFilterChain 설정");
        response.put("username", userDetails.getUsername());
        response.put("securityChain", "apiSecurityFilterChain (Order 5)");
        response.put("urlPattern", "/v5/api/**");
        
        Map<String, Object> settings = new HashMap<>();
        settings.put("CSRF", "비활성화");
        settings.put("세션정책", "STATELESS");
        settings.put("인증방식", "HTTP Basic");
        settings.put("권한검사", "authenticated() - 인증만 확인");
        
        response.put("설정", settings);
        
        Map<String, String> filters = new HashMap<>();
        filters.put("CsrfFilter", "제거됨");
        filters.put("BasicAuthenticationFilter", "활성화 - 매 요청마다 실행");
        filters.put("SessionManagementFilter", "STATELESS 모드");
        filters.put("AuthorizationFilter", "활성화 - authenticated() 검사");
        
        response.put("주요필터", filters);
        
        return response;
    }
}

