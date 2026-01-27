package org.example.hsspringsecurity.week1.v5multichain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * V5 - 다중 SecurityFilterChain (Multiple Chains)
 * 
 * URL 패턴: /v5/api/**, /v5/admin/**
 * 
 * 학습 목표:
 * - URL 패턴별로 다른 보안 전략 적용 가능함을 확인
 * - @Order로 우선순위 제어
 * - API와 Admin 페이지의 필터 체인 차이 비교
 * - 하나의 애플리케이션에서 여러 인증 방식 동시 운영
 * 
 * 핵심 개념:
 * - FilterChainProxy가 여러 SecurityFilterChain을 관리
 * - 각 요청마다 RequestMatcher로 적절한 체인 선택
 * - @Order로 우선순위 지정 (낮은 숫자 = 높은 우선순위)
 * - 먼저 매칭되는 체인이 실행됨
 * 
 * 실전 사용 사례:
 * - /api/** : JWT 기반 Stateless REST API
 * - /admin/** : 세션 기반 폼 로그인 관리자 페이지
 * - /oauth2/** : OAuth2 소셜 로그인
 * - /public/** : 인증 불필요
 * 
 * 주의사항:
 * - securityMatcher()로 각 체인의 적용 범위를 명확히 지정
 * - 겹치는 패턴이 없도록 주의 (겹치면 @Order가 낮은 것이 우선)
 * - 마지막에 기본 체인을 추가하여 매칭되지 않은 요청 처리
 */
@Configuration
public class MultiChainSecurityConfig {

    /**
     * API용 SecurityFilterChain - Stateless REST API 스타일
     * 
     * 특징:
     * - CSRF 비활성화 (Stateless)
     * - 세션 사용 안 함 (STATELESS)
     * - HTTP Basic 인증
     * - 매 요청마다 Authorization 헤더 필요
     * 
     * 활성화되는 주요 필터:
     * - SecurityContextHolderFilter
     * - HeaderWriterFilter
     * - LogoutFilter (STATELESS 모드)
     * - BasicAuthenticationFilter ← 인증 처리
     * - RequestCacheAwareFilter
     * - AnonymousAuthenticationFilter
     * - ExceptionTranslationFilter
     * - AuthorizationFilter
     * 
     * 제거되는 필터:
     * - CsrfFilter (disable)
     * - SessionManagementFilter (STATELESS 모드로 동작)
     */
    @Bean
    @Order(5) // API용 체인
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // /v5/api/** 경로에만 적용
            .securityMatcher("/v5/api/**")
            
            // CSRF 비활성화 (REST API)
            .csrf(csrf -> csrf.disable())
            
            // Stateless 세션 관리
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // URL 기반 접근 제어
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v5/api/public").permitAll() // 공개 API
                .anyRequest().authenticated() // 나머지는 인증 필요
            )
            
            // HTTP Basic 인증
            .httpBasic(withDefaults());
        
        return http.build();
    }

    /**
     * Admin용 SecurityFilterChain - Stateful 폼 로그인 스타일
     * 
     * 특징:
     * - CSRF 활성화 (세션 기반)
     * - 세션 사용 (기본 설정)
     * - 폼 로그인
     * - ADMIN 권한 필요
     * - 로그인 페이지 자동 생성
     * 
     * 활성화되는 주요 필터:
     * - SecurityContextHolderFilter
     * - HeaderWriterFilter
     * - CsrfFilter ← CSRF 보호
     * - LogoutFilter
     * - UsernamePasswordAuthenticationFilter ← 폼 로그인
     * - DefaultLoginPageGeneratingFilter
     * - DefaultLogoutPageGeneratingFilter
     * - RequestCacheAwareFilter
     * - AnonymousAuthenticationFilter
     * - SessionManagementFilter
     * - ExceptionTranslationFilter
     * - AuthorizationFilter
     * 
     * V2와의 차이점:
     * - hasRole("ADMIN") 권한 검사 추가 (WEEK 3 선행 학습)
     */
    @Bean
    @Order(6) // Admin용 체인 (API 다음)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // /v5/admin/**, /login, /logout 경로에 적용
            // /login, /logout은 formLogin()의 기본 로그인/로그아웃 페이지를 위해 필요
            .securityMatcher("/v5/admin/**", "/login", "/logout")
            
            // URL 기반 접근 제어 + 권한 검사
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v5/admin/login").permitAll() // 로그인 페이지는 공개
                .anyRequest().hasRole("ADMIN") // ADMIN 권한 필요 (ROLE_ADMIN)
            )
            
            // 폼 로그인 활성화
            .formLogin(withDefaults());
        
        // CSRF는 기본 활성화 (세션 기반 폼 로그인이므로 필수)
        return http.build();
    }
}

