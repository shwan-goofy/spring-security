package org.example.hsspringsecurity.week1.v4nocsrf;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * V4 - CSRF 비활성화 컨트롤러
 * 
 * 테스트 시나리오:
 * 
 * 1. GET 요청 (인증 불필요):
 *    curl http://localhost:8080/v4/nocsrf/public
 *    → 200 OK
 * 
 * 2. GET 요청 (인증 필요):
 *    curl -u admin:12345 http://localhost:8080/v4/nocsrf/secured
 *    → 200 OK
 * 
 * 3. POST 요청 (CSRF 토큰 없이):
 *    curl -X POST -u admin:12345 http://localhost:8080/v4/nocsrf/create
 *    → 200 OK (V2, V3와 달리 CSRF 토큰 불필요!)
 * 
 * 4. PUT 요청:
 *    curl -X PUT -u admin:12345 -H "Content-Type: application/json" \
 *         -d '{"data":"test"}' http://localhost:8080/v4/nocsrf/update
 *    → 200 OK
 * 
 * 5. DELETE 요청:
 *    curl -X DELETE -u admin:12345 http://localhost:8080/v4/nocsrf/delete
 *    → 200 OK
 * 
 * 6. 세션 확인:
 *    curl -i -u admin:12345 http://localhost:8080/v4/nocsrf/secured
 *    → Set-Cookie 헤더 없음 (STATELESS 모드)
 * 
 * V2, V3와의 비교:
 * - V2, V3: POST 요청 시 CSRF 토큰 필요 (CsrfFilter 활성화)
 * - V4: POST 요청 시 CSRF 토큰 불필요 (CsrfFilter 제거)
 * - V2, V3: 세션에 SecurityContext 저장 (JSESSIONID 쿠키)
 * - V4: 세션 사용 안 함 (STATELESS)
 * 
 * 예상 결과:
 * - POST, PUT, DELETE 요청이 CSRF 토큰 없이 성공
 * - 매 요청마다 Authorization 헤더 필요 (세션 없음)
 * - 응답에 Set-Cookie 헤더 없음
 * - Postman, curl에서 테스트 용이
 */
@RestController
@RequestMapping("/v4/nocsrf")
public class NoCsrfController {

    /**
     * 공개 엔드포인트 - 인증 불필요
     */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V4 - 공개 엔드포인트 (CSRF 비활성화)");
        response.put("description", "인증 없이 접근 가능합니다");
        response.put("csrfProtection", false);
        response.put("sessionPolicy", "STATELESS");
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("testCommand", "curl http://localhost:8080/v4/nocsrf/public");
        
        return response;
    }

    /**
     * 보호된 엔드포인트 - 인증 필요 (GET)
     */
    @GetMapping("/secured")
    public Map<String, Object> securedEndpoint(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V4 - 보호된 엔드포인트 (CSRF 비활성화)");
        response.put("username", userDetails.getUsername());
        response.put("authorities", userDetails.getAuthorities());
        response.put("csrfProtection", false);
        response.put("sessionPolicy", "STATELESS - 매 요청마다 인증 필요");
        response.put("testCommand", "curl -u admin:12345 http://localhost:8080/v4/nocsrf/secured");
        
        return response;
    }

    /**
     * POST 요청 - CSRF 토큰 없이 성공
     * 
     * 중요: V2, V3에서는 CSRF 토큰이 필요했지만, V4에서는 불필요!
     */
    @PostMapping("/create")
    public Map<String, Object> createEndpoint(@AuthenticationPrincipal UserDetails userDetails,
                                                @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V4 - POST 요청 성공! (CSRF 토큰 불필요)");
        response.put("username", userDetails.getUsername());
        response.put("requestBody", body != null ? body : "No body");
        response.put("csrfProtection", false);
        response.put("note", "CsrfFilter가 제거되어 CSRF 토큰 검증 안 함");
        response.put("testCommand", "curl -X POST -u admin:12345 -H \"Content-Type: application/json\" -d '{\"data\":\"test\"}' http://localhost:8080/v4/nocsrf/create");
        
        return response;
    }

    /**
     * PUT 요청 - CSRF 토큰 없이 성공
     */
    @PutMapping("/update")
    public Map<String, Object> updateEndpoint(@AuthenticationPrincipal UserDetails userDetails,
                                                @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V4 - PUT 요청 성공!");
        response.put("username", userDetails.getUsername());
        response.put("requestBody", body != null ? body : "No body");
        response.put("csrfProtection", false);
        response.put("testCommand", "curl -X PUT -u admin:12345 -H \"Content-Type: application/json\" -d '{\"id\":1,\"data\":\"updated\"}' http://localhost:8080/v4/nocsrf/update");
        
        return response;
    }

    /**
     * DELETE 요청 - CSRF 토큰 없이 성공
     */
    @DeleteMapping("/delete")
    public Map<String, Object> deleteEndpoint(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V4 - DELETE 요청 성공!");
        response.put("username", userDetails.getUsername());
        response.put("csrfProtection", false);
        response.put("testCommand", "curl -X DELETE -u admin:12345 http://localhost:8080/v4/nocsrf/delete");
        
        return response;
    }

    /**
     * 필터 체인 비교 엔드포인트
     */
    @GetMapping("/filter-comparison")
    public Map<String, Object> filterComparison(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V4 - 필터 체인 비교");
        response.put("username", userDetails.getUsername());
        
        Map<String, Object> v2v3 = new HashMap<>();
        v2v3.put("CsrfFilter", "활성화 - POST 요청 시 CSRF 토큰 필요");
        v2v3.put("SessionManagementFilter", "활성화 - 세션에 SecurityContext 저장");
        v2v3.put("SecurityContextHolderFilter", "세션에서 SecurityContext 로드");
        
        Map<String, Object> v4 = new HashMap<>();
        v4.put("CsrfFilter", "제거됨 - CSRF 토큰 불필요");
        v4.put("SessionManagementFilter", "STATELESS 모드 - 세션 생성 안 함");
        v4.put("SecurityContextHolderFilter", "빈 SecurityContext 생성 (세션 사용 안 함)");
        v4.put("BasicAuthenticationFilter", "매 요청마다 실행 - Authorization 헤더 확인");
        
        response.put("V2_V3_필터", v2v3);
        response.put("V4_필터", v4);
        response.put("주요차이", "V4는 Stateless REST API에 최적화됨");
        
        return response;
    }

    /**
     * Stateless 확인 엔드포인트
     */
    @GetMapping("/stateless-check")
    public Map<String, Object> statelessCheck(@AuthenticationPrincipal UserDetails userDetails) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V4 - Stateless 확인");
        response.put("authenticated", auth.isAuthenticated());
        response.put("username", userDetails.getUsername());
        response.put("sessionPolicy", "STATELESS");
        response.put("note1", "서버가 세션을 생성하지 않음");
        response.put("note2", "매 요청마다 Authorization 헤더 필요");
        response.put("note3", "응답에 Set-Cookie 헤더 없음");
        response.put("usageScenario", "REST API, 모바일 앱 백엔드, 마이크로서비스");
        
        return response;
    }
}

