package org.example.hsspringsecurity.debug;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Debug용 SecurityFilterChain
 * 
 * URL 패턴: /debug/**
 * 
 * 목적:
 * - 런타임에 활성화된 필터 체인 확인
 * - 각 SecurityFilterChain의 설정 정보 조회
 * - 학습 및 디버깅 지원
 * 
 * 보안 설정:
 * - 모든 사용자 접근 허용 (개발/학습 목적)
 * - 운영 환경에서는 반드시 제거하거나 접근 제한 필요
 * 
 * 주의사항:
 * - 이 엔드포인트는 보안 정보를 노출하므로 개발 환경에서만 사용
 * - 운영 환경 배포 전 반드시 제거 또는 ADMIN 권한으로 제한
 * - Spring Profile로 dev 환경에서만 활성화하는 것을 권장
 */
@Configuration
public class DebugSecurityConfig {

    @Bean
    @Order(99) // 가장 낮은 우선순위 (다른 모든 체인 이후)
    public SecurityFilterChain debugSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // 이 SecurityFilterChain은 /debug/** 경로에만 적용
            .securityMatcher("/debug/**")
            
            // 모든 사용자 접근 허용 (개발/학습 목적)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        
        // 주의: 운영 환경에서는 다음과 같이 제한해야 함
        // .authorizeHttpRequests(auth -> auth
        //     .anyRequest().hasRole("ADMIN")
        // )
        // .formLogin(withDefaults());
        
        return http.build();
    }
}

