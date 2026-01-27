package org.example.hsspringsecurity.week2.v8register;

import org.example.hsspringsecurity.common.domain.Customer;
import org.example.hsspringsecurity.common.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 회원가입 서비스
 * 
 * WEEK 2 학습 목표:
 * - PasswordEncoder를 사용한 비밀번호 해싱
 * - 회원가입 비즈니스 로직 구현
 * - 이메일 중복 체크
 * 
 * 핵심 동작:
 * 1. 이메일 중복 체크
 * 2. 비밀번호 BCrypt 해싱 (passwordEncoder.encode())
 * 3. 기본 role 설정 (ROLE_USER)
 * 4. Customer 저장
 */
@Service
public class RegistrationService {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 새로운 Customer 등록
     * 
     * @param customer 등록할 Customer (평문 비밀번호)
     * @return 등록된 Customer (해싱된 비밀번호)
     * @throws IllegalArgumentException 이메일 중복 시
     */
    public Customer registerCustomer(Customer customer) {
        // 1. 이메일 중복 체크
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + customer.getEmail());
        }
        
        // 2. 비밀번호 해싱 (BCrypt)
        String hashedPassword = passwordEncoder.encode(customer.getPwd());
        customer.setPwd(hashedPassword);
        
        // 3. 기본 role 설정
        if (customer.getRole() == null || customer.getRole().isEmpty()) {
            customer.setRole("ROLE_USER");
        }
        
        // 4. Customer 저장
        Customer savedCustomer = customerRepository.save(customer);
        
        System.out.println("=== 새 사용자 등록 완료 ===");
        System.out.println("Email: " + savedCustomer.getEmail());
        System.out.println("Role: " + savedCustomer.getRole());
        System.out.println("Password Hash: " + savedCustomer.getPwd().substring(0, 20) + "...");
        
        return savedCustomer;
    }
}

