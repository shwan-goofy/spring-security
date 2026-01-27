package org.example.hsspringsecurity.week1.v2formlogin;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * V2 - 폼 로그인 추가 (Form Login)
 * 
 * URL 패턴: /v2/formlogin/**
 * 
 * 학습 목표:
 * - formLogin() 설정 시 UsernamePasswordAuthenticationFilter가 추가됨을 확인
 * - 기본 로그인 페이지 (/login) 자동 생성 확인
 * - POST /login으로 username/password 인증 처리
 * - 세션 기반 인증 이해
 * 
 * V1과의 차이점:
 * - .formLogin(withDefaults()) 추가
 * 
 * 추가되는 필터:
 * 9. UsernamePasswordAuthenticationFilter - POST /login 처리, 폼 로그인 인증
 * 10. DefaultLoginPageGeneratingFilter - 기본 로그인 페이지 (/login) 자동 생성
 * 11. DefaultLogoutPageGeneratingFilter - 기본 로그아웃 페이지 생성
 * 
 * UsernamePasswordAuthenticationFilter 동작 과정:
 * 1. POST /login 요청 감지
 * 2. username, password 파라미터 추출
 * 3. UsernamePasswordAuthenticationToken (미인증) 생성
 * 4. AuthenticationManager에 인증 위임
 * 5. DaoAuthenticationProvider가 UserDetailsService로 사용자 조회
 * 6. PasswordEncoder로 비밀번호 검증
 * 7. 인증 성공 시 Authentication (인증 완료) 객체 생성
 * 8. SecurityContextHolder에 저장
 * 9. 세션에 SecurityContext 저장 (세션 기반 인증)
 */
@Configuration
public class FormLoginSecurityConfig {

    @Bean
    @Order(2) // V1 다음 우선순위
    public SecurityFilterChain formLoginSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // 이 SecurityFilterChain은 /v2/formlogin/**, /login, /logout 경로에 적용
            // /login, /logout은 formLogin()의 기본 로그인/로그아웃 페이지를 위해 필요
            .securityMatcher("/v2/formlogin/**", "/login", "/logout")
            
            // URL 기반 접근 제어
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v2/formlogin/public").permitAll() // 인증 불필요
                .anyRequest().authenticated() // 나머지는 모두 인증 필요
            )
            
            // 폼 로그인 활성화 (기본 설정 사용)
            // → UsernamePasswordAuthenticationFilter 추가
            // → DefaultLoginPageGeneratingFilter 추가 (기본 로그인 페이지)
            // → 로그인 URL: /login (GET: 로그인 페이지, POST: 인증 처리)
            // → 로그아웃 URL: /logout
            // → 로그인 성공 시: 원래 요청한 페이지로 리디렉션
            .formLogin(withDefaults());
        
        return http.build();
    }
}

