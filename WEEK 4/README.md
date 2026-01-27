# WEEK 4: 커스텀 필터, JWT 토큰 인증, 메소드 레벨 보안

> **호환 버전**: Spring Boot 3.2.x, Spring Security 6.2.x ~ 7.0.x, JJWT 0.12.x

### 학습 목표
- `OncePerRequestFilter`를 상속받아 커스텀 보안 필터를 만들고 필터 체인에 등록한다.
- JWT(JSON Web Token)를 이용한 무상태(Stateless) 인증 아키텍처를 이해하고 구현한다.
- `@PreAuthorize`, `@PostAuthorize` 등을 사용하여 메소드 레벨에서 세밀한 보안을 적용한다.

---

Week 3까지 인증, 인가, 웹 보안의 기본(CORS, CSRF)을 다뤘다. Week 4에서는 Spring Security를 더욱 깊이 있게 활용하는 방법을 학습한다. 보안 필터 체인에 직접 개입하는 커스텀 필터, 현대적인 API 환경의 표준인 JWT 기반의 무상태 인증, 그리고 코드 레벨에서 보안을 강화하는 메소드 레벨 보안을 다룬다.

---

### 1. 커스텀 필터(Custom Filter) 구현

Spring Security는 여러 기본 필터들로 구성된 체인(Chain)을 통해 동작한다. 때로는 이 체인에 우리만의 로직을 추가해야 할 필요가 있다.

#### 1.1. 커스텀 필터가 필요한 이유
- **로깅 및 감사**: 모든 요청에 대해 사용자 정보, IP 주소 등을 로그로 남기고자 할 때
- **입력값 검증**: 인증 처리 전 특정 HTTP 헤더나 요청 파라미터의 유효성을 검증하고 싶을 때
- **암/복호화**: 요청 본문을 복호화하거나 응답 본문을 암호화해야 할 때

#### 1.2. Spring Security 필터 체인 구조

```mermaid
graph LR
    subgraph SecurityFilterChain
        direction LR
        CSRF[CsrfFilter]
        CSRF --> Logout[LogoutFilter]
        Logout --> Custom1[Custom Filter 1<br/>addFilterBefore]
        Custom1 --> BasicAuth[BasicAuthenticationFilter]
        BasicAuth --> Custom2[Custom Filter 2<br/>addFilterAfter]
        Custom2 --> ExHandler[ExceptionTranslationFilter]
        ExHandler --> FilterSecurity[AuthorizationFilter]
    end
```

#### 1.3. 필터 구현 및 체인에 추가하기
- **`OncePerRequestFilter` 상속**: 커스텀 필터는 한 요청에 대해 단 한 번만 실행되는 것을 보장하는 `OncePerRequestFilter`를 상속하여 만드는 것이 일반적이다. 실제 로직은 `doFilterInternal()` 메소드에 구현한다.
- **필터 체인에 추가**: `SecurityFilterChain` 설정에서 `addFilterBefore()`, `addFilterAfter()`를 사용하여 원하는 위치에 필터를 추가할 수 있다.

**코드 예제: 인증 성공 후 권한을 로깅하는 필터**

```java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

// 1. OncePerRequestFilter를 상속하여 필터 클래스 생성
public class AuthoritiesLoggingAfterFilter extends OncePerRequestFilter {

    private final Logger LOG = Logger.getLogger(AuthoritiesLoggingAfterFilter.class.getName());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // SecurityContextHolder에서 인증 정보를 가져온다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (null != authentication) {
            LOG.info("User " + authentication.getName() + " is successfully authenticated and has the authorities "
                    + authentication.getAuthorities().toString());
        }

        // 다음 필터로 요청을 전달한다.
        filterChain.doFilter(request, response);
    }
}
```

**설정 예제: `SecurityFilterChain`에 커스텀 필터 추가**

```java
@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(requests -> requests
                .requestMatchers("/myAccount/**").authenticated()
                .anyRequest().permitAll()
            )
            // BasicAuthenticationFilter가 실행된 후, 위에서 만든 로깅 필터를 실행한다.
            .addFilterAfter(new AuthoritiesLoggingAfterFilter(), BasicAuthenticationFilter.class)
            .formLogin(withDefaults())
            .httpBasic(withDefaults());
        return http.build();
    }
}
```

**커스텀 필터 실행 순서 예제:**

```java
// 요청 전에 실행되는 필터
public class RequestValidationBeforeFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                     FilterChain filterChain) throws ServletException, IOException {
        String email = request.getHeader("X-User-Email");
        if (email != null && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid email format");
            return; // 필터 체인 중단
        }
        filterChain.doFilter(request, response);
    }
}

// SecurityConfig에 추가
http.addFilterBefore(new RequestValidationBeforeFilter(), BasicAuthenticationFilter.class);
```

---

### 1.4. 커스텀 필터 추가 위치와 실행 순서 상세 가이드

Spring Security 필터 체인에 커스텀 필터를 추가할 때, 정확한 위치를 선택하는 것이 매우 중요하다.

#### 1.4.1. addFilterBefore/After/At 비교 표

