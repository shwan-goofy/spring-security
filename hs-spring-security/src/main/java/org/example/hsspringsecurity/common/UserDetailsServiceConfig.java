package org.example.hsspringsecurity.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * UserDetailsService 설정 (WEEK 1용)
 * 
 * *** WEEK 2에서는 비활성화됨 ***
 * WEEK 2에서는 각 버전별로 독립적인 UserDetailsService 구현을 사용합니다.
 * 
 * 메모리 기반의 사용자 정보를 설정한다.
 * 실제 운영 환경에서는 JdbcUserDetailsManager나 커스텀 UserDetailsService를 사용해야 한다.
 * 
 * 학습 포인트:
 * - UserDetailsService Bean을 등록하면 Spring Security가 자동으로 DaoAuthenticationProvider를 생성
 * - DaoAuthenticationProvider는 이 UserDetailsService와 PasswordEncoder를 사용하여 인증 처리
 * - User.builder()에 password()를 호출할 때 PasswordEncoder로 암호화된 비밀번호를 전달해야 함
 * - authorities()는 권한을 설정 (WEEK 3에서 자세히 다룸)
 * 
 * 테스트 계정:
 * - admin / 12345 (권한: ADMIN, USER)
 * - user / 12345 (권한: USER)
 */
// @Configuration  // WEEK 2: 비활성화 - 각 버전별 독립적인 UserDetailsService 사용
public class UserDetailsServiceConfig {

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // admin 사용자 생성 - ADMIN과 USER 권한 부여
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("12345")) // PasswordEncoder로 암호화
                .roles("ADMIN", "USER") // ROLE_ 접두사가 자동으로 추가됨
                .build();

        // 일반 user 생성 - USER 권한만 부여
        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder.encode("12345")) // PasswordEncoder로 암호화
                .roles("USER") // ROLE_USER로 저장됨
                .build();

        // InMemoryUserDetailsManager는 메모리에 사용자 정보를 저장
        return new InMemoryUserDetailsManager(admin, user);
    }
}

