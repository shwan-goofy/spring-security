package org.example.hsspringsecurity.week5.v15oauth2client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * V15: OAuth2 Client 보안 설정 (Mock)
 * 
 * WEEK 5 학습 목표:
 * - OAuth2 Client 모드 이해
 * - Authorization Code Grant Flow 학습
 * - OAuth2User와 UserDetails의 차이
 * - 소셜 로그인 구현 (Mock 환경)
 * 
 * SecurityFilterChain 특징:
 * - @Order(15): V14 다음 순서
 * - securityMatcher("/v15/oauth2/**", "/login", "/logout"): 경로 매칭
 * - oauth2Login(): OAuth2 로그인 기능 활성화
 * - userService(): 커스텀 OAuth2UserService 주입
 * 
 * OAuth2 Client 필터 체인:
 * 1. SecurityContextHolderFilter (순서 3)
 * 2. HeaderWriterFilter (순서 4)
 * 3. CsrfFilter (순서 6) ← 기본 활성화
 * 4. LogoutFilter (순서 7)
 * 5. OAuth2AuthorizationRequestRedirectFilter (순서 8) ← oauth2Login()으로 추가
 *    역할: /oauth2/authorization/{provider} 요청 시 Authorization Server로 리디렉션
 * 6. OAuth2LoginAuthenticationFilter (순서 13) ← oauth2Login()으로 추가
 *    역할: Authorization Code를 Access Token으로 교환하고 사용자 정보 조회
 * 7. RequestCacheAwareFilter (순서 14)
 * 8. AnonymousAuthenticationFilter (순서 17)
 * 9. SessionManagementFilter (순서 19)
 * 10. ExceptionTranslationFilter (순서 20)
 * 11. AuthorizationFilter (순서 21)
 * 
 * OAuth2 로그인 플로우 (실제 환경):
 * 
 * [사용자 → 애플리케이션]
 * 1. 사용자가 보호된 리소스 접근 또는 /oauth2/authorization/github 클릭
 * 
 * [애플리케이션 → Authorization Server]
 * 2. OAuth2AuthorizationRequestRedirectFilter가 GitHub 로그인 페이지로 리디렉션
 *    GET https://github.com/login/oauth/authorize?
 *        client_id=YOUR_CLIENT_ID&
 *        redirect_uri=http://localhost:8080/login/oauth2/code/github&
 *        scope=read:user,user:email&
 *        state=random_state
 * 
 * [사용자 → Authorization Server]
 * 3. 사용자가 GitHub에서 로그인 및 권한 동의
 * 
 * [Authorization Server → 애플리케이션]
 * 4. GitHub이 Authorization Code와 함께 콜백
 *    http://localhost:8080/login/oauth2/code/github?code=abc123&state=random_state
 * 
 * [애플리케이션 내부]
 * 5. OAuth2LoginAuthenticationFilter가 처리:
 *    a. Authorization Code → Access Token 교환
 *       POST https://github.com/login/oauth/access_token
 *       Body: code=abc123&client_id=...&client_secret=...
 *       Response: {"access_token": "gho_...", "token_type": "bearer"}
 *    
 *    b. OAuth2UserService.loadUser() 호출
 *       GET https://api.github.com/user
 *       Header: Authorization: Bearer gho_...
 *       Response: {"login": "john", "email": "john@example.com", ...}
 *    
 *    c. OAuth2User 객체 생성
 *    
 *    d. OAuth2AuthenticationToken 생성 후 SecurityContext에 저장
 *    
 *    e. 세션에 SecurityContext 저장 (JSESSIONID 쿠키 발급)
 * 
 * 6. 성공 후 defaultSuccessUrl()로 리디렉션 (기본: /)
 * 
 * Mock 환경의 차이:
 * - 실제 GitHub/Google 호출 없음
 * - MockOAuth2UserService가 하드코딩된 데이터 반환
 * - Authorization Code Flow 생략 (학습 목적 간소화)
 * - 브라우저 리디렉션 없이 바로 인증 완료
 * 
 * 실무 설정 예시 (application.yml):
 * spring:
 *   security:
 *     oauth2:
 *       client:
 *         registration:
 *           github:
 *             client-id: YOUR_GITHUB_CLIENT_ID
 *             client-secret: YOUR_GITHUB_CLIENT_SECRET
 *             scope: read:user,user:email
 *           google:
 *             client-id: YOUR_GOOGLE_CLIENT_ID
 *             client-secret: YOUR_GOOGLE_CLIENT_SECRET
 *             scope: openid,profile,email
 * 
 * 주의사항:
 * - OAuth2 로그인은 세션 기반 (JSESSIONID 사용)
 * - CSRF 보호 필수 (기본 활성화)
 * - 소셜 로그인 시 자동 회원가입 로직 필요
 * - Provider 장애 시 로그인 불가 (백업 로그인 방법 필요)
 * 
 * 테스트 방법 (Mock):
 * 브라우저로 http://localhost:8080/v15/oauth2/user 접근
 * → MockOAuth2UserService가 가짜 사용자 정보 반환
 */
@Configuration
public class OAuth2ClientSecurityConfig {

    @Autowired
    private MockOAuth2UserService mockOAuth2UserService;

    @Bean
    @Order(15)
    public SecurityFilterChain oauth2ClientSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v15/oauth2/**", "/login", "/logout")
                
                // CSRF 비활성화 (테스트 편의)
                // 실제 OAuth2 로그인에서는 CSRF 활성화 권장
                .csrf(csrf -> csrf.disable())
                
                // URL 기반 접근 제어
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트
                        .requestMatchers(
                            "/v15/oauth2/public", 
                            "/v15/oauth2/login-simulation",
                            "/v15/oauth2/oauth2-vs-form"
                        ).permitAll()
                        
                        // 나머지는 OAuth2 인증 필요
                        .anyRequest().authenticated()
                )
                
                // OAuth2 로그인 활성화
                // 이 설정으로 두 개의 필터가 자동 추가됨:
                // 1. OAuth2AuthorizationRequestRedirectFilter (순서 8)
                // 2. OAuth2LoginAuthenticationFilter (순서 13)
                .oauth2Login(oauth2 -> oauth2
                        // 커스텀 OAuth2UserService 주입
                        // 실제 환경에서는 DefaultOAuth2UserService 사용
                        // Mock 환경에서는 MockOAuth2UserService 사용
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(mockOAuth2UserService)
                        )
                        
                        // 로그인 성공 후 리디렉션할 URL (선택)
                        // .defaultSuccessUrl("/v15/oauth2/user", true)
                        
                        // 로그인 실패 시 URL (선택)
                        // .failureUrl("/login?error")
                )
                
                // HTTP Basic 인증도 함께 지원 (테스트 편의)
                // 실제로는 OAuth2만 사용
                .httpBasic(withDefaults());

        return http.build();
    }
}