| 메소드 | 설명 | 사용 예시 |
|--------|------|----------|
| `addFilterBefore(A, B.class)` | A 필터를 B 필터 **앞**에 추가 | 요청 검증을 인증 전에 수행 |
| `addFilterAfter(A, B.class)` | A 필터를 B 필터 **뒤**에 추가 | 인증 후 로깅 수행 |
| `addFilterAt(A, B.class)` | A 필터를 B 필터와 **같은 순서**에 추가 | 기본 필터 대체 (주의 필요) |

**주의사항:**
- `addFilterAt()`은 기존 필터를 제거하지 **않고** 같은 위치에 추가한다. 두 필터가 모두 실행된다.
- 기본 필터를 완전히 대체하려면 해당 설정을 `disable()`하고 커스텀 필터를 추가해야 한다.

#### 1.4.2. JWT 필터 추가 시 체인 변화 시각화

```mermaid
graph LR
    subgraph BeforeJWT[JWT 추가 전 - 세션 기반]
        Csrf1[CsrfFilter] --> Logout1[LogoutFilter]
        Logout1 --> FormLogin1[UsernamePasswordAuthenticationFilter]
        FormLogin1 --> BasicAuth1[BasicAuthenticationFilter]
        BasicAuth1 --> Session1[SessionManagementFilter]
        Session1 --> Exception1[ExceptionTranslationFilter]
        Exception1 --> Authz1[AuthorizationFilter]
    end
    
    subgraph AfterJWT[JWT 추가 후 - Stateless]
        Csrf2[CsrfFilter<br/>DISABLED] -.-> Logout2[LogoutFilter]
        Logout2 --> JWTValidator[JwtTokenValidatorFilter<br/>addFilterBefore]
        JWTValidator --> FormLogin2[UsernamePasswordAuthenticationFilter]
        FormLogin2 --> BasicAuth2[BasicAuthenticationFilter]
        BasicAuth2 --> JWTGenerator[JwtTokenGeneratorFilter<br/>addFilterAfter]
        JWTGenerator --> Session2[SessionManagementFilter<br/>STATELESS]
        Session2 --> Exception2[ExceptionTranslationFilter]
        Exception2 --> Authz2[AuthorizationFilter]
    end
    
    style JWTValidator fill:#ffcccc
    style JWTGenerator fill:#ffcccc
    style Csrf2 fill:#cccccc
    style Session2 fill:#cccccc
```

**변경 사항:**
- ✅ **JwtTokenValidatorFilter** 추가: `UsernamePasswordAuthenticationFilter` 이전
- ✅ **JwtTokenGeneratorFilter** 추가: `BasicAuthenticationFilter` 이후
- ❌ **CsrfFilter** 비활성화: JWT는 CSRF에 취약하지 않음
- ⚙️ **SessionManagementFilter** 변경: `STATELESS` 모드로 설정

#### 1.4.3. Stateless vs Stateful 필터 비교

```mermaid
graph TD
    subgraph Stateful[SessionCreationPolicy.IF_REQUIRED - 세션 기반]
        direction TB
        Login1[UsernamePasswordAuthenticationFilter] --> CreateAuth1[Authentication 객체 생성]
        CreateAuth1 --> Session1[SessionManagementFilter]
        Session1 --> CreateSession[세션 생성]
        CreateSession --> SecurityContext1[SecurityContextHolderFilter]
        SecurityContext1 --> SaveToSession[SecurityContext를 세션에 저장]
        SaveToSession --> JSESSIONID[JSESSIONID 쿠키 발급]
        JSESSIONID --> SubReq1[후속 요청]
        SubReq1 --> LoadSession[세션에서 Authentication 로드]
        LoadSession --> NoReAuth[재인증 불필요]
    end
    
    subgraph Stateless[SessionCreationPolicy.STATELESS - JWT 기반]
        direction TB
        JWTVal[JwtTokenValidatorFilter] --> ParseJWT[JWT 파싱 및 검증]
        ParseJWT --> ExtractClaims[Claims 추출 username, roles]
        ExtractClaims --> CreateAuth2[Authentication 객체 생성]
        CreateAuth2 --> SetContext[SecurityContextHolder에 저장]
        SetContext --> NoSession[세션 생성 안 함]
        NoSession --> NoJSESSIONID[JSESSIONID 없음]
        NoJSESSIONID --> SubReq2[후속 요청]
        SubReq2 --> JWTVal2[매 요청마다 JWT 재검증]
        JWTVal2 --> FullValidation[서명 검증 + 만료 확인]
    end
    
    style CreateSession fill:#ffe6e6
    style SaveToSession fill:#ffe6e6
    style NoSession fill:#e6ffe6
    style SetContext fill:#e6ffe6
```

**비교표:**

| 특성 | Stateful (세션 기반) | Stateless (JWT 기반) |
|------|---------------------|---------------------|
| **상태 저장** | 서버 메모리/DB에 세션 저장 | 서버에 상태 저장 안 함 |
| **인증 정보** | JSESSIONID 쿠키 | JWT 토큰 (헤더 또는 쿠키) |
| **후속 요청** | 세션에서 로드 (빠름) | 매번 토큰 검증 (상대적으로 느림) |
| **확장성** | 세션 공유 필요 (Redis 등) | 수평 확장 용이 |
| **로그아웃** | 세션 무효화 | 토큰 블랙리스트 또는 만료 대기 |
| **CSRF 방어** | 필요 | 불필요 (헤더 방식 시) |

