package org.example.hsspringsecurity.week1.v1basic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * V1 - 최소 설정 (Minimal Configuration)
 * 
 * URL 패턴: /v1/basic/**
 * 
 * 학습 목표:
 * - 기본 필터 체인 구조 이해
 * - formLogin/httpBasic 없이는 로그인 불가능함을 확인
 * - 인증 방법이 없을 때의 동작 확인
 * 
 * 활성화되는 주요 필터 (약 10개):
 * 1. DisableEncodingFilter
 * 2. WebAsyncManagerIntegrationFilter
 * 3. SecurityContextHolderFilter - SecurityContext를 ThreadLocal에 로드
 * 4. HeaderWriterFilter - 보안 헤더 추가
 * 5. CsrfFilter - CSRF 토큰 검증 (기본 활성화)
 * 6. LogoutFilter - 로그아웃 처리
 * 7. RequestCacheAwareFilter - 인증 전 요청 URL 저장/복원
 * 8. SecurityContextHolderAwareRequestFilter - HttpServletRequest를 Spring Security 래퍼로 감싸기
 * 9. AnonymousAuthenticationFilter - 인증되지 않은 사용자에게 익명 권한 부여
 * 10. SessionManagementFilter - 세션 관리
 * 11. ExceptionTranslationFilter - 인증/인가 예외 처리
 * 12. AuthorizationFilter - URL 기반 권한 검사
 * 
 * 주목할 점:
 * - UsernamePasswordAuthenticationFilter 없음 (formLogin 미설정)
 * - BasicAuthenticationFilter 없음 (httpBasic 미설정)
 * - 따라서 인증 방법이 없어서 로그인 불가능
 */
@Configuration
public class BasicSecurityConfig {

    @Bean
    @Order(1) // 가장 높은 우선순위
    public SecurityFilterChain basicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // 이 SecurityFilterChain은 /v1/basic/** 경로에만 적용
            .securityMatcher("/v1/basic/**")
            
            // URL 기반 접근 제어
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v1/basic/public").permitAll() // 인증 불필요
                .anyRequest().authenticated() // 나머지는 모두 인증 필요
            );
        
        // formLogin(), httpBasic()을 설정하지 않음
        // → 인증 필터가 없어서 실제로 로그인할 수 없음
        // → /v1/basic/secured 접근 시 403 Forbidden 발생
        
        return http.build();
    }
}

