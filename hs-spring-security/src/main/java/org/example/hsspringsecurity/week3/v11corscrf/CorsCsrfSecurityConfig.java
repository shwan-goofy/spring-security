package org.example.hsspringsecurity.week3.v11corscrf;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * V11: CORS + CSRF 통합 보안 설정
 * 
 * WEEK 3 학습 목표:
 * - CORS(Cross-Origin Resource Sharing) 설정하여 다른 출처의 요청 허용
 * - CSRF(Cross-Site Request Forgery) 공격 방어
 * - CookieCsrfTokenRepository를 사용하여 토큰을 쿠키에 저장
 * - 커스텀 필터로 CSRF 토큰을 응답 헤더에 추가
 * 
 * SecurityFilterChain 특징:
 * - @Order(11): V10 다음 순서
 * - securityMatcher("/v11/corscrf/**", "/login", "/logout"): 경로 매칭
 * - cors(): CORS 필터 활성화
 * - csrf(): CSRF 필터 활성화 + 쿠키 저장소 사용
 * - addFilterAfter(): CsrfCookieFilter 추가
 * 
 * 필터 체인 구조:
 * 1. SecurityContextHolderFilter (순서 3) - SecurityContext 로드
 * 2. HeaderWriterFilter (순서 4) - 보안 헤더 추가
 * 3. CorsFilter (순서 5) ← cors() 설정으로 활성화
 * 4. CsrfFilter (순서 6) ← csrf() 설정으로 활성화
 * 5. LogoutFilter (순서 7)
 * 6. UsernamePasswordAuthenticationFilter (순서 9)
 * 7. BasicAuthenticationFilter (순서 12)
 * 8. CsrfCookieFilter ← addFilterAfter()로 추가 (순서 12-13 사이)
 * 9. RequestCacheAwareFilter (순서 14)
 * 10. AnonymousAuthenticationFilter (순서 17)
 * 11. ExceptionTranslationFilter (순서 20)
 * 12. AuthorizationFilter (순서 21)
 * 
 * CORS 설정:
 * - Allowed Origins: http://localhost:4200 (Angular/React 개발 서버)
 * - Allowed Methods: * (모든 HTTP 메소드)
 * - Allow Credentials: true (쿠키 포함 요청 허용)
 * - Allowed Headers: * (모든 헤더)
 * - Max Age: 3600초 (Pre-flight 결과 캐시)
 * 
 * CSRF 설정:
 * - Repository: CookieCsrfTokenRepository.withHttpOnlyFalse()
 *   → XSRF-TOKEN 쿠키에 토큰 저장 (JavaScript에서 읽을 수 있도록 HttpOnly=false)
 * - Ignoring: /v11/corscrf/public (공개 API는 CSRF 보호 제외)
 * - Handler: CsrfTokenRequestAttributeHandler (Spring Security 6.0+ 권장)
 * 
 * 테스트 방법:
 * 1. CORS Pre-flight:
 *    curl -X OPTIONS http://localhost:8080/v11/corscrf/transfer \
 *      -H "Origin: http://localhost:4200" \
 *      -H "Access-Control-Request-Method: POST"
 * 
 * 2. CSRF 토큰 조회:
 *    curl http://localhost:8080/v11/corscrf/csrf-token -u user@example.com:user123
 * 
 * 3. CSRF 토큰과 함께 POST (실제로는 브라우저가 자동 처리):
 *    - 쿠키: XSRF-TOKEN=abc123
 *    - 헤더: X-XSRF-TOKEN=abc123
 */
@Configuration
public class CorsCsrfSecurityConfig {

    @Autowired
    @Qualifier("customUserDetailsService")
    private UserDetailsService customUserDetailsService;

    @Bean
    @Order(11)
    public SecurityFilterChain corsCsrfSecurityFilterChain(HttpSecurity http) throws Exception {
        // CSRF 토큰 핸들러 설정 (Spring Security 6.0+ 권장 방식)
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
                .securityMatcher("/v11/corscrf/**", "/login", "/logout")
                
                // 세션 관리: 필요시 생성 (CSRF는 세션에 토큰 저장 가능)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                
                // CORS 설정: CorsFilter 활성화
                .cors(corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {
                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                        CorsConfiguration config = new CorsConfiguration();
                        
                        // 허용할 프론트엔드 출처 (개발 환경)
                        // 운영 환경에서는 실제 도메인으로 변경 필요
                        config.setAllowedOrigins(Collections.singletonList("http://localhost:4200"));
                        
                        // 모든 HTTP 메소드 허용 (GET, POST, PUT, DELETE, OPTIONS 등)
                        config.setAllowedMethods(Collections.singletonList("*"));
                        
                        // 인증 정보(쿠키, Authorization 헤더) 포함 요청 허용
                        config.setAllowCredentials(true);
                        
                        // 모든 헤더 허용
                        config.setAllowedHeaders(Collections.singletonList("*"));
                        
                        // Pre-flight 요청 결과를 1시간 동안 캐시
                        config.setMaxAge(3600L);
                        
                        return config;
                    }
                }))
                
                // CSRF 설정: CsrfFilter 활성화 + 커스터마이징
                .csrf(csrf -> csrf
                        // CSRF 토큰 핸들러 설정
                        .csrfTokenRequestHandler(requestHandler)
                        
                        // 특정 경로는 CSRF 보호 제외 (공개 API)
                        .ignoringRequestMatchers("/v11/corscrf/public")
                        
                        // CSRF 토큰을 쿠키에 저장 (JavaScript가 읽을 수 있도록 HttpOnly=false)
                        // 쿠키 이름: XSRF-TOKEN
                        // 헤더 이름: X-XSRF-TOKEN
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                
                // URL 기반 접근 제어
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v11/corscrf/public").permitAll()  // 공개 API
                        .anyRequest().authenticated()  // 나머지는 인증 필요
                )
                
                // BasicAuthenticationFilter 이후에 CsrfCookieFilter 추가
                // 역할: CSRF 토큰을 응답 헤더에도 명시적으로 추가
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                
                // Form Login 활성화 (UsernamePasswordAuthenticationFilter)
                .formLogin(withDefaults())
                
                // HTTP Basic 인증 활성화 (BasicAuthenticationFilter)
                .httpBasic(withDefaults())
                
                // CustomUserDetailsService 사용
                .userDetailsService(customUserDetailsService);

        return http.build();
    }
}