#### 1.4.4. SecurityContext 저장 전략 변화

**Spring Security 6.0+ 변경사항:**

```java
// ❌ Deprecated (Spring Security 5.x)
http.securityContext(context -> 
    context.securityContextRepository(
        new HttpSessionSecurityContextRepository()
    )
);

// ✅ 현재 방식 (Spring Security 6.0+)
http.securityContext(context -> 
    context.requireExplicitSave(true) // 기본값: true
);

// JWT Stateless 설정
http
    .csrf(csrf -> csrf.disable())
    .sessionManagement(session -> 
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    )
    .addFilterBefore(jwtValidatorFilter, UsernamePasswordAuthenticationFilter.class)
    .addFilterAfter(jwtGeneratorFilter, BasicAuthenticationFilter.class);
```

**requireExplicitSave(true) 의미:**
- `false`: 필터 체인 종료 시 자동으로 SecurityContext를 세션에 저장
- `true`: 명시적으로 `SecurityContextRepository.saveContext()` 호출 필요
- **JWT 환경에서는**: 세션을 사용하지 않으므로 영향 없음

#### 1.4.5. 실전 예제: 커스텀 필터 위치 선택 가이드

**시나리오 1: API 키 검증 필터**
```java
// 인증 전에 API 키 유효성을 먼저 확인
http.addFilterBefore(apiKeyValidationFilter, UsernamePasswordAuthenticationFilter.class);
```
**이유**: 유효하지 않은 API 키는 인증 프로세스를 시작할 필요가 없음

**시나리오 2: 인증 성공 로깅 필터**
```java
// 인증 완료 후 감사 로그 기록
http.addFilterAfter(auditLoggingFilter, UsernamePasswordAuthenticationFilter.class);
```
**이유**: 인증이 성공한 후에만 로그를 남기고 싶음

**시나리오 3: JWT 검증 필터**
```java
// 인증 필터 이전에 JWT 검증
http.addFilterBefore(jwtValidatorFilter, UsernamePasswordAuthenticationFilter.class);
```
**이유**: JWT가 유효하면 `Authentication` 객체를 설정하고, 이후 인증 필터들을 건너뛸 수 있음

**시나리오 4: JWT 생성 필터**
```java
// 인증 완료 후 JWT 생성 및 응답 헤더에 추가
http.addFilterAfter(jwtGeneratorFilter, BasicAuthenticationFilter.class);
```
**이유**: 인증이 성공한 후에만 JWT를 발급해야 함

---

### 2. JWT(JSON Web Token)를 이용한 토큰 기반 인증

#### 2.1. 무상태(Stateless) 인증의 필요성
전통적인 세션 기반 인증(`JSESSIONID`)은 서버가 각 사용자의 세션 정보를 메모리에 유지해야 하므로 '상태를 유지(Stateful)'한다. 이는 서버 확장이 어렵고 마이크로서비스 환경에 적합하지 않다. 반면, 토큰 기반 인증은 서버가 상태를 저장하지 않는 '무상태(Stateless)' 아키텍처를 가능하게 한다.

**세션 vs JWT 비교:**

| 특징 | 세션 기반 (Stateful) | JWT 기반 (Stateless) |
|------|---------------------|---------------------|
| 상태 저장 | 서버 메모리/DB에 세션 저장 | 서버에 상태 저장 안 함 |
| 확장성 | 여러 서버 간 세션 공유 필요 | 어떤 서버든 토큰 검증 가능 |
| CSRF 방어 | 필수 | 선택적 (덜 취약) |
| 로그아웃 | 세션 삭제로 즉시 가능 | 토큰 만료까지 유효 (블랙리스트 필요) |
| 모바일 앱 | 쿠키 관리 어려움 | 헤더에 토큰 전송으로 쉬움 |

#### 2.2. JWT(JSON Web Token)란?
JWT는 필요한 모든 정보(사용자 정보, 권한 등)를 자체적으로 가지고 있는(Self-contained) 토큰이다. `.`으로 구분된 세 부분으로 구성된다.

