package org.example.hsspringsecurity.week5.v16resourceserver;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * V16: OAuth2 Resource Server 컨트롤러
 * 
 * 테스트 시나리오:
 * 
 * 1. V13에서 JWT 발급 받기:
 *    curl -u user@example.com:user123 http://localhost:8080/v13/jwt/login -v
 *    → Response Header: Authorization: eyJhbGc...
 *    → JWT 토큰 복사
 * 
 * 2. V16에서 JWT로 API 호출:
 *    curl http://localhost:8080/v16/resource/api/account \
 *      -H "Authorization: eyJhbGc..."
 *    → 200 OK
 * 
 * 3. JWT 없이 접근:
 *    curl http://localhost:8080/v16/resource/api/account
 *    → 401 Unauthorized
 * 
 * 4. 만료된 JWT로 접근:
 *    (1시간 후)
 *    curl http://localhost:8080/v16/resource/api/account \
 *      -H "Authorization: (expired JWT)"
 *    → 401 Unauthorized
 * 
 * 5. USER JWT로 ADMIN API 접근:
 *    curl http://localhost:8080/v16/resource/api/admin \
 *      -H "Authorization: (USER JWT)"
 *    → 403 Forbidden (권한 부족)
 * 
 * 6. ADMIN JWT로 ADMIN API 접근:
 *    curl -u admin@example.com:admin123 http://localhost:8080/v13/jwt/login -v
 *    → ADMIN JWT 발급
 *    
 *    curl http://localhost:8080/v16/resource/api/admin \
 *      -H "Authorization: (ADMIN JWT)"
 *    → 200 OK
 * 
 * 7. 공개 엔드포인트:
 *    curl http://localhost:8080/v16/resource/public
 *    → 200 OK (JWT 불필요)
 * 
 * 학습 포인트:
 * - Resource Server는 JWT 검증만 수행 (발급 안 함)
 * - BearerTokenAuthenticationFilter가 자동으로 JWT 처리
 * - @AuthenticationPrincipal Jwt로 JWT 정보 접근
 * - V13(JWT 발급)과 V16(JWT 검증)의 분리
 * - MSA 환경에서 인증 서버와 리소스 서버 분리 아키텍처
 */
@RestController
@RequestMapping("/v16/resource")
public class ResourceServerController {

    /**
     * JWT로 보호된 계정 정보 API
     * 
     * @AuthenticationPrincipal Jwt
     * - BearerTokenAuthenticationFilter가 JWT 검증 후 생성한 Jwt 객체
     * - Claims: JWT Payload의 모든 정보
     * - Headers: JWT Header 정보
     * - Token Value: 원본 JWT 문자열
     */
    @GetMapping("/api/account")
    public Map<String, Object> getAccount(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V16");
        response.put("message", "Resource Server API - 계정 정보");
        
        // JWT Claims에서 정보 추출
        response.put("username", jwt.getClaim("username"));
        response.put("authorities", jwt.getClaim("authorities"));
        
        // JWT 표준 Claims
        response.put("issuer", jwt.getIssuer());
        response.put("subject", jwt.getSubject());
        response.put("issuedAt", jwt.getIssuedAt());
        response.put("expiresAt", jwt.getExpiresAt());
        
        // 토큰 정보
        response.put("tokenValue", jwt.getTokenValue().substring(0, 20) + "...");
        response.put("headers", jwt.getHeaders());
        
        response.put("note", "V13에서 발급받은 JWT로 인증됨");
        response.put("architecture", "V13(Authorization Server) ↔ V16(Resource Server)");
        
        return response;
    }

    /**
     * JWT + ROLE_USER 권한 필요
     */
    @GetMapping("/api/balance")
    public Map<String, Object> getBalance(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V16");
        response.put("message", "잔액 조회");
        response.put("username", jwt.getClaim("username"));
        response.put("balance", 1000000);
        response.put("currency", "KRW");
        response.put("requiredRole", "ROLE_USER");
        response.put("yourRoles", jwt.getClaim("authorities"));
        
        return response;
    }

