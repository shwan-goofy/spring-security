package org.example.hsspringsecurity.week3.v11corscrf;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * V11: CORS + CSRF 통합 컨트롤러
 * 
 * 테스트 시나리오:
 * 1. CORS Pre-flight 테스트:
 *    curl -X OPTIONS http://localhost:8080/v11/corscrf/transfer \
 *      -H "Origin: http://localhost:4200" \
 *      -H "Access-Control-Request-Method: POST"
 *    → 200 OK + Access-Control-Allow-Origin 헤더
 * 
 * 2. CSRF 토큰 조회:
 *    curl http://localhost:8080/v11/corscrf/csrf-token -u user@example.com:user123
 *    → {"token": "abc123...", "headerName": "X-XSRF-TOKEN"}
 * 
 * 3. CSRF 토큰 없이 POST:
 *    curl -X POST http://localhost:8080/v11/corscrf/transfer \
 *      -H "Content-Type: application/json" \
 *      -d '{"amount": 1000, "toAccount": "123456"}' \
 *      -u user@example.com:user123
 *    → 403 Invalid CSRF Token
 * 
 * 4. CSRF 토큰과 함께 POST (브라우저 시뮬레이션):
 *    - 쿠키: XSRF-TOKEN=abc123
 *    - 헤더: X-XSRF-TOKEN=abc123
 *    → 200 OK
 * 
 * 학습 포인트:
 * - CORS는 브라우저의 Same-Origin Policy를 우회하기 위한 메커니즘
 * - CSRF는 사용자의 의도하지 않은 요청을 방지하는 보안 기법
 * - CookieCsrfTokenRepository는 CSRF 토큰을 HttpOnly=false 쿠키에 저장
 * - 프론트엔드(React, Angular)는 쿠키에서 토큰을 읽어 헤더에 담아 전송
 */
@RestController
@RequestMapping("/v11/corscrf")
public class CorsCsrfController {

    /**
     * 공개 엔드포인트 - CORS 테스트용
     * 
     * CSRF 보호 제외: ignoringRequestMatchers()에 포함됨
     * 인증 불필요: permitAll()
     */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V11");
        response.put("message", "CORS 테스트용 공개 엔드포인트");
        response.put("cors", "모든 출처 허용 (학습용)");
        response.put("csrf", "CSRF 보호 제외");
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("principal", auth != null ? auth.getName() : "anonymous");
        
        return response;
    }

    /**
     * CSRF 토큰 조회 엔드포인트
     * 
     * 프론트엔드가 CSRF 토큰을 얻기 위해 호출
     * CsrfCookieFilter가 응답 헤더에도 토큰을 추가함
     */
    @GetMapping("/csrf-token")
    public Map<String, Object> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V11");
        response.put("token", csrfToken.getToken());
        response.put("headerName", csrfToken.getHeaderName());
        response.put("parameterName", csrfToken.getParameterName());
        response.put("cookieName", "XSRF-TOKEN");
        response.put("usage", Map.of(
            "step1", "쿠키에서 XSRF-TOKEN 값을 읽는다",
            "step2", "POST/PUT/DELETE 요청 시 X-XSRF-TOKEN 헤더에 토큰을 담아 보낸다",
            "step3", "서버는 쿠키의 토큰과 헤더의 토큰을 비교하여 검증한다"
        ));
        
        return response;
    }

    /**
     * CSRF 보호가 적용된 계좌이체 API
     * 
     * POST 요청이므로 CSRF 토큰 필수
     * 토큰이 없거나 일치하지 않으면 403 Forbidden
     */
    @PostMapping("/transfer")
    public Map<String, Object> transfer(@RequestBody Map<String, Object> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V11");
        response.put("message", "계좌이체 성공");
        response.put("from", auth.getName());
        response.put("toAccount", request.get("toAccount"));
        response.put("amount", request.get("amount"));
        response.put("csrfProtection", "활성화됨");
        response.put("note", "X-XSRF-TOKEN 헤더가 쿠키의 CSRF 토큰과 일치해야 합니다");
        response.put("securityNote", "CSRF 공격으로부터 보호됨");
        
        return response;
    }

    /**
     * CSRF 보호 테스트용 PUT 엔드포인트
     */
    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V11");
        response.put("message", "업데이트 성공");
        response.put("data", request);
        response.put("csrfProtection", "활성화됨");
        
        return response;
    }

    /**
     * CSRF 보호 테스트용 DELETE 엔드포인트
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V11");
        response.put("message", "삭제 성공");
        response.put("deletedId", id);
        response.put("csrfProtection", "활성화됨");
        response.put("note", "DELETE 요청도 CSRF 토큰 필수");
        
        return response;
    }
}

