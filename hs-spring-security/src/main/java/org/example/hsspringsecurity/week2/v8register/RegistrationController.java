package org.example.hsspringsecurity.week2.v8register;

import org.example.hsspringsecurity.common.domain.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * V8: 회원가입 및 로그인 컨트롤러
 * 
 * 테스트 시나리오:
 * 1. 회원가입
 *    curl -X POST -H "Content-Type: application/json" \
 *      -d '{"email":"test@test.com","pwd":"test123","name":"Test User"}' \
 *      http://localhost:8080/v8/register/signup
 *    → 201 Created
 * 
 * 2. 등록된 사용자 확인
 *    curl http://localhost:8080/debug/customers | python3 -m json.tool
 *    → test@test.com의 비밀번호가 $2a$10$... 형태로 저장됨
 * 
 * 3. 등록한 사용자로 로그인
 *    curl -u test@test.com:test123 http://localhost:8080/v8/register/myinfo
 *    → 200 OK, 사용자 정보 반환
 * 
 * 4. 중복 이메일로 가입 시도
 *    curl -X POST -H "Content-Type: application/json" \
 *      -d '{"email":"test@test.com","pwd":"test123","name":"Test User"}' \
 *      http://localhost:8080/v8/register/signup
 *    → 400 Bad Request (이메일 중복)
 * 
 * 학습 포인트:
 * - 회원가입: passwordEncoder.encode() → BCrypt 해시
 * - 로그인: DaoAuthenticationProvider가 matches() 자동 호출
 * - BCrypt 해시: $2a$10$... (60자 길이)
 */
@RestController
@RequestMapping("/v8/register")
public class RegistrationController {
    
    @Autowired
    private RegistrationService registrationService;
    
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V8");
        response.put("message", "회원가입 API 공개 엔드포인트");
        response.put("description", "인증 없이 접근 가능합니다");
        response.put("features", "회원가입 + 비밀번호 암호화");
        
        return response;
    }
    
    /**
     * 회원가입 API
     * 
     * Request Body:
     * {
     *   "email": "test@test.com",
     *   "pwd": "test123",
     *   "name": "Test User"
     * }
     */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> registerUser(@RequestBody Customer customer) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Customer savedCustomer = registrationService.registerCustomer(customer);
            
            response.put("status", "success");
            response.put("message", "User registered successfully");
            response.put("email", savedCustomer.getEmail());
            response.put("role", savedCustomer.getRole());
            response.put("passwordHashPrefix", savedCustomer.getPwd().substring(0, 10) + "...");
            response.put("note", "비밀번호가 BCrypt로 해싱되어 저장되었습니다");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException ex) {
            response.put("status", "error");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception ex) {
            response.put("status", "error");
            response.put("message", "An error occurred: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 로그인한 사용자 정보 조회
     * 
     * HTTP Basic 인증 필요:
     * curl -u email:password http://localhost:8080/v8/register/myinfo
     */
    @GetMapping("/myinfo")
    public Map<String, Object> getMyInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V8");
        response.put("message", "로그인한 사용자 정보");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("authenticated", authentication.isAuthenticated());
        
        response.put("learningNote", "DaoAuthenticationProvider가 자동으로 비밀번호를 검증했습니다 (matches())");
        
        return response;
    }
}

