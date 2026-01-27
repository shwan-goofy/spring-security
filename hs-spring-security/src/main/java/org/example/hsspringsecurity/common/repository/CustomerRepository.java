package org.example.hsspringsecurity.common.repository;

import org.example.hsspringsecurity.common.domain.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Customer Repository 인터페이스
 * WEEK 2: Repository 추상화 패턴 학습용
 * 
 * 실제 구현체는 InMemoryCustomerRepository (인메모리 컬렉션 사용)
 * 추후 JPA Repository로 교체 가능
 */
public interface CustomerRepository {
    
    /**
     * Customer 저장 (신규 또는 업데이트)
     * @param customer 저장할 Customer
     * @return 저장된 Customer (ID 할당됨)
     */
    Customer save(Customer customer);
    
    /**
     * ID로 Customer 조회
     * @param id Customer ID
     * @return Optional<Customer>
     */
    Optional<Customer> findById(Long id);
    
    /**
     * 이메일로 Customer 조회 (로그인 시 사용)
     * @param email 이메일 주소
     * @return Customer 리스트 (일반적으로 0개 또는 1개)
     */
    List<Customer> findByEmail(String email);
    
    /**
     * 모든 Customer 조회
     * @return Customer 리스트
     */
    List<Customer> findAll();
    
    /**
     * 이메일 중복 체크 (회원가입 시 사용)
     * @param email 체크할 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);
}

