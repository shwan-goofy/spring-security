package org.example.hsspringsecurity.week2.v9authprovider;

import org.example.hsspringsecurity.common.domain.Customer;
import org.example.hsspringsecurity.common.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 커스텀 AuthenticationProvider 구현
 * 
 * WEEK 2 학습 목표:
 * - AuthenticationProvider를 직접 구현하여 완전한 제어권 확보
 * - 비즈니스 로직이 포함된 인증 (예: 도메인 검증, IP 체크 등)
 * - UserDetailsService보다 유연한 인증 구현
 * 
 * UserDetailsService vs AuthenticationProvider:
 * - UserDetailsService: DB 조회만 담당, 비밀번호 검증은 DaoAuthenticationProvider가 처리
 * - AuthenticationProvider: DB 조회, 비밀번호 검증, 추가 비즈니스 로직 모두 직접 구현
 * 
 * 주의:
 * - AuthenticationProvider Bean이 등록되면 DaoAuthenticationProvider가 생성되지 않음
 * - UserDetailsService Bean이 있어도 무시됨 (배타적 관계)
 * 
 * 구현 내용:
 * 1. DB에서 사용자 조회
 * 2. 비밀번호 검증 (직접 passwordEncoder.matches() 호출)
 * 3. 추가 비즈니스 로직: @example.com 도메인만 허용
 * 4. 인증 성공 시 UsernamePasswordAuthenticationToken 반환
 */
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String pwd = authentication.getCredentials().toString();
        
        // 1. DB에서 사용자 조회 (직접 Repository 호출)
        List<Customer> customers = customerRepository.findByEmail(username);
        if (customers.isEmpty()) {
            throw new BadCredentialsException("No user registered with this email!");
        }
        
        Customer customer = customers.get(0);
        
        // 2. 비밀번호 검증 (직접 passwordEncoder.matches() 호출)
        if (!passwordEncoder.matches(pwd, customer.getPwd())) {
            throw new BadCredentialsException("Invalid password!");
        }
        
        // 3. 추가 비즈니스 로직: 특정 도메인만 허용
        if (!username.endsWith("@example.com")) {
            throw new BadCredentialsException(
                "Only @example.com domain is allowed! Your domain: " + 
                username.substring(username.indexOf("@"))
            );
        }
        
        // 4. 권한 설정
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(customer.getRole()));
        
        // 5. 인증 성공: authenticated=true인 Authentication 객체 반환
        System.out.println("=== CustomAuthenticationProvider 인증 성공 ===");
        System.out.println("User: " + username);
        System.out.println("Role: " + customer.getRole());
        System.out.println("Business Rule: @example.com domain check passed");
        
        return new UsernamePasswordAuthenticationToken(username, pwd, authorities);
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        // 이 Provider가 UsernamePasswordAuthenticationToken을 처리함을 명시
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

