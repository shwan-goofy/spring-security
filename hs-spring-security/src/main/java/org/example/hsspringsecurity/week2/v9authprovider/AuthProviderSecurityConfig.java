package org.example.hsspringsecurity.week2.v9authprovider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * V9: Custom AuthenticationProvider 보안 설정
 * 
 * WEEK 2 학습 목표:
 * - AuthenticationProvider를 직접 구현하여 인증 로직 제어
 * - 비즈니스 로직이 포함된 인증 (도메인 검증)
 * - UserDetailsService와의 차이점 이해
 * 
 * SecurityFilterChain 특징:
 * - @Order(9): V8 다음 순서
 * - securityMatcher("/v9/authprovider/**"): /v9/authprovider/** 경로만 매칭
 * - httpBasic(): HTTP Basic 인증 활성화
 * - CustomAuthenticationProvider 사용 (자동 등록됨)
 * 
 * 필터 체인:
 * - DaoAuthenticationProvider 생성되지 않음 ❌
 * - CustomAuthenticationProvider만 사용됨
 * - UserDetailsService가 있어도 무시됨 (배타적 관계)
 * 
 * 주의사항:
 * - AuthenticationProvider Bean이 등록되면 Spring Security는 기본 DaoAuthenticationProvider를 생성하지 않음
 * - 모든 인증 로직을 개발자가 직접 구현해야 함
 */
@Configuration
public class AuthProviderSecurityConfig {
    
    @Autowired
    private CustomAuthenticationProvider customAuthenticationProvider;
    
    @Bean
    @Order(9)
    public SecurityFilterChain authProviderSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v9/authprovider/**")
                .csrf(csrf -> csrf.disable())  // API 테스트 편의
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v9/authprovider/public").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults())
                .authenticationProvider(customAuthenticationProvider);  // CustomAuthenticationProvider 명시적 등록
        
        return http.build();
    }
}

