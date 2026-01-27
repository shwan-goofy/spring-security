package org.example.hsspringsecurity.week2.v6jdbc;

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
 * V6: JDBC 스타일 인증 설정
 * 
 * WEEK 2 학습 목표:
 * - Spring Security 표준 스키마(users, authorities) 이해
 * - JdbcUserDetailsManager의 동작 방식 학습
 * - 고정 스키마의 한계 인식
 * 
 * SecurityFilterChain 특징:
 * - @Order(6): V1~V5 다음 순서
 * - securityMatcher("/v6/jdbc/**"): /v6/jdbc/** 경로만 매칭
 * - httpBasic(): HTTP Basic 인증 활성화
 * - csrf().disable(): API 테스트 편의를 위해 CSRF 비활성화
 * 
 * 필터 체인:
 * - DaoAuthenticationProvider 자동 생성됨 (UserDetailsService + PasswordEncoder)
 * - BasicAuthenticationFilter 활성화
 */
@Configuration
public class JdbcSecurityConfig {
    
    @Autowired
    @Qualifier("jdbcStyleUserDetailsService")
    private UserDetailsService jdbcStyleUserDetailsService;
    
    @Bean
    @Order(6)
    public SecurityFilterChain jdbcSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v6/jdbc/**")
                .csrf(csrf -> csrf.disable())  // API 테스트 편의
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v6/jdbc/public").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults())  // HTTP Basic 인증 활성화
                .userDetailsService(jdbcStyleUserDetailsService);  // 명시적으로 UserDetailsService 지정
        
        return http.build();
    }
}

