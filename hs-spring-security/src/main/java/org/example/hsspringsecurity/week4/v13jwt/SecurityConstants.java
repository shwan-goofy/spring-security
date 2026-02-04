package org.example.hsspringsecurity.week4.v13jwt;

/**
 * JWT 보안 상수 클래스
 * 
 * JWT 생성 및 검증에 필요한 상수들을 정의한다.
 * 
 * 학습 목적:
 * - 실무에서는 application.properties나 환경 변수에서 관리
 * - 하지만 학습 목적으로 간단하게 클래스로 관리
 * 
 * 보안 주의사항:
 * - JWT_KEY는 절대 코드에 하드코딩하지 말 것 (실무 환경)
 * - 최소 32자 이상의 강력한 키 사용
 * - 운영 환경에서는 AWS Secrets Manager, HashiCorp Vault 등 사용
 * - Git에 커밋하지 말 것 (.gitignore에 추가)
 * 
 * JJWT 0.12.x 요구사항:
 * - HS256 알고리즘: 최소 256비트(32바이트) 이상 키 필요
 * - HS384 알고리즘: 최소 384비트(48바이트) 이상 키 필요
 * - HS512 알고리즘: 최소 512비트(64바이트) 이상 키 필요
 */
public class SecurityConstants {
    
    /**
     * JWT 서명에 사용할 비밀 키
     * 
     * 최소 32자 이상 필요 (HS256 알고리즘)
     * 학습용으로 40자 사용
     * 
     * 실무 권장 방법:
     * 1. application.properties: jwt.secret.key=...
     * 2. 환경 변수: export JWT_SECRET_KEY=...
     * 3. 외부 비밀 관리 서비스
     */
    public static final String JWT_KEY = "jxgEQeXHuPq8VdbyYFNkANdudQ53YUn4Learning";
    
    /**
     * JWT를 전달할 HTTP 헤더 이름
     * 
     * 표준: "Authorization"
     * 값 형식: "Bearer eyJhbGciOiJ..." 또는 직접 토큰 문자열
     * 
     * 참고: V13에서는 Bearer 접두사 없이 직접 토큰만 전달
     *      V16(Resource Server)에서는 Bearer 접두사 사용
     */
    public static final String JWT_HEADER = "Authorization";
}

