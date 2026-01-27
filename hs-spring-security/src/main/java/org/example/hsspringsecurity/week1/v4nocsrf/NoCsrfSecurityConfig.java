package org.example.hsspringsecurity.week1.v4nocsrf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * V4 - CSRF 비활성화 (CSRF Disabled)
 * 
 * URL 패턴: /v4/nocsrf/**
 * 
 * 학습 목표:
 * - CSRF 비활성화 시 CsrfFilter가 제거됨을 확인
 * - Stateless REST API에서 CSRF 불필요한 이유 이해
 * - POST 요청 시 CSRF 토큰 없이도 정상 처리됨을 확인
 * - SessionCreationPolicy.STATELESS 설정의 의미
 * 
 * V1, V2, V3와의 차이점:
 * - .csrf(csrf -> csrf.disable()) 추가
 * - .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) 추가
 * 
 * 제거되는 필터:
 * 6. CsrfFilter - 완전히 제거됨
 * 
 * CSRF (Cross-Site Request Forgery) 보호란?
 * - 악의적인 웹사이트가 사용자의 브라우저를 이용하여 인증된 요청을 보내는 공격 방지
 * - 폼 기반 로그인, 세션 인증에서는 필수
 * - 각 요청마다 CSRF 토큰을 포함시켜 검증
 * 
 * Stateless REST API에서 CSRF가 불필요한 이유:
 * 1. 세션 쿠키를 사용하지 않음 (STATELESS)
 * 2. 매 요청마다 Authorization 헤더로 인증 (JWT, API Key 등)
 * 3. 브라우저가 자동으로 자격 증명을 전송하지 않음
 * 4. CORS 정책으로 다른 도메인의 요청 차단 가능
 * 
 * SessionCreationPolicy.STATELESS:
 * - SecurityContext를 세션에 저장하지 않음
 * - 매 요청마다 인증 필터(BasicAuthenticationFilter 등)가 인증 수행
 * - 서버가 세션을 생성하지 않아 확장성 향상
 * - REST API, 마이크로서비스 아키텍처에 적합
 * 
 * 주의사항:
 * - CSRF 비활성화는 Stateless REST API에서만 사용
 * - 브라우저 기반 폼 로그인에서는 반드시 CSRF 보호 활성화 필요
 * - Public API, 모바일 앱 백엔드, 마이크로서비스 간 통신에 적합
 */
@Configuration
public class NoCsrfSecurityConfig {

    @Bean
    @Order(4) // V3 다음 우선순위
    public SecurityFilterChain noCsrfSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // 이 SecurityFilterChain은 /v4/nocsrf/** 경로에만 적용
            .securityMatcher("/v4/nocsrf/**")
            
            // CSRF 보호 비활성화
            // → CsrfFilter 제거
            // → POST, PUT, DELETE 요청에 CSRF 토큰 불필요
            .csrf(csrf -> csrf.disable())
            
            // 세션 관리 정책: STATELESS
            // → SecurityContext를 세션에 저장하지 않음
            // → 매 요청마다 인증 필터가 실행됨
            // → 서버가 세션을 생성하지 않음 (JSESSIONID 쿠키 없음)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // URL 기반 접근 제어
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v4/nocsrf/public").permitAll() // 인증 불필요
                .anyRequest().authenticated() // 나머지는 모두 인증 필요
            )
            
            // HTTP Basic 인증 사용
            // STATELESS 환경에서는 매 요청마다 Authorization 헤더 필요
            .httpBasic(withDefaults());
        
        return http.build();
    }
}