**JWT 구조:**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
└────────── Header ──────────┘ └───────────────── Payload ─────────────────┘ └─────────── Signature ───────────┘
```

1.  **Header**: 토큰의 타입, 사용하는 해시 알고리즘 등의 메타데이터를 담는다.
    ```json
    {
      "alg": "HS256",
      "typ": "JWT"
    }
    ```

2.  **Payload**: 실제 전달할 데이터(Claims)를 담는다. 사용자의 아이디, 권한, 토큰 만료 시간 등을 포함할 수 있다.
    ```json
    {
      "username": "john@example.com",
      "authorities": "ROLE_USER,ROLE_ADMIN",
      "iat": 1609459200,
      "exp": 1609545600
    }
    ```
    > **주의**: Payload는 Base64로 인코딩될 뿐, 암호화된 것이 아니므로 민감 정보를 담아서는 안 된다.

3.  **Signature**: 토큰의 무결성을 보장하는 서명. Header와 Payload, 그리고 **서버만 아는 비밀 키(Secret Key)**를 조합하여 해싱 알고리즘(e.g., HMAC-SHA256)으로 생성한다. 이 서명 덕분에 서버는 토큰이 위변조되지 않았음을 확신할 수 있다.

#### 2.3. JWT 인증 전체 플로우

```mermaid
sequenceDiagram
    participant Client
    participant JWTGenFilter as JWT Generator Filter
    participant JWTValFilter as JWT Validator Filter
    participant API
    participant DB

    rect rgb(200, 220, 250)
        Note over Client,DB: 1. Initial Login & JWT Generation
        Client->>JWTGenFilter: POST /login (username, password)
        JWTGenFilter->>DB: Validate Credentials
        DB-->>JWTGenFilter: User Valid
        JWTGenFilter->>JWTGenFilter: Generate JWT with Secret Key
        JWTGenFilter-->>Client: Response Header: Authorization: Bearer JWT_TOKEN
    end

    rect rgb(220, 250, 220)
        Note over Client,API: 2. Subsequent Requests with JWT
        Client->>JWTValFilter: GET /api/myAccount<br/>Header: Authorization: Bearer JWT_TOKEN
        JWTValFilter->>JWTValFilter: Verify JWT Signature
        JWTValFilter->>JWTValFilter: Extract user info & authorities
        JWTValFilter->>JWTValFilter: Set SecurityContext
        JWTValFilter->>API: Forward Request
        API-->>Client: Protected Resource
    end
```

#### 2.4. Spring Security JWT 구현

**1단계: 의존성 추가 (build.gradle)**
```gradle
dependencies {
    // JWT Dependencies
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'
}
```

**2단계: SecurityConstants 클래스 생성**
```java
public class SecurityConstants {
    public static final String JWT_KEY = "jxgEQeXHuPq8VdbyYFNkANdudQ53YUn4"; // 최소 32자
    public static final String JWT_HEADER = "Authorization";
}
```

**3단계: JWT 생성 필터 (JWTTokenGeneratorFilter)**
```java
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

public class JWTTokenGeneratorFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                     FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (null != authentication) {
            SecretKey key = Keys.hmacShaKeyFor(SecurityConstants.JWT_KEY.getBytes(StandardCharsets.UTF_8));
            
            String jwt = Jwts.builder()
                    .issuer("Spring Security App")
                    .subject("JWT Token")
                    .claim("username", authentication.getName())
                    .claim("authorities", authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(",")))
                    .issuedAt(new Date())
                    .expiration(new Date(new Date().getTime() + 30000000)) // 약 8시간
                    .signWith(key)
                    .compact();
            
            response.setHeader(SecurityConstants.JWT_HEADER, jwt);
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // /user 경로(로그인)에서만 JWT 생성
        return !request.getServletPath().equals("/user");
    }
}
```

**4단계: JWT 검증 필터 (JWTTokenValidatorFilter)**
```java
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JWTTokenValidatorFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = request.getHeader(SecurityConstants.JWT_HEADER);
        if (null != jwt) {
            try {
                // 1. 서버만 아는 비밀 키로 SecretKey 객체를 생성한다.
                SecretKey key = Keys.hmacShaKeyFor(
                        SecurityConstants.JWT_KEY.getBytes(StandardCharsets.UTF_8));

                // 2. JWT 파싱 및 서명 검증. 검증 실패 시 예외 발생.
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(jwt)
                        .getPayload();
                
                // 3. 토큰에서 사용자 이름과 권한을 추출한다.
                String username = String.valueOf(claims.get("username"));
                String authorities = (String) claims.get("authorities");
                
                // 4. 검증 성공 시, Authentication 객체를 생성하여 SecurityContext에 저장한다.
                Authentication auth = new UsernamePasswordAuthenticationToken(username, null,
                        AuthorityUtils.commaSeparatedStringToAuthorityList(authorities));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                throw new BadCredentialsException("Invalid Token received!");
            }
        }
        filterChain.doFilter(request, response);
    }

    // 로그인 요청(/user)에서는 이 필터를 실행하지 않는다.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().equals("/user");
    }
}
```

**5단계: SecurityConfig 설정**
```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // JWT 사용 시 세션을 사용하지 않으므로 STATELESS로 설정
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // JWT 방식에서는 CSRF 공격에 비교적 안전하므로 비활성화
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(requests -> requests
                .requestMatchers("/myAccount", "/myBalance", "/myLoans", "/myCards").authenticated()
                .requestMatchers("/notices", "/contact", "/register").permitAll()
            )
            // JWT 검증 필터를 BasicAuthenticationFilter 이전에 추가
            .addFilterBefore(new JWTTokenValidatorFilter(), BasicAuthenticationFilter.class)
            // JWT 생성 필터를 BasicAuthenticationFilter 이후에 추가
            .addFilterAfter(new JWTTokenGeneratorFilter(), BasicAuthenticationFilter.class)
            .httpBasic(withDefaults());
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

> **전문가 Tip**: JWT를 구현하면 `JSESSIONID`와 CSRF 토큰이 더 이상 필요 없게 된다. CSRF 공격은 세션 쿠키가 자동으로 전송되는 것을 이용하는데, JWT는 클라이언트가 수동으로 헤더에 담아 보내므로 CSRF 공격에 비교적 안전하기 때문이다.

