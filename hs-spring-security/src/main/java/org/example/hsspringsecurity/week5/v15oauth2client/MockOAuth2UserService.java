package org.example.hsspringsecurity.week5.v15oauth2client;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mock OAuth2 User Service
 * 
 * 역할:
 * - OAuth2LoginAuthenticationFilter가 Access Token을 받은 후 사용자 정보 조회
 * - 실제 환경에서는 GitHub/Google의 /user 엔드포인트 호출
 * - Mock 환경에서는 외부 호출 없이 시뮬레이션 데이터 반환
 * 
 * DefaultOAuth2UserService 상속:
 * - Spring Security가 제공하는 기본 OAuth2 사용자 정보 조회 서비스
 * - loadUser() 메소드를 오버라이드하여 커스터마이징
 * 
 * OAuth2 User 정보 조회 플로우:
 * 1. OAuth2LoginAuthenticationFilter가 Authorization Code 획득
 * 2. Code → Access Token 교환
 * 3. OAuth2UserService.loadUser() 호출
 * 4. Access Token으로 사용자 정보 조회 (/userinfo 엔드포인트)
 * 5. OAuth2User 객체 생성
 * 6. SecurityContext에 OAuth2AuthenticationToken 저장
 * 
 * 실무 환경 vs Mock 환경:
 * 
 * [실무 - GitHub OAuth2]
 * 1. Access Token 획득: ghp_abc123...
 * 2. GitHub API 호출: GET https://api.github.com/user
 *    Authorization: Bearer ghp_abc123...
 * 3. 응답: {"login": "john", "email": "john@example.com", ...}
 * 4. OAuth2User 생성
 * 
 * [Mock - 학습 환경]
 * 1. Access Token 획득: mock-token (실제로는 사용 안 함)
 * 2. 외부 API 호출 생략
 * 3. 하드코딩된 Mock 데이터 반환
 * 4. OAuth2User 생성
 * 
 * 학습 포인트:
 * - OAuth2 Client 모드에서 UserService의 역할
 * - OAuth2User와 UserDetails의 차이
 * - 소셜 로그인 시 자동 회원가입 구현 위치
 * - attributes에서 필요한 정보 추출하여 DB 저장
 */
@Component
public class MockOAuth2UserService extends DefaultOAuth2UserService {

    /**
     * OAuth2 사용자 정보 로드 (Mock)
     * 
     * @param userRequest Access Token과 Client 정보를 포함한 요청 객체
     * @return OAuth2User 사용자 정보 객체
     * @throws OAuth2AuthenticationException 사용자 정보 조회 실패 시
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 실제 환경에서는 super.loadUser(userRequest)를 호출하여
        // Access Token으로 Provider의 /userinfo 엔드포인트 호출
        
        // Mock 환경: 외부 호출 없이 시뮬레이션 데이터 반환
        
        // Provider 정보 (실제로는 userRequest에서 가져옴)
        String registrationId = "mock-provider";  // GitHub, Google, Kakao 등
        
        // Mock 사용자 속성 (실제로는 Provider API 응답)
        Map<String, Object> attributes = Map.of(
            "sub", "mock-user-123",  // Subject (고유 식별자)
            "name", "Mock User",  // 사용자 이름
            "email", "mockuser@example.com",  // 이메일
            "picture", "https://via.placeholder.com/150",  // 프로필 사진
            "provider", registrationId,  // OAuth2 Provider
            "locale", "ko_KR",  // 언어/지역
            "email_verified", true  // 이메일 인증 여부
        );
        
        // 실무에서는 여기서 DB 확인 및 자동 회원가입 처리
        // 예: if (!userRepository.existsByEmail(email)) { ... }
        
        // OAuth2User 생성
        // - authorities: 부여할 권한 목록
        // - attributes: Provider에서 받은 사용자 속성
        // - nameAttributeKey: attributes에서 사용자 이름으로 사용할 키
        return new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"  // Principal의 이름으로 사용할 attribute 키
        );
    }
}

