package org.example.hsspringsecurity.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder 설정
 * 
 * BCryptPasswordEncoder를 사용하여 비밀번호를 안전하게 암호화한다.
 * 이 Bean은 Spring Security가 자동으로 감지하여 DaoAuthenticationProvider에 주입한다.
 * 
 * 학습 포인트:
 * - 비밀번호는 절대 평문으로 저장하면 안 된다
 * - BCrypt는 단방향 해시 알고리즘으로, 무차별 대입 공격을 늦추는 메커니즘이 내장되어 있다
 * - Spring Security는 PasswordEncoder Bean이 반드시 필요하다
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