#### 2.5. 클라이언트에서 JWT 사용하기

**로그인 요청 (JavaScript):**
```javascript
// 1. 로그인하여 JWT 받기
fetch('http://localhost:8080/user', {
    method: 'GET',
    headers: {
        'Authorization': 'Basic ' + btoa('user@example.com:password123')
    }
})
.then(response => {
    const jwt = response.headers.get('Authorization');
    localStorage.setItem('jwt', jwt); // JWT를 로컬 스토리지에 저장
    return response.json();
})
.then(data => console.log('Login successful:', data));
```

**보호된 리소스 요청:**
```javascript
// 2. 저장된 JWT로 API 호출
const jwt = localStorage.getItem('jwt');

fetch('http://localhost:8080/myAccount', {
    method: 'GET',
    headers: {
        'Authorization': jwt  // JWT를 헤더에 포함
    }
})
.then(response => response.json())
.then(data => console.log('Account data:', data));
```

#### 2.6. JWT Bean과 필터 매핑: Stateless 인증의 내부 동작

JWT 기반 인증에서 사용되는 커스텀 필터들과 Spring Security OAuth2 Resource Server 방식의 Bean 매핑을 비교한다.

##### 2.6.1. JWT 인증 방식별 Bean-Filter 매핑

| 구현 방식 | Bean/컴포넌트 | 생성되는 필터 | 필터 위치 | 역할 |
|---------|------------|------------|----------|------|
| **커스텀 필터 방식** | `JwtTokenGeneratorFilter`<br/>`JwtTokenValidatorFilter` | 개발자가 직접 생성한 필터 | `addFilterBefore/After`로 지정 | JWT 생성 및 검증 로직 직접 구현 |
| **OAuth2 Resource Server** | `JwtDecoder` Bean<br/>`JwtAuthenticationConverter` Bean | `BearerTokenAuthenticationFilter` 자동 생성 | 인증 필터 사이 (자동 배치) | Spring Security가 제공하는 표준 JWT 검증 |

##### 2.6.2. 커스텀 JWT 필터 방식의 Bean-Filter 구조

```mermaid
graph TB
    subgraph CustomBeans[개발자가 생성하는 컴포넌트]
        JTG[JwtTokenGeneratorFilter<br/>OncePerRequestFilter 상속]
        JTV[JwtTokenValidatorFilter<br/>OncePerRequestFilter 상속]
        SC[SecurityConstants<br/>JWT_KEY, JWT_HEADER]
    end
    
    subgraph Config[SecurityFilterChain 설정]
        AddAfter[addFilterAfter<br/>JwtTokenGeneratorFilter]
        AddBefore[addFilterBefore<br/>JwtTokenValidatorFilter]
    end
    
    subgraph Filters[필터 체인에 삽입]
        BAF[BasicAuthenticationFilter<br/>순서 12]
        JTV_Filter[JwtTokenValidatorFilter<br/>순서 8-9 사이]
        JTG_Filter[JwtTokenGeneratorFilter<br/>순서 12-13 사이]
        UPF[UsernamePasswordAuthenticationFilter<br/>순서 9]
    end
    
    JTG --> AddAfter
    JTV --> AddBefore
    SC --> JTG
    SC --> JTV
    
    AddAfter --> JTG_Filter
    AddBefore --> JTV_Filter
    
    JTV_Filter --> UPF
    UPF --> BAF
    BAF --> JTG_Filter
    
    style JTG fill:#ffe6cc
    style JTV fill:#ffe6cc
    style JTG_Filter fill:#ffcccc
    style JTV_Filter fill:#ffcccc
```

**코드 예제:**

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        // JWT 검증 필터: UsernamePasswordAuthenticationFilter 이전에 배치
        .addFilterBefore(new JwtTokenValidatorFilter(), UsernamePasswordAuthenticationFilter.class)
        // JWT 생성 필터: BasicAuthenticationFilter 이후에 배치
        .addFilterAfter(new JwtTokenGeneratorFilter(), BasicAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/myAccount").authenticated()
            .anyRequest().permitAll()
        )
        .httpBasic(withDefaults());
    return http.build();
}
```

**필터 체인 순서:**
```
SecurityContextHolderFilter (3)
  ↓
HeaderWriterFilter (4)
  ↓
LogoutFilter (7)
  ↓
JwtTokenValidatorFilter ← 여기서 JWT 검증 (순서 8-9 사이)
  ↓
UsernamePasswordAuthenticationFilter (9)
  ↓
BasicAuthenticationFilter (12)
  ↓
JwtTokenGeneratorFilter ← 여기서 JWT 생성 (순서 12-13 사이)
  ↓
RequestCacheAwareFilter (14)
  ...
