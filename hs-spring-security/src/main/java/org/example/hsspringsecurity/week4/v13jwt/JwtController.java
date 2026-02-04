package org.example.hsspringsecurity.week4.v13jwt;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * V13: JWT 토큰 인증 컨트롤러
 * 
 * 테스트 시나리오:
 * 1. 로그인하여 JWT 토큰 받기:
 *    curl -u user@example.com:user123 http://localhost:8080/v13/jwt/login -v
 *    → Response Header에 Authorization: eyJhbGc... (JWT 토큰)
 *    → Body: {"message": "JWT 발급 완료", "username": "user@example.com"}
 * 
 * 2. JWT로 보호된 리소스 접근:
 *    curl http://localhost:8080/v13/jwt/secured \
 *      -H "Authorization: eyJhbGc..."
 *    → 200 OK
 * 
 * 3. JWT 없이 접근:
 *    curl http://localhost:8080/v13/jwt/secured
 *    → 403 Forbidden (JWT가 없어서 인증 실패)
 * 
 * 4. 만료된 JWT로 접근:
 *    (1시간 후)
 *    curl http://localhost:8080/v13/jwt/secured \
 *      -H "Authorization: (expired JWT)"
 *    → 403 Forbidden (BadCredentialsException)
 * 
 * 5. 공개 엔드포인트:
 *    curl http://localhost:8080/v13/jwt/public
 *    → 200 OK (JWT 불필요)
 * 
 * 학습 포인트:
 * - Stateless 인증: 서버는 세션을 사용하지 않음
 * - JWT에 모든 정보가 포함되어 있음 (self-contained)
 * - JSESSIONID 쿠키가 발급되지 않음
 * - 매 요청마다 JWT 검증 수행
 * - JWT 탈취 시 만료 시간까지 유효 (로그아웃 불가능)
 * - Refresh Token 패턴으로 보완 가능 (WEEK 5에서 다룸)
 */
@RestController
@RequestMapping("/v13/jwt")
public class JwtController {

    /**
     * 로그인 엔드포인트
     * 
     * HTTP Basic Auth로 인증하면 JWT 토큰 발급
     * BasicAuthenticationFilter가 인증 처리
     * JwtTokenGeneratorFilter가 JWT 생성 후 응답 헤더에 추가
     * 
     * 사용 방법:
     * curl -u user@example.com:user123 http://localhost:8080/v13/jwt/login -v
     * 
     * 응답 헤더 확인:
     * Authorization: eyJhbGciOiJIUzI1NiJ9...
     */
    @GetMapping("/login")
    public Map<String, Object> login() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V13");
        response.put("message", "JWT 발급 완료");
        response.put("username", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("instruction", Map.of(
            "step1", "Response Header에서 'Authorization' 헤더 값을 복사하세요",
            "step2", "이후 요청 시 'Authorization: (JWT)' 헤더에 담아 보내세요",
            "step3", "JWT 토큰은 1시간 동안 유효합니다"
        ));
        response.put("jwtHeader", SecurityConstants.JWT_HEADER);
        response.put("authType", "JWT (Stateless)");
        response.put("sessionId", "없음 (Stateless 모드)");
        
        return response;
    }

    /**
     * JWT로 보호된 엔드포인트
     * 
     * JWT가 유효해야 접근 가능
     * JwtTokenValidatorFilter가 JWT 검증 후 Authentication 객체 생성
     * 
     * 사용 방법:
     * curl http://localhost:8080/v13/jwt/secured \
     *   -H "Authorization: eyJhbGciOiJIUzI1NiJ9..."
     */
    @GetMapping("/secured")
    public Map<String, Object> secured() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V13");
        response.put("message", "JWT로 보호된 리소스");
        response.put("username", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("authenticated", auth.isAuthenticated());
        response.put("authType", "JWT");
        response.put("stateless", true);
        response.put("note", "JWT가 유효하므로 접근 허용됨");
        response.put("securityNote", "서버는 세션을 저장하지 않음 (Stateless)");
        
        return response;
    }

    /**
     * 공개 엔드포인트
     * 
     * JWT 없이도 접근 가능
     * SecurityConfig에서 permitAll()로 설정
     */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V13");
        response.put("message", "JWT 공개 엔드포인트");
        response.put("authRequired", false);
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("principal", auth != null ? auth.getName() : "anonymous");
        
        return response;
    }

    /**
     * JWT vs 세션 비교 정보
     */
    @GetMapping("/jwt-vs-session")
    public Map<String, Object> jwtVsSession() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V13");
        
        response.put("jwt", Map.of(
            "storage", "클라이언트 (로컬스토리지, 쿠키 등)",
            "stateless", true,
            "scalability", "수평 확장 용이",
            "revocation", "만료까지 유효 (블랙리스트 필요)",
            "csrfProtection", "불필요 (헤더 방식 시)"
        ));
        
        response.put("session", Map.of(
            "storage", "서버 (메모리, Redis 등)",
            "stateless", false,
            "scalability", "세션 공유 필요",
            "revocation", "즉시 가능 (세션 삭제)",
            "csrfProtection", "필수"
        ));
        
        response.put("recommendation", Map.of(
            "jwt", "REST API, 마이크로서비스, 모바일 앱",
            "session", "전통적인 웹 애플리케이션, 즉시 로그아웃 필요한 경우"
        ));
        
        return response;
    }
}

