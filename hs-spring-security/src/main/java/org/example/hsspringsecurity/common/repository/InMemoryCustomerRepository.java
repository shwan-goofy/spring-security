package org.example.hsspringsecurity.common.repository;

import jakarta.annotation.PostConstruct;
import org.example.hsspringsecurity.common.domain.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * InMemory Customer Repository 구현체
 * WEEK 2: 인메모리 컬렉션으로 DB 시뮬레이션
 * 
 * - ConcurrentHashMap: 스레드 안전한 저장소
 * - AtomicLong: ID 자동 증가
 * - @PostConstruct: 초기 테스트 데이터 생성
 */
@Repository
public class InMemoryCustomerRepository implements CustomerRepository {
    
    private final Map<Long, Customer> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 초기 테스트 데이터 생성
     * - admin@example.com / admin123 / ROLE_ADMIN
     * - user@example.com / user123 / ROLE_USER
     */
    @PostConstruct
    public void init() {
        // Admin 사용자
        Customer admin = new Customer();
        admin.setEmail("admin@example.com");
        admin.setPwd(passwordEncoder.encode("admin123"));
        admin.setRole("ROLE_ADMIN");
        admin.setName("Admin User");
        save(admin);
        
        // 일반 사용자
        Customer user = new Customer();
        user.setEmail("user@example.com");
        user.setPwd(passwordEncoder.encode("user123"));
        user.setRole("ROLE_USER");
        user.setName("Regular User");
        save(user);
        
        System.out.println("=== InMemory Customer Repository 초기화 완료 ===");
        System.out.println("Admin: admin@example.com / admin123");
        System.out.println("User: user@example.com / user123");
    }
    
    @Override
    public Customer save(Customer customer) {
        if (customer.getId() == null) {
            // 신규 저장: ID 할당
            customer.setId(idGenerator.incrementAndGet());
        }
        store.put(customer.getId(), customer);
        return customer;
    }
    
    @Override
    public Optional<Customer> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }
    
    @Override
    public List<Customer> findByEmail(String email) {
        return store.values().stream()
                .filter(customer -> customer.getEmail().equals(email))
                .toList();
    }
    
    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(store.values());
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return store.values().stream()
                .anyMatch(customer -> customer.getEmail().equals(email));
    }
}