```

##### 2.6.3. OAuth2 Resource Server 방식의 Bean-Filter 구조

```mermaid
graph TB
    subgraph Beans[Spring Security가 제공하는 Bean]
        JD[JwtDecoder<br/>@Bean 등록]
        JAC[JwtAuthenticationConverter<br/>@Bean 등록 선택]
    end
    
    subgraph Config[SecurityFilterChain 설정]
        O2RS[oauth2ResourceServer<br/>oauth2 -> oauth2.jwt...]
    end
    
    subgraph AutoGenerated[자동 생성]
        BTAF[BearerTokenAuthenticationFilter<br/>자동 생성]
        JAM[JwtAuthenticationManager<br/>내부 사용]
    end
    
    subgraph Runtime[런타임 동작]
        Extract[JWT 추출<br/>Authorization: Bearer]
        Decode[JWT 디코딩 및 검증]
        Convert[Claims → Authorities 변환]
        SetAuth[SecurityContext에 저장]
    end
    
    JD --> BTAF
    JAC --> BTAF
    O2RS --> BTAF
    
    BTAF --> Extract
    Extract --> Decode
    Decode --> Convert
    Convert --> SetAuth
    
    style BTAF fill:#e6ffe6
    style JD fill:#ffe6e6
```

**코드 예제:**

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/myAccount").authenticated()
            .anyRequest().permitAll()
        )
        // OAuth2 Resource Server 설정 → BearerTokenAuthenticationFilter 자동 생성
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
    return http.build();
}

// JwtDecoder Bean 등록 (필수)
@Bean
public JwtDecoder jwtDecoder() {
    String secretKey = "jxgEQeXHuPq8VdbyYFNkANdudQ53YUn4";
    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    return NimbusJwtDecoder.withSecretKey(key).build();
    // → BearerTokenAuthenticationFilter가 이 Bean을 사용하여 JWT 검증
}

// JwtAuthenticationConverter Bean 등록 (선택)
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = 
        new JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
    grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
    
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return converter;
    // → BearerTokenAuthenticationFilter가 이 Bean을 사용하여 권한 추출
}
```

**필터 체인 순서:**
```
SecurityContextHolderFilter (3)
  ↓
HeaderWriterFilter (4)
  ↓
LogoutFilter (7)
  ↓
BearerTokenAuthenticationFilter ← JWT 검증 (자동 배치)
  ↓
RequestCacheAwareFilter (14)
  ↓
AnonymousAuthenticationFilter (17)
  ↓
ExceptionTranslationFilter (20)
  ↓
AuthorizationFilter (21)
```

##### 2.6.4. 두 방식의 상세 비교

| 특성 | 커스텀 필터 방식 | OAuth2 Resource Server 방식 |
|------|----------------|---------------------------|
| **Bean 등록** | `JwtTokenGeneratorFilter`, `JwtTokenValidatorFilter` 직접 생성 | `JwtDecoder` Bean 등록 (필수) |
| **필터 생성** | 개발자가 `addFilterBefore/After`로 직접 추가 | `BearerTokenAuthenticationFilter` 자동 생성 |
| **JWT 검증 로직** | 개발자가 JJWT 라이브러리 직접 사용 | Spring Security가 제공하는 표준 로직 |
| **권한 추출** | 개발자가 Claims에서 직접 파싱 | `JwtAuthenticationConverter`로 커스터마이징 |
| **예외 처리** | `try-catch`로 직접 처리 | Spring Security 표준 예외 처리 |
| **유연성** | 높음 (모든 로직 제어 가능) | 중간 (Converter로 커스터마이징) |
| **코드 복잡도** | 높음 (모든 것 직접 구현) | 낮음 (Bean 등록만) |
| **권장 사용처** | 학습 목적, 특수한 JWT 로직 | 실무 프로젝트 (표준 방식) |

##### 2.6.5. BearerTokenAuthenticationFilter 내부 동작

```mermaid
sequenceDiagram
    participant Request
    participant BTAF as BearerTokenAuthenticationFilter
    participant Resolver as BearerTokenResolver
    participant AM as AuthenticationManager
    participant Provider as JwtAuthenticationProvider
    participant JD as JwtDecoder<br/>@Bean
    participant JAC as JwtAuthenticationConverter<br/>@Bean
    participant SCH as SecurityContextHolder
    
    Request->>BTAF: HTTP Request<br/>Authorization: Bearer eyJhbGc...
    BTAF->>Resolver: resolve(request)
    Resolver-->>BTAF: JWT Token String
    
    BTAF->>BTAF: Create BearerTokenAuthenticationToken<br/>principal = token, authenticated = false
    
    BTAF->>AM: authenticate(token)
    AM->>Provider: authenticate(token)
    
    Provider->>JD: decode(token)
    
    alt JWT 유효
        JD-->>Provider: Jwt (검증 완료)
        Provider->>JAC: convert(jwt)
        JAC->>JAC: Extract authorities from Claims<br/>e.g. jwt.getClaim("roles")
        JAC-->>Provider: AbstractAuthenticationToken<br/>with authorities
        Provider-->>AM: JwtAuthenticationToken<br/>authenticated = true
        AM-->>BTAF: Authentication
        BTAF->>SCH: setAuthentication(auth)
        BTAF-->>Request: Continue filter chain
    else JWT 무효 (만료, 서명 불일치 등)
        JD-->>Provider: JwtException
        Provider-->>AM: AuthenticationException
        AM-->>BTAF: AuthenticationException
        BTAF-->>Request: 401 Unauthorized<br/>ExceptionTranslationFilter가 처리
    end
```

##### 2.6.6. JwtDecoder Bean의 영향 범위