    /**
     * JWT + ROLE_ADMIN 권한 필요
     * 
     * SecurityConfig에서 .hasRole("ADMIN") 설정
     * USER JWT로 접근 시 403 Forbidden
     */
    @GetMapping("/api/admin")
    public Map<String, Object> getAdminData(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V16");
        response.put("message", "관리자 전용 API");
        response.put("username", jwt.getClaim("username"));
        response.put("requiredRole", "ROLE_ADMIN");
        response.put("yourRoles", jwt.getClaim("authorities"));
        response.put("data", Map.of(
            "totalUsers", 100,
            "activeUsers", 85,
            "revenue", 10000000
        ));
        
        return response;
    }

    /**
     * 공개 엔드포인트
     * 
     * JWT 없이도 접근 가능
     * SecurityConfig에서 permitAll() 설정
     */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V16");
        response.put("message", "Resource Server 공개 API");
        response.put("authRequired", false);
        response.put("note", "JWT 없이 접근 가능");
        
        return response;
    }

    /**
     * Resource Server 개념 설명
     */
    @GetMapping("/concept")
    public Map<String, Object> concept() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V16");
        
        response.put("resourceServer", Map.of(
            "역할", "보호된 리소스(API)를 제공하는 서버",
            "인증방식", "JWT (Bearer Token)",
            "책임", "JWT 검증 + API 제공",
            "JWT발급", "하지 않음 (Authorization Server의 역할)"
        ));
        
        response.put("authorizationServer", Map.of(
            "역할", "JWT 발급 및 사용자 인증",
            "예시", "V13, Keycloak, Auth0, Okta",
            "책임", "사용자 인증 + JWT 발급",
            "API제공", "하지 않음 (Resource Server의 역할)"
        ));
        
        response.put("architecture", Map.of(
            "MSA", "인증 서버와 리소스 서버 분리",
            "장점", "관심사 분리, 수평 확장 용이",
            "통신", "JWT로 상태 전달 (Stateless)"
        ));
        
        response.put("flow", Map.of(
            "1", "클라이언트가 Authorization Server(V13)에서 JWT 발급",
            "2", "클라이언트가 Resource Server(V16)에 JWT와 함께 API 요청",
            "3", "Resource Server가 JWT 검증",
            "4", "검증 성공 시 리소스 제공"
        ));
        
        response.put("thisExample", Map.of(
            "V13", "Authorization Server 역할 (JWT 발급)",
            "V16", "Resource Server 역할 (JWT 검증 + API 제공)",
            "연동", "V13에서 발급한 JWT를 V16에서 검증"
        ));
        
        return response;
    }

    /**
     * JWT 검증 실패 시나리오 설명
     */
    @GetMapping("/jwt-errors")
    public Map<String, Object> jwtErrors() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V16");
        
        response.put("errors", Map.of(
            "JWT_MISSING", Map.of(
                "상황", "Authorization 헤더 없음",
                "응답", "401 Unauthorized",
                "해결", "V13에서 JWT 발급 후 헤더에 포함"
            ),
            "JWT_EXPIRED", Map.of(
                "상황", "JWT 만료 시간(exp) 초과",
                "응답", "401 Unauthorized",
                "해결", "V13에서 새로운 JWT 재발급"
            ),
            "JWT_INVALID_SIGNATURE", Map.of(
                "상황", "서명 불일치 (위변조 시도)",
                "응답", "401 Unauthorized",
                "해결", "올바른 JWT 사용"
            ),
            "INSUFFICIENT_AUTHORITY", Map.of(
                "상황", "JWT는 유효하지만 권한 부족",
                "응답", "403 Forbidden",
                "해결", "필요한 권한을 가진 사용자로 재로그인"
            )
        ));
        
        return response;
    }
}

