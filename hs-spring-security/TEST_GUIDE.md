# Spring Security Test Guide

> 이 문서는 WEEK 1-5 예제 코드의 테스트 방법을 상세히 설명합니다.

## 목차

- [WEEK 3: CORS/CSRF/Authorization](#week-3-corscsfauthorization)
  - [V11: CORS + CSRF](#v11-cors--csrf)
  - [V12: Authorization](#v12-authorization)
- [WEEK 4: JWT/Method Security](#week-4-jwtmethod-security)
  - [V13: JWT](#v13-jwt)
  - [V14: Method Security](#v14-method-security)
- [WEEK 5: OAuth2](#week-5-oauth2)
  - [V15: OAuth2 Client](#v15-oauth2-client)
  - [V16: Resource Server](#v16-resource-server)

---

## WEEK 3: CORS/CSRF/Authorization

### V11: CORS + CSRF

**학습 목표**: CORS Pre-flight 처리 및 CSRF 토큰 검증

#### 테스트 1: CORS Pre-flight 요청

```bash
# OPTIONS 요청으로 Pre-flight 테스트
curl -X OPTIONS http://localhost:8080/v11/corscrf/transfer \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v
```

**예상 결과**:
```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:4200
Access-Control-Allow-Methods: *
Access-Control-Allow-Headers: *
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

#### 테스트 2: CSRF 토큰 조회

```bash
# 로그인하여 CSRF 토큰 조회
curl http://localhost:8080/v11/corscrf/csrf-token \
  -u user@example.com:user123 \
  -v
```

**예상 결과**:
```json
{
  "version": "V11",
  "token": "abc123-def456-ghi789",
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf",
  "cookieName": "XSRF-TOKEN"
}
```

**응답 헤더 확인**:
```
Set-Cookie: XSRF-TOKEN=abc123-def456-ghi789; Path=/
X-XSRF-TOKEN: abc123-def456-ghi789
```

#### 테스트 3: CSRF 토큰 없이 POST (실패)

```bash
curl -X POST http://localhost:8080/v11/corscrf/transfer \
  -H "Content-Type: application/json" \
  -d '{"amount": 1000, "toAccount": "123456"}' \
  -u user@example.com:user123
```

**예상 결과**:
```
HTTP/1.1 403 Forbidden
{"error": "Invalid CSRF Token"}
```

#### 테스트 4: CSRF 토큰과 함께 POST (성공)

```bash
# 1단계: CSRF 토큰 조회하여 변수에 저장
CSRF_TOKEN=$(curl -s http://localhost:8080/v11/corscrf/csrf-token \
  -u user@example.com:user123 | jq -r '.token')

# 2단계: 토큰과 함께 POST 요청
curl -X POST http://localhost:8080/v11/corscrf/transfer \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  -H "Cookie: XSRF-TOKEN=$CSRF_TOKEN" \
  -d '{"amount": 1000, "toAccount": "123456"}' \
  -u user@example.com:user123
```

**예상 결과**:
```json
{
  "version": "V11",
  "message": "계좌이체 성공",
  "toAccount": "123456",
  "amount": 1000,
  "csrfProtection": "활성화됨"
}
```

#### 테스트 5: 공개 엔드포인트 (CSRF 제외)

```bash
curl http://localhost:8080/v11/corscrf/public
```

**예상 결과**:
```json
{
  "version": "V11",
  "message": "CORS 테스트용 공개 엔드포인트",
  "csrf": "CSRF 보호 제외"
}
```

---

### V12: Authorization

**학습 목표**: 역할 기반 접근 제어

#### 테스트 계정
- **USER**: `user@example.com` / `user123` (ROLE_USER)
- **ADMIN**: `admin@example.com` / `admin123` (ROLE_ADMIN)

#### 테스트 1: USER 계정으로 USER API 접근 (성공)

```bash
curl http://localhost:8080/v12/authorization/user \
  -u user@example.com:user123
```

**예상 결과**:
```json
{
  "version": "V12",
  "access": "user",
  "username": "user@example.com",
  "requiredRole": "ROLE_USER",
  "authorities": [{"authority": "ROLE_USER"}]
}
```

#### 테스트 2: USER 계정으로 ADMIN API 접근 (실패)

```bash
curl http://localhost:8080/v12/authorization/admin \
  -u user@example.com:user123
```

**예상 결과**:
```
HTTP/1.1 403 Forbidden
```

#### 테스트 3: ADMIN 계정으로 ADMIN API 접근 (성공)

```bash
curl http://localhost:8080/v12/authorization/admin \
  -u admin@example.com:admin123
```

**예상 결과**:
```json
{
  "version": "V12",
  "access": "admin",
  "username": "admin@example.com",
  "requiredRole": "ROLE_ADMIN",
  "authorities": [{"authority": "ROLE_ADMIN"}]
}
```

#### 테스트 4: ADMIN 계정으로 USER API 접근 (실패)

```bash
curl http://localhost:8080/v12/authorization/user \
  -u admin@example.com:admin123
```

**예상 결과**:
```
HTTP/1.1 403 Forbidden
```

**주의**: ADMIN과 USER는 별개의 역할이므로 ADMIN이 USER API에 접근 불가

#### 테스트 5: hasAnyRole 테스트 (USER와 ADMIN 모두 접근 가능)

```bash
# USER 계정
curl http://localhost:8080/v12/authorization/any \
  -u user@example.com:user123

# ADMIN 계정
curl http://localhost:8080/v12/authorization/any \
  -u admin@example.com:admin123
```

**예상 결과**: 둘 다 200 OK

#### 테스트 6: 인증 없이 접근 (실패)

```bash
curl http://localhost:8080/v12/authorization/user
```

**예상 결과**:
```
HTTP/1.1 401 Unauthorized
```

---

## WEEK 4: JWT/Method Security

### V13: JWT

**학습 목표**: Stateless JWT 인증

#### 테스트 1: JWT 발급

```bash
# USER 계정으로 JWT 발급
curl http://localhost:8080/v13/jwt/login \
  -u user@example.com:user123 \
  -v
```

**예상 결과**:
```
HTTP/1.1 200 OK
Authorization: eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJocy1zcHJpbmctc2VjdXJpdHkiLCJzdWIiOiJKV1QgVG9rZW4iLCJ1c2VybmFtZSI6InVzZXJAZXhhbXBsZS5jb20iLCJhdXRob3JpdGllcyI6IlJPTEVfVVNFUiIsImlhdCI6MTYwOTQ1OTIwMCwiZXhwIjoxNjA5NDYyODAwfQ...

{
  "message": "JWT 발급 완료",
  "username": "user@example.com"
}
```

**JWT 저장**:
```bash
# 변수에 JWT 저장 (헤더에서 추출)
JWT=$(curl -s http://localhost:8080/v13/jwt/login \
  -u user@example.com:user123 \
  -D - | grep -i "^Authorization:" | cut -d' ' -f2)

echo $JWT
```

#### 테스트 2: JWT로 보호된 리소스 접근 (성공)

```bash
curl http://localhost:8080/v13/jwt/secured \
  -H "Authorization: $JWT"
```

**예상 결과**:
```json
{
  "version": "V13",
  "message": "JWT로 보호된 리소스",
  "username": "user@example.com",
  "authenticated": true,
  "stateless": true
}
```

#### 테스트 3: JWT 없이 접근 (실패)

```bash
curl http://localhost:8080/v13/jwt/secured
```

**예상 결과**:
```
HTTP/1.1 403 Forbidden
```

#### 테스트 4: 잘못된 JWT로 접근 (실패)

```bash
curl http://localhost:8080/v13/jwt/secured \
  -H "Authorization: invalid-jwt-token"
```

**예상 결과**:
```
HTTP/1.1 403 Forbidden
{"error": "Invalid Token received!"}
```

#### 테스트 5: JWT vs 세션 비교 정보

```bash
curl http://localhost:8080/v13/jwt/jwt-vs-session \
  -H "Authorization: $JWT"
```

#### 테스트 6: ADMIN JWT 발급 및 사용

```bash
# ADMIN JWT 발급
ADMIN_JWT=$(curl -s http://localhost:8080/v13/jwt/login \
  -u admin@example.com:admin123 \
  -D - | grep -i "^Authorization:" | cut -d' ' -f2)

# ADMIN JWT로 접근
curl http://localhost:8080/v13/jwt/secured \
  -H "Authorization: $ADMIN_JWT"
```

---

### V14: Method Security

**학습 목표**: 메소드 레벨 보안 (@PreAuthorize, @PostAuthorize)

#### 테스트 1: ADMIN 전용 메소드 (USER는 실패)

```bash
# USER 계정 (실패)
curl http://localhost:8080/v14/method/admin-only \
  -u user@example.com:user123

# ADMIN 계정 (성공)
curl http://localhost:8080/v14/method/admin-only \
  -u admin@example.com:admin123
```

**USER 예상 결과**: 403 Forbidden (메소드 레벨에서 거부)  
**ADMIN 예상 결과**: 200 OK

#### 테스트 2: 파라미터 기반 권한 체크

```bash
# 자기 자신의 username (성공)
curl http://localhost:8080/v14/method/owner-only/user@example.com \
  -u user@example.com:user123

# 다른 사람의 username (실패)
curl http://localhost:8080/v14/method/owner-only/other@example.com \
  -u user@example.com:user123
```

**예상 결과**:
- 첫 번째: 200 OK (파라미터가 현재 사용자와 일치)
- 두 번째: 403 Forbidden (파라미터가 현재 사용자와 불일치)

#### 테스트 3: @PostAuthorize 테스트

```bash
# public=true (성공)
curl http://localhost:8080/v14/method/post-check/true \
  -u user@example.com:user123

# public=false, USER 계정 (실패)
curl http://localhost:8080/v14/method/post-check/false \
  -u user@example.com:user123

# public=false, ADMIN 계정 (성공)
curl http://localhost:8080/v14/method/post-check/false \
  -u admin@example.com:admin123
```

**예상 결과**:
- public=true: 200 OK (반환 값의 public이 true)
- public=false + USER: 403 Forbidden
- public=false + ADMIN: 200 OK (ADMIN 역할 보유)

#### 테스트 4: 복잡한 SpEL 표현식

```bash
# ADMIN은 username 상관없이 접근 가능
curl http://localhost:8080/v14/method/complex/anyone \
  -u admin@example.com:admin123

# USER는 자기 자신의 username만 가능
curl http://localhost:8080/v14/method/complex/user@example.com \
  -u user@example.com:user123

curl http://localhost:8080/v14/method/complex/other@example.com \
  -u user@example.com:user123
```

#### 테스트 5: URL 레벨 vs 메소드 레벨 보안 비교

```bash
curl http://localhost:8080/v14/method/security-levels \
  -u user@example.com:user123
```

---

## WEEK 5: OAuth2

### V15: OAuth2 Client

**학습 목표**: OAuth2 Client 모드 (Mock 환경)

#### 테스트 1: OAuth2 사용자 정보 조회

```bash
# Mock OAuth2 사용자 정보
curl http://localhost:8080/v15/oauth2/user \
  -u mockuser@example.com:any
```

**예상 결과**:
```json
{
  "version": "V15",
  "message": "OAuth2 인증된 사용자 정보",
  "provider": "mock-provider",
  "email": "mockuser@example.com",
  "name": "Mock User",
  "sub": "mock-user-123",
  "attributes": {
    "sub": "mock-user-123",
    "name": "Mock User",
    "email": "mockuser@example.com",
    "provider": "mock-provider"
  }
}
```

#### 테스트 2: OAuth2 로그인 플로우 설명

```bash
curl http://localhost:8080/v15/oauth2/login-simulation
```

**예상 결과**: Authorization Code Grant Flow의 각 단계 설명

#### 테스트 3: OAuth2 vs 일반 로그인 비교

```bash
curl http://localhost:8080/v15/oauth2/oauth2-vs-form
```

#### 참고: 실제 환경에서의 OAuth2 로그인

실제 GitHub/Google OAuth2 연동 시:

1. **브라우저로 접근**: `http://localhost:8080/oauth2/authorization/github`
2. **GitHub로 리디렉션**: 로그인 페이지
3. **로그인 및 권한 동의**
4. **콜백**: `/login/oauth2/code/github?code=abc123`
5. **자동 처리**: Spring Security가 Code → Token 교환
6. **사용자 정보 조회**: `/user` 엔드포인트 호출
7. **세션 생성**: JSESSIONID 쿠키 발급
8. **리디렉션**: 애플리케이션 홈

---

### V16: Resource Server

**학습 목표**: OAuth2 Resource Server 모드 (JWT 검증)

#### 사전 준비: V13에서 JWT 발급

```bash
# USER JWT 발급
USER_JWT=$(curl -s http://localhost:8080/v13/jwt/login \
  -u user@example.com:user123 \
  -D - | grep -i "^Authorization:" | cut -d' ' -f2)

# ADMIN JWT 발급
ADMIN_JWT=$(curl -s http://localhost:8080/v13/jwt/login \
  -u admin@example.com:admin123 \
  -D - | grep -i "^Authorization:" | cut -d' ' -f2)

echo "USER JWT: $USER_JWT"
echo "ADMIN JWT: $ADMIN_JWT"
```

#### 테스트 1: JWT로 계정 정보 API 접근 (성공)

```bash
curl http://localhost:8080/v16/resource/api/account \
  -H "Authorization: $USER_JWT"
```

**예상 결과**:
```json
{
  "version": "V16",
  "message": "Resource Server API - 계정 정보",
  "username": "user@example.com",
  "authorities": "ROLE_USER",
  "issuer": "hs-spring-security",
  "subject": "JWT Token",
  "issuedAt": "2024-01-27T10:00:00Z",
  "expiresAt": "2024-01-27T11:00:00Z"
}
```

#### 테스트 2: JWT 없이 접근 (실패)

```bash
curl http://localhost:8080/v16/resource/api/account
```

**예상 결과**:
```
HTTP/1.1 401 Unauthorized
```

#### 테스트 3: 잔액 조회 (USER 또는 ADMIN 필요)

```bash
curl http://localhost:8080/v16/resource/api/balance \
  -H "Authorization: $USER_JWT"
```

**예상 결과**:
```json
{
  "version": "V16",
  "message": "잔액 조회",
  "username": "user@example.com",
  "balance": 1000000,
  "currency": "KRW",
  "requiredRole": "ROLE_USER"
}
```

#### 테스트 4: USER JWT로 ADMIN API 접근 (실패)

```bash
curl http://localhost:8080/v16/resource/api/admin \
  -H "Authorization: $USER_JWT"
```

**예상 결과**:
```
HTTP/1.1 403 Forbidden
```

#### 테스트 5: ADMIN JWT로 ADMIN API 접근 (성공)

```bash
curl http://localhost:8080/v16/resource/api/admin \
  -H "Authorization: $ADMIN_JWT"
```

**예상 결과**:
```json
{
  "version": "V16",
  "message": "관리자 전용 API",
  "username": "admin@example.com",
  "requiredRole": "ROLE_ADMIN",
  "data": {
    "totalUsers": 100,
    "activeUsers": 85,
    "revenue": 10000000
  }
}
```

#### 테스트 6: 공개 엔드포인트 (JWT 불필요)

```bash
curl http://localhost:8080/v16/resource/public
```

**예상 결과**:
```json
{
  "version": "V16",
  "message": "Resource Server 공개 API",
  "authRequired": false
}
```

#### 테스트 7: Resource Server 개념 설명

```bash
curl http://localhost:8080/v16/resource/concept
```

#### 테스트 8: JWT 에러 시나리오

```bash
curl http://localhost:8080/v16/resource/jwt-errors
```

---

## 통합 테스트 시나리오

### 시나리오 1: JWT 발급 → Resource Server 호출

```bash
# 1. V13에서 JWT 발급
JWT=$(curl -s http://localhost:8080/v13/jwt/login \
  -u user@example.com:user123 \
  -D - | grep -i "^Authorization:" | cut -d' ' -f2)

# 2. V16 Resource Server에서 사용
curl http://localhost:8080/v16/resource/api/account \
  -H "Authorization: $JWT"

# 3. 1시간 후 만료 확인 (sleep 3600 후)
# curl http://localhost:8080/v16/resource/api/account \
#   -H "Authorization: $JWT"
# → 401 Unauthorized (JWT 만료)
```

### 시나리오 2: 역할별 접근 제어 테스트

```bash
# USER 계정
USER_JWT=$(curl -s http://localhost:8080/v13/jwt/login \
  -u user@example.com:user123 \
  -D - | grep -i "^Authorization:" | cut -d' ' -f2)

# ADMIN 계정
ADMIN_JWT=$(curl -s http://localhost:8080/v13/jwt/login \
  -u admin@example.com:admin123 \
  -D - | grep -i "^Authorization:" | cut -d' ' -f2)

# 테스트 매트릭스
echo "=== USER JWT 테스트 ==="
curl -w "\n%{http_code}\n" http://localhost:8080/v12/authorization/user -u user@example.com:user123
curl -w "\n%{http_code}\n" http://localhost:8080/v12/authorization/admin -u user@example.com:user123
curl -w "\n%{http_code}\n" http://localhost:8080/v16/resource/api/balance -H "Authorization: $USER_JWT"
curl -w "\n%{http_code}\n" http://localhost:8080/v16/resource/api/admin -H "Authorization: $USER_JWT"

echo "=== ADMIN JWT 테스트 ==="
curl -w "\n%{http_code}\n" http://localhost:8080/v12/authorization/admin -u admin@example.com:admin123
curl -w "\n%{http_code}\n" http://localhost:8080/v16/resource/api/admin -H "Authorization: $ADMIN_JWT"
```

---

## 필터 체인 확인

애플리케이션 시작 시 로그에서 필터 체인 확인:

```
2024-01-27 10:00:00.123  INFO ... : Will secure Ant [pattern='/v11/corscrf/**'] with [
  org.springframework.security.web.session.DisableEncodeUrlFilter@...,
  org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter@...,
  org.springframework.security.web.context.SecurityContextHolderFilter@...,
  org.springframework.security.web.header.HeaderWriterFilter@...,
  org.springframework.web.filter.CorsFilter@...,
  org.springframework.security.web.csrf.CsrfFilter@...,
  org.springframework.security.web.authentication.logout.LogoutFilter@...,
  ...
  org.springframework.security.web.authentication.www.BasicAuthenticationFilter@...,
  org.example.hsspringsecurity.week3.v11corscrf.CsrfCookieFilter@...,
  ...
]
```

---

## 문제 해결 (Troubleshooting)

### 문제 1: CSRF 토큰 검증 실패

**증상**: 403 Forbidden - Invalid CSRF Token

**해결**:
1. 쿠키와 헤더 모두에 토큰 포함 확인
2. 토큰 값이 정확히 일치하는지 확인
3. 세션이 유지되고 있는지 확인 (JSESSIONID 쿠키)

### 문제 2: JWT 검증 실패

**증상**: 401 Unauthorized - Invalid Token

**해결**:
1. JWT가 만료되지 않았는지 확인 (1시간 유효)
2. V13에서 발급한 JWT를 정확히 복사했는지 확인
3. Authorization 헤더 형식 확인: `Authorization: eyJhbGc...`

### 문제 3: 권한 부족

**증상**: 403 Forbidden

**해결**:
1. 사용자의 역할 확인 (ROLE_USER vs ROLE_ADMIN)
2. API가 요구하는 역할 확인
3. JWT의 authorities claim 확인

### 문제 4: 필터가 실행되지 않음

**증상**: 예상과 다른 동작

**해결**:
1. SecurityFilterChain의 @Order 확인
2. securityMatcher() 경로 패턴 확인
3. 로그에서 실제 필터 체인 확인

---

## 추가 학습 자료

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [JWT.io](https://jwt.io/) - JWT 디버거
- [OAuth2 RFC](https://oauth.net/2/) - OAuth2 표준 문서
- [CORS MDN](https://developer.mozilla.org/ko/docs/Web/HTTP/CORS) - CORS 상세 설명