`JwtDecoder` Bean 등록 여부에 따라:

| 상황 | JwtDecoder Bean | oauth2ResourceServer() 설정 | 결과 |
|------|----------------|---------------------------|------|
| ❌ Bean 미등록 | 없음 | 있음 | 애플리케이션 시작 실패<br/>`BeanCreationException` |
| ✅ Bean 등록 | 있음 | 있음 | `BearerTokenAuthenticationFilter` 정상 동작 |
| ✅ Bean 등록 | 있음 | 없음 | Bean은 생성되지만 필터는 생성 안 됨 (사용되지 않음) |

**JwtDecoder Bean 등록 예제:**

```java
// 방법 1: Secret Key 기반 (HMAC-SHA256)
@Bean
public JwtDecoder jwtDecoder() {
    String secretKey = "jxgEQeXHuPq8VdbyYFNkANdudQ53YUn4";
    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    return NimbusJwtDecoder.withSecretKey(key).build();
}

// 방법 2: JWK Set URI 기반 (Keycloak, Auth0 등)
@Bean
public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withJwkSetUri(
        "http://localhost:8180/realms/eazybank/protocol/openid-connect/certs"
    ).build();
}

// 방법 3: Issuer URI 기반 (자동 JWK 조회)
@Bean
public JwtDecoder jwtDecoder() {
    return JwtDecoders.fromIssuerLocation(
        "http://localhost:8180/realms/eazybank"
    );
}
```

##### 2.6.7. JwtAuthenticationConverter Bean의 역할

기본적으로 Spring Security는 JWT의 `scope` 또는 `scp` claim에서 권한을 추출한다. 커스텀 claim(예: `roles`)을 사용하려면 `JwtAuthenticationConverter` Bean이 필요하다:

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = 
        new JwtGrantedAuthoritiesConverter();
    
    // 커스텀 claim 이름 지정
    grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
    
    // 권한 접두사 지정 (ROLE_ 추가)
    grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
    
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    
    return converter;
}
```

**JWT Payload 예시:**
```json
{
  "sub": "user@example.com",
  "roles": ["USER", "ADMIN"],
  "exp": 1609545600
}
```

**Converter 적용 전:**
- 권한: 없음 (scope claim이 없으므로)

**Converter 적용 후:**
- 권한: `ROLE_USER`, `ROLE_ADMIN`

##### 2.6.8. 실전 팁: Bean 등록 순서와 디버깅

**추천 Bean 등록 순서:**

```java
@Configuration
public class JwtSecurityConfig {
    
    // 1. JwtDecoder Bean (필수)
    @Bean
    public JwtDecoder jwtDecoder() {
        // ...
    }
    
    // 2. JwtAuthenticationConverter Bean (선택)
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // ...
    }
    
    // 3. SecurityFilterChain 설정
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );
        return http.build();
    }
}
```

**디버깅 방법:**

```java
// BearerTokenAuthenticationFilter가 생성되었는지 확인
@Component
public class FilterDebugger implements CommandLineRunner {
    
    @Autowired
    private FilterChainProxy filterChainProxy;
    
    @Override
    public void run(String... args) {
        filterChainProxy.getFilterChains().forEach(chain -> {
            ((SecurityFilterChain) chain).getFilters().forEach(filter -> {
                if (filter instanceof BearerTokenAuthenticationFilter) {
                    System.out.println("✅ BearerTokenAuthenticationFilter 활성화됨!");
                }
            });
        });
    }
}
```

> **전문가 Tip**: 실무에서는 **OAuth2 Resource Server 방식**을 강력히 권장합니다. Spring Security가 제공하는 표준 방식이므로 보안 패치가 자동으로 적용되고, 코드 유지보수가 쉽습니다. 커스텀 필터 방식은 학습 목적으로는 유용하지만, 운영 환경에서는 보안 취약점을 직접 관리해야 하는 부담이 있습니다. `JwtDecoder` Bean 하나만 등록하면 나머지는 Spring Security가 알아서 처리합니다!

---

### 3. 메소드 레벨 보안

URL 기반의 접근 제어(`authorizeHttpRequests`) 외에, 서비스나 리포지토리의 특정 자바 메소드에 직접 보안 규칙을 적용할 수 있다. 

#### 3.1. 활성화
`@Configuration` 클래스에 `@EnableMethodSecurity` 어노테이션을 추가하여 활성화한다.

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity  // 메소드 레벨 보안 활성화
public class SecurityConfig {
    // ... SecurityFilterChain 설정 ...
}
```

#### 3.2. 주요 어노테이션
- **`@PreAuthorize`**: 메소드가 실행되기 **전**에 권한을 검사한다. 조건이 거짓이면 메소드는 실행되지 않는다.
- **`@PostAuthorize`**: 메소드가 실행된 **후**, 반환된 객체(`returnObject`)를 가지고 권한을 검사한다.
- **`@PreFilter`**: 메소드에 전달되는 컬렉션 타입의 파라미터를 필터링한다.
- **`@PostFilter`**: 메소드가 반환하는 컬렉션 타입의 결과를 필터링한다.

이 어노테이션들은 SpEL(Spring Expression Language)을 사용하여 동적이고 강력한 규칙을 만들 수 있다.

