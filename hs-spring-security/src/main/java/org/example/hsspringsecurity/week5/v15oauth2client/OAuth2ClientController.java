package org.example.hsspringsecurity.week5.v15oauth2client;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * V15: OAuth2 Client 컨트롤러 (Mock)
 * 
 * 테스트 시나리오:
 * 
 * Mock 환경이므로 실제 OAuth2 로그인 과정을 시뮬레이션합니다.
 * 실제 환경에서는 다음과 같이 동작:
 * 
 * 1. /oauth2/authorization/github 접근 → GitHub 로그인 페이지로 리디렉션
 * 2. GitHub에서 로그인 및 동의
 * 3. /login/oauth2/code/github?code=abc123 으로 콜백
 * 4. Spring Security가 code → Access Token 교환
 * 5. Access Token으로 /user 정보 조회
 * 6. OAuth2User 객체 생성 후 SecurityContext에 저장
 * 7. 애플리케이션으로 리디렉션 (보통 /)
 * 
 * Mock 테스트:
 * - 브라우저로 http://localhost:8080/v15/oauth2/user 접근
 * - MockOAuth2UserService가 가짜 사용자 정보 반환
 * - 로그인 페이지 없이 바로 인증됨 (학습용 간소화)
 * 
 * curl 테스트 (실제 OAuth2는 브라우저 필요):
 * curl -u mockuser@example.com:any http://localhost:8080/v15/oauth2/user
 * (실제로는 Basic Auth 대신 OAuth2 Flow 필요)
 * 
 * 학습 포인트:
 * - OAuth2User vs UserDetails 차이
 * - OAuth2 Client 모드의 필터 체인
 * - Authorization Code Grant Flow 이해
 * - 소셜 로그인과 자체 로그인 통합 방법
 */
@RestController
@RequestMapping("/v15/oauth2")
public class OAuth2ClientController {

    /**
     * OAuth2 인증된 사용자 정보 조회
     * 
     * @AuthenticationPrincipal OAuth2User
     * - Spring Security가 OAuth2 로그인 완료 후 생성한 사용자 객체
     * - attributes: Provider에서 받은 사용자 정보 (Map)
     * - authorities: Spring Security가 부여한 권한
     * - name: Principal의 이름 (attributes의 특정 키 값)
     */
    @GetMapping("/user")
    public Map<String, Object> getUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V15");
        response.put("message", "OAuth2 인증된 사용자 정보");
        
        // OAuth2User의 주요 정보
        response.put("provider", oauth2User.getAttribute("provider"));
        response.put("email", oauth2User.getAttribute("email"));
        response.put("name", oauth2User.getAttribute("name"));
        response.put("picture", oauth2User.getAttribute("picture"));
        response.put("sub", oauth2User.getAttribute("sub"));
        
        // 전체 attributes
        response.put("attributes", oauth2User.getAttributes());
        
        // Spring Security가 부여한 권한
        response.put("authorities", oauth2User.getAuthorities());
        
        // Principal 이름 (nameAttributeKey로 지정한 값)
        response.put("principal", oauth2User.getName());
        
        response.put("note", Map.of(
            "실제환경", "GitHub/Google 등 외부 Provider에서 정보 조회",
            "Mock환경", "MockOAuth2UserService가 하드코딩된 데이터 반환",
            "자동회원가입", "실무에서는 이 시점에 DB 확인 및 회원 저장"
        ));
        
        return response;
    }

    /**
     * OAuth2 로그인 과정 시뮬레이션 설명
     */
    @GetMapping("/login-simulation")
    public Map<String, Object> loginSimulation() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V15");
        response.put("title", "OAuth2 Authorization Code Flow 시뮬레이션");
        
        response.put("step1", Map.of(
            "action", "사용자가 /oauth2/authorization/{provider} 접근",
            "example", "/oauth2/authorization/github",
            "result", "OAuth2AuthorizationRequestRedirectFilter가 GitHub 로그인 페이지로 리디렉션"
        ));
        
        response.put("step2", Map.of(
            "action", "Authorization Server(GitHub)로 리디렉션",
            "url", "https://github.com/login/oauth/authorize?client_id=...&redirect_uri=...&scope=...",
            "user", "GitHub에서 로그인 및 권한 동의"
        ));
        
        response.put("step3", Map.of(
            "action", "사용자 로그인 및 동의",
            "result", "GitHub이 Authorization Code 발급"
        ));
        
        response.put("step4", Map.of(
            "action", "Authorization Code 발급",
            "callback", "애플리케이션의 redirect_uri로 콜백",
            "example", "/login/oauth2/code/github?code=abc123"
        ));
        
        response.put("step5", Map.of(
            "action", "Code → Access Token 교환",
            "filter", "OAuth2LoginAuthenticationFilter",
            "request", "POST https://github.com/login/oauth/access_token",
            "body", "code=abc123&client_id=...&client_secret=...",
            "response", "{\"access_token\": \"gho_...\", \"token_type\": \"bearer\"}"
        ));
        
        response.put("step6", Map.of(
            "action", "Access Token으로 사용자 정보 조회",
            "service", "OAuth2UserService.loadUser()",
            "request", "GET https://api.github.com/user",
            "header", "Authorization: Bearer gho_...",
            "response", "{\"login\": \"john\", \"email\": \"john@example.com\", ...}"
        ));
        
        response.put("step7", Map.of(
            "action", "Spring Security에 OAuth2User 저장",
            "object", "OAuth2AuthenticationToken",
            "context", "SecurityContextHolder에 저장",
            "session", "JSESSIONID 쿠키 발급 (세션 기반)"
        ));
        
        response.put("step8", Map.of(
            "action", "애플리케이션으로 리디렉션",
            "default", "/",
            "custom", "defaultSuccessUrl() 설정 가능"
        ));
        
        response.put("currentMode", "Mock Mode");
        response.put("mockNote", "실제 외부 호출 없이 MockOAuth2UserService가 시뮬레이션 데이터 반환");
        
        return response;
    }

    /**
     * 공개 엔드포인트
     */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V15");
        response.put("message", "OAuth2 공개 엔드포인트");
        response.put("authRequired", false);
        
        return response;
    }

    /**
     * OAuth2 vs 일반 로그인 비교
     */
    @GetMapping("/oauth2-vs-form")
    public Map<String, Object> oauth2VsForm() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V15");
        
        response.put("oauth2Login", Map.of(
            "user", "OAuth2User",
            "authentication", "OAuth2AuthenticationToken",
            "filter", "OAuth2LoginAuthenticationFilter",
            "provider", "외부 (GitHub, Google, Kakao 등)",
            "credentials", "Access Token",
            "장점", "비밀번호 관리 불필요, 간편 로그인",
            "단점", "Provider 의존, 네트워크 필요"
        ));
        
        response.put("formLogin", Map.of(
            "user", "UserDetails",
            "authentication", "UsernamePasswordAuthenticationToken",
            "filter", "UsernamePasswordAuthenticationFilter",
            "provider", "자체 (DB)",
            "credentials", "Password",
            "장점", "독립적, 완전한 제어",
            "단점", "비밀번호 관리 필요, 회원가입 프로세스"
        ));
        
        response.put("hybrid", Map.of(
            "전략", "둘 다 지원",
            "방법", "여러 SecurityFilterChain 또는 통합 UserDetailsService",
            "실무", "OAuth2로 회원가입 후 자체 로그인도 가능하게"
        ));
        
        return response;
    }
}

