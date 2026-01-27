package org.example.hsspringsecurity.week2.v7custom;

import org.example.hsspringsecurity.common.domain.Customer;
import org.example.hsspringsecurity.common.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 커스텀 UserDetailsService 구현 (권장 방식)
 * 
 * WEEK 2 학습 목표:
 * - 비즈니스 도메인에 맞는 자유로운 테이블 구조 사용
 * - CustomerRepository를 통한 DB 조회
 * - UserDetails 변환 로직 구현
 * 
 * V6(JDBC 스타일)과의 차이:
 * - V6: 고정 스키마(users, authorities) 필요
 * - V7: 커스텀 도메인(Customer) 사용 가능
 * 
 * DaoAuthenticationProvider 동작:
 * 1. loadUserByUsername() 호출로 UserDetails 조회
 * 2. PasswordEncoder.matches()로 비밀번호 자동 검증
 * 3. 인증 성공 시 SecurityContext에 저장
 */
@Service("customUserDetailsService")  // Bean 이름 명시
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Customer Repository에서 이메일로 조회
        List<Customer> customers = customerRepository.findByEmail(username);
        
        if (customers.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email: " + username);
        }
        
        Customer customer = customers.get(0);
        
        // Customer의 role을 GrantedAuthority로 변환
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(customer.getRole()));
        
        // Spring Security UserDetails 객체 생성
        return User.builder()
                .username(customer.getEmail())
                .password(customer.getPwd())  // BCrypt 해시된 비밀번호
                .authorities(authorities)
                .build();
    }
}