#### 3.3. 코드 예제

**리포지토리 메소드에 `@PreAuthorize` 적용:**
```java
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoanRepository extends CrudRepository<Loans, Long> {

    // 이 메소드는 'USER' 역할을 가진 사용자만 호출할 수 있다.
    @PreAuthorize("hasRole('USER')")
    List<Loans> findByCustomerIdOrderByStartDtDesc(int customerId);
}
```

**서비스 레이어에 적용:**
```java
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

@Service
public class LoanService {

    // 파라미터로 넘어온 username이 현재 인증된 사용자의 이름과 같을 때만 메소드를 실행한다.
    @PreAuthorize("#username == authentication.principal.username")
    public List<Loan> getLoanDetails(String username) {
        return loanRepository.findByUsername(username);
    }

    // 메소드가 반환하는 Loan 객체의 소유주가 현재 인증된 사용자와 같을 때만 결과를 반환한다.
    @PostAuthorize("returnObject.customer.email == authentication.name")
    public Loan getSingleLoanDetail(int loanId) {
        return loanRepository.findById(loanId).orElse(null);
    }
    
    // ADMIN 역할만 모든 대출 내역을 조회할 수 있다
    @PreAuthorize("hasRole('ADMIN')")
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }
}
```

**Controller에 적용:**
```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    // 메소드 레벨에서도 보안 적용 가능
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted");
    }
}
```

#### 3.4. SpEL 표현식 예제

```java
// 1. 역할 체크
@PreAuthorize("hasRole('ADMIN')")

// 2. 권한 체크
@PreAuthorize("hasAuthority('DELETE_USER')")

// 3. 여러 역할 중 하나
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")

// 4. 파라미터와 인증된 사용자 비교
@PreAuthorize("#email == authentication.name")

// 5. 복잡한 조건
@PreAuthorize("hasRole('ADMIN') and #user.age >= 18")

// 6. 반환 객체 검증
@PostAuthorize("returnObject.owner == authentication.name")

// 7. 컬렉션 필터링
@PostFilter("filterObject.public == true or filterObject.owner == authentication.name")
```

> **전문가 Tip**: URL 기반 보안이 1차 방어선이라면, 메소드 레벨 보안은 2차 방어선 역할을 합니다. 이를 '심층 방어(Defense in Depth)' 전략이라고 합니다. 만약 URL 설정이 잘못되더라도, 비즈니스 로직의 핵심인 서비스 메소드에서 직접 접근을 제어하므로 애플리케이션의 보안을 한층 더 견고하게 만들 수 있습니다.

---

## FAQ

**Q: JWT의 비밀 키(Secret Key)는 어떻게 관리해야 하나요?**  
A: 
- 최소 32자 이상의 강력한 키 사용
- 환경 변수 또는 외부 설정 파일에 저장 (절대 코드에 하드코딩 금지)
- 운영 환경에서는 AWS Secrets Manager, HashiCorp Vault 같은 비밀 관리 서비스 사용

**Q: JWT 토큰이 탈취되면 어떻게 하나요?**  
A:
- JWT는 만료 시간이 있으므로 짧게 설정 (15분~1시간)
- Refresh Token 패턴 사용하여 Access Token을 주기적으로 갱신
- 민감한 작업은 재인증 요구
- 로그아웃 시 블랙리스트(Redis 등)에 토큰 추가

**Q: JWT를 로컬 스토리지에 저장하는 것이 안전한가요?**  
A: XSS 공격에 취약합니다. 대안:
- HttpOnly 쿠키에 저장 (XSS로부터 보호, 하지만 CSRF 방어 필요)
- 짧은 만료 시간 + Refresh Token 패턴
- 중요한 작업은 추가 인증 요구

**Q: `@PreAuthorize`가 작동하지 않습니다.**  
A: 
1. `@EnableMethodSecurity`가 추가되었는지 확인
2. 어노테이션을 적용한 클래스가 Spring Bean으로 등록되었는지 확인
3. 같은 클래스 내부에서 메소드를 직접 호출하면 작동 안 함 (AOP 프록시 우회)

**Q: Bearer 스킴을 사용하려면?**  
A: 
```java
// 생성 시
response.setHeader("Authorization", "Bearer " + jwt);

// 검증 시
String authHeader = request.getHeader("Authorization");
if (authHeader != null && authHeader.startsWith("Bearer ")) {
    String jwt = authHeader.substring(7);
    // 검증 로직...
}
```

**Q: 메소드 레벨 보안과 URL 레벨 보안 중 어느 것을 사용해야 하나요?**  
A: **둘 다 사용**하세요! URL 레벨은 넓은 범위의 1차 방어, 메소드 레벨은 세밀한 2차 방어 역할을 합니다.

**Q: JWT 만료 시간을 어떻게 설정하나요?**  
A:
```java
.expiration(new Date(System.currentTimeMillis() + 3600000)) // 1시간
// 또는
.expiration(Date.from(Instant.now().plusSeconds(3600)))
```

---

**다음 주차 예고**: WEEK 5에서는 OAuth2와 OpenID Connect를 학습하고, Keycloak을 이용한 독립 인증 서버를 구축하는 방법을 다룹니다!
