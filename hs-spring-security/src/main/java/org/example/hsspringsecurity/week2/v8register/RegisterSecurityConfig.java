package org.example.hsspringsecurity.week2.v8register;

import org.example.hsspringsecurity.week2.v7custom.CustomUserDetailsService;
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
 * V8: 회원가입 API 보안 설정
 * 
 * WEEK 2 학습 목표:
 * - 회원가입 엔드포인트는 인증 불필요 (permitAll)
 * - 로그인 후 정보 조회는 인증 필요 (authenticated)
 * - CustomUserDetailsService 재사용
 * 
 * SecurityFilterChain 특징:
 * - @Order(8): V7 다음 순서
 * - securityMatcher("/v8/register/**"): /v8/register/** 경로만 매칭
 * - /signup, /public: permitAll() - 인증 불필요
 * - 나머지: authenticated() - 인증 필요
 * - csrf().disable(): POST 요청 테스트 편의
 * - httpBasic(): HTTP Basic 인증 활성화
 * 
 * 필터 체인:
 * - DaoAuthenticationProvider 사용
 * - CustomUserDetailsService로 로그인 시 사용자 조회
 * - PasswordEncoder.matches()로 비밀번호 검증
 */
@Configuration
public class RegisterSecurityConfig {
    
    @Autowired
    @Qualifier("customUserDetailsService")
    private UserDetailsService customUserDetailsService;
    
    @Bean
    @Order(8)
    public SecurityFilterChain registerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v8/register/**")
                .csrf(csrf -> csrf.disable())  // 회원가입 POST 요청 편의
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v8/register/signup", "/v8/register/public").permitAll()  // 회원가입은 인증 불필요
                        .anyRequest().authenticated()  // 나머지는 인증 필요
                )
                .httpBasic(withDefaults())
                .userDetailsService(customUserDetailsService);  // V7의 CustomUserDetailsService 재사용
        
        return http.build();
    }
}

