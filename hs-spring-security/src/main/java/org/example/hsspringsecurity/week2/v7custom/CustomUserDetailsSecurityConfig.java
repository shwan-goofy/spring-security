package org.example.hsspringsecurity.week2.v7custom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * V7: Custom UserDetailsService 인증 설정 (권장 방식)
 * 
 * WEEK 2 학습 목표:
 * - 커스텀 도메인 모델(Customer)로 인증 구현
 * - UserDetailsService를 통한 유연한 DB 연동
 * - Role-based 권한 부여 (hasRole)
 * 
 * SecurityFilterChain 특징:
 * - @Order(7): V6 다음 순서
 * - securityMatcher("/v7/custom/**"): /v7/custom/** 경로만 매칭
 * - httpBasic(): HTTP Basic 인증 활성화
 * - hasRole("ADMIN"): ROLE_ADMIN 권한 필요
 * 
 * 필터 체인:
 * - DaoAuthenticationProvider 자동 생성됨
 * - CustomUserDetailsService 사용
 * - PasswordEncoder로 비밀번호 자동 검증
 */
@Configuration
public class CustomUserDetailsSecurityConfig {
    
    @Autowired
    @Qualifier("customUserDetailsService")
    private UserDetailsService customUserDetailsService;
    
    @Bean
    @Order(7)
    public SecurityFilterChain customUserDetailsSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v7/custom/**")
                .csrf(csrf -> csrf.disable())  // API 테스트 편의
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v7/custom/public").permitAll()
                        .requestMatchers("/v7/custom/admin").hasRole("ADMIN")  // ADMIN 권한 필요
                        .requestMatchers("/v7/custom/user").hasAnyRole("USER", "ADMIN")  // USER 또는 ADMIN
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults())
                .userDetailsService(customUserDetailsService);  // 명시적으로 UserDetailsService 지정
        
        return http.build();
    }
}

