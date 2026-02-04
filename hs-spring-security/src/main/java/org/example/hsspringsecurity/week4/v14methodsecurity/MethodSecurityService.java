package org.example.hsspringsecurity.week4.v14methodsecurity;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * V14: 메소드 레벨 보안 서비스
 * 
 * 메소드 레벨 보안 어노테이션:
 * - @PreAuthorize: 메소드 실행 전 권한 체크
 * - @PostAuthorize: 메소드 실행 후 반환 값 기반 권한 체크
 * - @PreFilter: 메소드 파라미터(컬렉션) 필터링
 * - @PostFilter: 메소드 반환 값(컬렉션) 필터링
 * 
 * SpEL 표현식:
 * - hasRole('ADMIN'): ROLE_ADMIN 역할 체크
 * - hasAuthority('DELETE_USER'): 특정 권한 체크
 * - authentication.name: 현재 인증된 사용자 이름
 * - #username: 메소드 파라미터 참조
 * - returnObject: 메소드 반환 값 참조 (@PostAuthorize에서 사용)
 * 
 * 학습 포인트:
 * - URL 레벨 보안(SecurityConfig)과 메소드 레벨 보안(Service) 병행
 * - URL 레벨은 1차 방어선, 메소드 레벨은 2차 방어선 (심층 방어)
 * - 메소드 레벨 보안은 AOP(Aspect-Oriented Programming) 기반
 * - 같은 클래스 내부 호출 시 AOP 프록시 우회 주의
 */
@Service
public class MethodSecurityService {

    /**
     * ADMIN 역할만 접근 가능한 메소드
     * 
     * @PreAuthorize("hasRole('ADMIN')")
     * - 메소드 실행 전 ROLE_ADMIN 권한 체크
     * - 권한이 없으면 AccessDeniedException 발생 (403 Forbidden)
     * 
     * 테스트:
     * - ADMIN 계정: 200 OK
     * - USER 계정: 403 Forbidden
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getAdminData() {
        return Map.of(
            "message", "관리자 전용 데이터",
            "data", List.of("sensitive", "information", "admin-only"),
            "annotation", "@PreAuthorize(\"hasRole('ADMIN')\")",
            "securityLevel", "ADMIN_ONLY"
        );
    }

    /**
     * 파라미터 username이 현재 인증된 사용자와 같을 때만 접근 가능
     * 
     * @PreAuthorize("#username == authentication.name")
     * - #username: 메소드 파라미터 'username' 참조
     * - authentication.name: 현재 SecurityContext의 사용자 이름
     * - 둘이 일치할 때만 메소드 실행
     * 
     * 테스트:
     * - user@example.com으로 로그인 후:
     *   getUserData("user@example.com") → 200 OK
     *   getUserData("other@example.com") → 403 Forbidden
     */
    @PreAuthorize("#username == authentication.name")
    public Map<String, Object> getUserData(String username) {
        return Map.of(
            "message", "사용자 데이터",
            "username", username,
            "annotation", "@PreAuthorize(\"#username == authentication.name\")",
            "note", "파라미터 username이 현재 인증된 사용자와 같아야 접근 가능",
            "data", Map.of(
                "balance", 1000000,
                "accountNumber", "1234-5678-9012",
                "level", "GOLD"
            )
        );
    }

    /**
     * USER 또는 ADMIN 역할 중 하나만 있으면 접근 가능
     * 
     * @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
     * - 나열된 역할 중 하나라도 있으면 허용
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Map<String, Object> getCommonData() {
        return Map.of(
            "message", "공통 데이터",
            "annotation", "@PreAuthorize(\"hasAnyRole('USER', 'ADMIN')\")",
            "allowedRoles", List.of("ROLE_USER", "ROLE_ADMIN"),
            "data", "Anyone with USER or ADMIN role can access"
        );
    }

    /**
     * 메소드 실행 후 반환 값을 기반으로 권한 체크
     * 
     * @PostAuthorize("returnObject['public'] == true or hasRole('ADMIN')")
     * - returnObject: 메소드의 반환 값 (Map)
     * - returnObject['public']: Map의 'public' 키 값 참조
     * - public == true이거나 ADMIN 역할이 있으면 허용
     * - 조건 불만족 시 메소드는 실행되지만 반환 값 전달 안 됨 (403 Forbidden)
     * 
     * 사용 케이스:
     * - DB 조회 후 결과 데이터의 속성에 따라 접근 제어
     * - 예: 게시글의 'isPublic' 필드에 따라 접근 제어
     */
    @PostAuthorize("returnObject['public'] == true or hasRole('ADMIN')")
    public Map<String, Object> getDataWithPostCheck(boolean isPublic) {
        // 메소드는 항상 실행됨
        Map<String, Object> data = Map.of(
            "message", "데이터 조회 성공",
            "public", isPublic,
            "data", "sensitive information",
            "annotation", "@PostAuthorize(\"returnObject['public'] == true or hasRole('ADMIN')\")",
            "note", "public이 true이거나 ADMIN 역할이 있어야 반환 값을 받을 수 있음"
        );
        
        // PostAuthorize가 반환 값을 체크하여 접근 제어
        return data;
    }

    /**
     * 복잡한 SpEL 표현식 예제
     * 
     * 조건:
     * 1. ADMIN 역할을 가지거나
     * 2. (USER 역할을 가지고 AND 파라미터 username이 현재 사용자와 같음)
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #username == authentication.name)")
    public Map<String, Object> getComplexData(String username) {
        return Map.of(
            "message", "복잡한 권한 체크",
            "username", username,
            "annotation", "@PreAuthorize(\"hasRole('ADMIN') or (hasRole('USER') and #username == authentication.name)\")",
            "allowedCases", List.of(
                "ADMIN 역할을 가진 사용자 (username 상관없이 접근 가능)",
                "USER 역할을 가지고 username이 자기 자신인 경우"
            )
        );
    }

    /**
     * 권한 체크 없는 메소드 (비교용)
     */
    public Map<String, Object> getPublicData() {
        return Map.of(
            "message", "권한 체크 없는 공개 데이터",
            "annotation", "없음 (메소드 레벨 보안 미적용)",
            "note", "Controller에서 URL 레벨 보안만 적용됨"
        );
    }
}

