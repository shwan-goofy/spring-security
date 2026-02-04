package org.example.hsspringsecurity.week4.v13jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * V13: JWT 토큰 인증 보안 설정
 * 
 * WEEK 4 학습 목표:
 * - Stateless(무상태) 인증 아키텍처 구현
 * - 커스텀 필터로 JWT 생성 및 검증
 * - 세션을 사용하지 않는 보안 구조 이해
 * - CSRF 보호가 불필요한 이유 학습
 * 
 * SecurityFilterChain 특징:
 * - @Order(13): V12 다음 순서
 * - securityMatcher("/v13/jwt/**", "/login", "/logout"): 경로 매칭
 * - sessionManagement(STATELESS): 세션 사용 안 함
 * - csrf().disable(): CSRF 보호 비활성화
 * - addFilterBefore(): JwtTokenValidatorFilter 추가
 * - addFilterAfter(): JwtTokenGeneratorFilter 추가
 * 
 * 필터 체인 구조 (Stateless 모드):
 * 1. SecurityContextHolderFilter (순서 3)
 * 2. HeaderWriterFilter (순서 4)
 * 3. (CorsFilter 제거 - 설정 안 함)
 * 4. (CsrfFilter 제거 - csrf().disable())
 * 5. LogoutFilter (순서 7)
 * 6. JwtTokenValidatorFilter ← 커스텀 추가 (순서 8-9 사이)
 * 7. UsernamePasswordAuthenticationFilter (순서 9)
 * 8. BasicAuthenticationFilter (순서 12)
 * 9. JwtTokenGeneratorFilter ← 커스텀 추가 (순서 12-13 사이)
 * 10. RequestCacheAwareFilter (순서 14)
 * 11. AnonymousAuthenticationFilter (순서 17)
 * 12. (SessionManagementFilter - STATELESS 모드)
 * 13. ExceptionTranslationFilter (순서 20)
 * 14. AuthorizationFilter (순서 21)
 * 
 * JWT 인증 플로우:
 * 
 * [로그인 - JWT 발급]
 * Client → BasicAuthenticationFilter → 인증 성공 → JwtTokenGeneratorFilter → JWT 생성 → Response Header
 * 
 * [보호된 리소스 접근]
 * Client (JWT 포함) → JwtTokenValidatorFilter → JWT 검증 → Authentication 생성 → Controller
 * 
 * Stateless 모드 특징:
 * - SessionCreationPolicy.STATELESS 설정
 * - JSESSIONID 쿠키 발급 안 함
 * - 서버는 세션을 생성/저장하지 않음
 * - SecurityContext는 요청마다 생성되고 요청 종료 시 폐기
 * - JWT에 모든 인증 정보가 포함됨 (self-contained)
 * 
 * CSRF 비활성화 이유:
 * - CSRF 공격은 브라우저가 자동으로 쿠키를 보내는 것을 이용
 * - JWT는 Authorization 헤더에 수동으로 담아 보냄
 * - 공격자는 사용자의 JWT를 헤더에 담을 수 없음 (Same-Origin Policy)
 * - 따라서 JWT 방식에서는 CSRF 보호가 불필요
 * 
 * 주의사항:
 * - JWT를 localStorage에 저장 시 XSS 공격에 취약
 * - 짧은 만료 시간 설정 권장 (15분~1시간)
 * - Refresh Token 패턴으로 UX 개선
 * - HTTPS 필수 (JWT 탈취 방지)
 * 
 * 테스트 방법:
 * 1. 로그인하여 JWT 받기:
 *    curl -u user@example.com:user123 http://localhost:8080/v13/jwt/login -v
 *    → Response Header: Authorization: eyJhbGc...
 * 
 * 2. JWT로 보호된 리소스 접근:
 *    curl http://localhost:8080/v13/jwt/secured \
 *      -H "Authorization: eyJhbGc..."
 *    → 200 OK
 * 
 * 3. JWT 없이 접근:
 *    curl http://localhost:8080/v13/jwt/secured
 *    → 403 Forbidden
 */
@Configuration
public class JwtSecurityConfig {

    @Autowired
    @Qualifier("customUserDetailsService")
    private UserDetailsService customUserDetailsService;

    @Bean
    @Order(13)
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v13/jwt/**", "/login", "/logout")
                
                // 세션 관리: STATELESS 모드
                // 서버는 세션을 생성하지 않고, JSESSIONID 쿠키도 발급하지 않음
                // SecurityContext는 요청마다 생성되고 요청 종료 시 폐기됨
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // CSRF 보호 비활성화
                // JWT를 Authorization 헤더에 담아 보내므로 CSRF 공격에 안전
                // (브라우저가 자동으로 JWT를 보내지 않음)
                .csrf(csrf -> csrf.disable())
                
                // URL 기반 접근 제어
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트
                        .requestMatchers("/v13/jwt/public", "/v13/jwt/jwt-vs-session").permitAll()
                        
                        // /login은 인증 필요 (BasicAuthenticationFilter가 처리)
                        // /secured는 JWT 검증 필요 (JwtTokenValidatorFilter가 처리)
                        .anyRequest().authenticated()
                )
                
                // JWT 검증 필터를 UsernamePasswordAuthenticationFilter 이전에 추가
                // 클라이언트가 JWT를 보내면 이 필터가 먼저 검증
                // JWT가 유효하면 Authentication 객체를 SecurityContext에 저장
                .addFilterBefore(new JwtTokenValidatorFilter(), UsernamePasswordAuthenticationFilter.class)
                
                // JWT 생성 필터를 BasicAuthenticationFilter 이후에 추가
                // BasicAuthenticationFilter가 인증 성공 후 이 필터가 JWT 생성
                // 생성된 JWT를 응답 헤더(Authorization)에 추가
                .addFilterAfter(new JwtTokenGeneratorFilter(), BasicAuthenticationFilter.class)
                
                // HTTP Basic 인증 활성화
                // /login 엔드포인트에서 사용
                .httpBasic(withDefaults())
                
                // CustomUserDetailsService 사용
                .userDetailsService(customUserDetailsService);

        return http.build();
    }
}

