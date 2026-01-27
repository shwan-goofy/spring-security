package org.example.hsspringsecurity.week1.v3httpbasic;

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
 * V3 - HTTP Basic 인증 컨트롤러
 * 
 * 테스트 시나리오:
 * 
 * 1. curl 테스트:
 *    curl http://localhost:8080/v3/httpbasic/public
 *    → 200 OK (인증 불필요)
 * 
 * 2. curl http://localhost:8080/v3/httpbasic/secured
 *    → 401 Unauthorized
 *    → WWW-Authenticate: Basic realm="Realm" 헤더 포함
 * 
 * 3. curl -u admin:12345 http://localhost:8080/v3/httpbasic/secured
 *    → 200 OK + 사용자 정보 반환
 *    → -u 옵션이 자동으로 Authorization: Basic base64(admin:12345) 헤더 생성
 * 
 * 4. curl -u user:12345 http://localhost:8080/v3/httpbasic/user
 *    → 200 OK
 * 
 * 5. Postman 테스트:
 *    - Authorization 탭에서 Type: Basic Auth 선택
 *    - Username: admin, Password: 12345 입력
 *    - Postman이 자동으로 Authorization 헤더 생성
 * 
 * 6. 브라우저 테스트:
 *    - http://localhost:8080/v3/httpbasic/secured 접속
 *    - 브라우저가 자동으로 로그인 팝업 표시
 *    - username, password 입력
 * 
 * 예상 결과:
 * - Authorization 헤더 없이 접근 시 401 Unauthorized
 * - 올바른 자격 증명으로 접근 시 200 OK
 * - 브라우저는 자격 증명을 캐시하여 재요청 시 자동으로 헤더 추가
 * 
 * Authorization 헤더 형식:
 * Authorization: Basic YWRtaW46MTIzNDU=
 * (YWRtaW46MTIzNDU= 는 base64("admin:12345"))
 */
@RestController
@RequestMapping("/v3/httpbasic")
public class HttpBasicController {

    /**
     * 공개 엔드포인트 - 인증 불필요
     */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V3 - 공개 엔드포인트 (HTTP Basic)");
        response.put("description", "인증 없이 접근 가능합니다");
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("principal", auth != null ? auth.getName() : "anonymous");
        response.put("testCommand", "curl http://localhost:8080/v3/httpbasic/public");
        
        return response;
    }

    /**
     * 보호된 엔드포인트 - 인증 필요
     * 
     * curl 테스트:
     * curl -u admin:12345 http://localhost:8080/v3/httpbasic/secured
     */
    @GetMapping("/secured")
    public Map<String, Object> securedEndpoint(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V3 - 보호된 엔드포인트 (HTTP Basic)");
        response.put("description", "HTTP Basic 인증 성공!");
        response.put("username", userDetails.getUsername());
        response.put("authorities", userDetails.getAuthorities());
        response.put("authMethod", "HTTP Basic (Authorization 헤더)");
        response.put("testCommand", "curl -u admin:12345 http://localhost:8080/v3/httpbasic/secured");
        
        return response;
    }

    /**
     * 사용자 엔드포인트 - 인증 필요
     */
    @GetMapping("/user")
    public Map<String, Object> userEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V3 - 사용자 엔드포인트 (HTTP Basic)");
        response.put("username", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("authenticationType", auth.getClass().getSimpleName());
        response.put("testCommand", "curl -u user:12345 http://localhost:8080/v3/httpbasic/user");
        
        return response;
    }

    /**
     * 관리자 엔드포인트 - 인증 필요
     */
    @GetMapping("/admin")
    public Map<String, Object> adminEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V3 - 관리자 엔드포인트 (HTTP Basic)");
        response.put("username", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("note", "WEEK 3에서 ROLE_ADMIN 권한 검사 추가 예정");
        response.put("testCommand", "curl -u admin:12345 http://localhost:8080/v3/httpbasic/admin");
        
        return response;
    }

    /**
     * POST 요청 테스트 - CSRF 토큰 검증
     * 
     * 중요: HTTP Basic을 사용하더라도 기본적으로 CSRF 보호가 활성화되어 있음
     * POST 요청 시 CSRF 토큰이 필요함 (V4에서 비활성화 예정)
     */
    @PostMapping("/create")
    public Map<String, Object> createEndpoint(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V3 - POST 요청 성공 (HTTP Basic)");
        response.put("username", userDetails.getUsername());
        response.put("note", "CSRF 필터가 활성화되어 있지만, Postman/curl에서는 CSRF 토큰 전송 방법 제한적");
        response.put("v4Preview", "V4에서 CSRF 비활성화하여 Stateless REST API 구현");
        
        return response;
    }

    /**
     * 인증 정보 확인 엔드포인트
     */
    @GetMapping("/auth-info")
    public Map<String, Object> authInfo(@AuthenticationPrincipal UserDetails userDetails) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V3 - 인증 정보");
        response.put("authenticated", auth.isAuthenticated());
        response.put("username", userDetails.getUsername());
        response.put("authType", auth.getClass().getSimpleName());
        response.put("filterUsed", "BasicAuthenticationFilter");
        response.put("headerFormat", "Authorization: Basic base64(username:password)");
        response.put("note", "매 요청마다 Authorization 헤더가 필요 (브라우저는 자동 캐시)");
        
        return response;
    }
}

