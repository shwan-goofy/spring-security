package org.example.hsspringsecurity.week2.v10failure;

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
 * V10: Custom Authentication Failure Handler 보안 설정
 * 
 * WEEK 2 학습 목표:
 * - 인증 실패 시 커스텀 응답 처리
 * - 사용자 친화적 오류 메시지 제공
 * - 예외 타입별 메시지 구분
 * 
 * SecurityFilterChain 특징:
 * - @Order(10): V9 다음 순서
 * - securityMatcher("/v10/failure/**", "/login", "/logout"): 경로 매칭
 * - formLogin() + failureHandler(): 폼 로그인에 failure handler 적용
 * - CustomUserDetailsService 사용
 * 
 * 필터 체인:
 * - DaoAuthenticationProvider 사용
 * - UsernamePasswordAuthenticationFilter 활성화
 * - CustomAuthenticationFailureHandler로 실패 처리
 * 
 * 테스트 방법:
 * 1. 브라우저로 http://localhost:8080/v10/failure/secured 접근
 * 2. 잘못된 비밀번호 입력
 * 3. JSON 오류 응답 확인
 */
@Configuration
public class FailureHandlerSecurityConfig {
    
    @Autowired
    @Qualifier("customUserDetailsService")
    private UserDetailsService customUserDetailsService;
    
    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    
    @Bean
    @Order(10)
    public SecurityFilterChain failureHandlerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v10/failure/**", "/login", "/logout")
                .csrf(csrf -> csrf.disable())  // 테스트 편의
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v10/failure/public").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .failureHandler(customAuthenticationFailureHandler)  // 커스텀 실패 핸들러 적용
                )
                .httpBasic(withDefaults())  // HTTP Basic도 함께 지원
                .userDetailsService(customUserDetailsService);
        
        return http.build();
    }
}

