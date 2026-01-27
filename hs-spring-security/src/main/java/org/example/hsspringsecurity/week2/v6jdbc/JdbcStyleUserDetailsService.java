package org.example.hsspringsecurity.week2.v6jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JDBC 스타일 UserDetailsService 구현
 * 
 * JdbcUserDetailsManager의 동작 방식을 시뮬레이션:
 * - 정해진 스키마(users, authorities)에서 사용자 조회
 * - UserDetails 객체로 변환하여 반환
 * 
 * WEEK 2 학습 목표:
 * - JdbcUserDetailsManager의 내부 동작 이해
 * - 고정 스키마의 한계 인식 → V7에서 커스텀 방식 학습
 */
@Service("jdbcStyleUserDetailsService")  // Bean 이름 명시 (다른 UserDetailsService와 구분)
public class JdbcStyleUserDetailsService implements UserDetailsService {
    
    @Autowired
    private InMemoryUserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        InMemoryUserRepository.UserData userData = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        // authorities 리스트를 GrantedAuthority로 변환
        List<GrantedAuthority> authorities = userData.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        
        // Spring Security UserDetails 객체 생성
        return User.builder()
                .username(userData.getUsername())
                .password(userData.getPassword())
                .authorities(authorities)
                .disabled(!userData.isEnabled())
                .build();
    }
}

