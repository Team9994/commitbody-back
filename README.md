# CommitBody 백엔드

소셜 피트니스 커뮤니티 CommitBody의 백엔드 서버입니다. 사용자는 운동 인증(이미지 중심)과 정보/질문 게시글을 작성하고, 팔로우·좋아요·댓글·알림 등의 소셜 기능을 이용할 수 있습니다. 서버는 JWT 기반 인증과 Redis, Elasticsearch, AWS S3, Firebase 등을 활용하여 확장성과 실시간성을 고려해 설계되었습니다.

## 한눈에 보기

| 영역 | 핵심 기능 | 관련 패키지(예시) |
|---|---|---|
| 회원/인증 | 소셜·일반 로그인, JWT 발급/검증/갱신, 로그아웃, 차단, 권한 | domain.Member, global.security, global.authorization |
| 게시글 | 운동 인증 이미지, 정보/질문, 공개 범위, 멀티파트 업로드 | domain.article |
| 소셜 | 댓글 CRUD, 좋아요 토글, 팔로우/언팔로우 | domain.comment, domain.like, domain.follow |
| 운동/루틴/기록 | 운동 데이터(ES 연동), 루틴, 개인 기록 | domain.exercise, domain.routine, domain.record |
| 알림 | Firebase 푸시 알림 전송 | global.notification |
| 검색/인덱싱 | Elasticsearch 기반 색인/검색 대비 | domain.exercise, domain.article, global.redis |
| 운영/문서화 | Swagger UI, Actuator, 스케줄링 | global.config, CommitBodyApplication |

## 주요 기능
- 회원/인증
  - 소셜·일반 로그인을 포괄하는 회원 도메인 구성 (예: LoginType, Member)
  - JWT 발급·검증·갱신, 로그아웃 처리 및 차단(블락) 사용자 제어
  - 권한(ROLE_USER) 기반 API 접근 제어
- 게시글(Article)
  - 운동 인증(이미지) 및 정보/질문 타입 지원 (ArticleType, ArticleCategory)
  - 멀티파트 업로드(이미지) → S3 저장, 공개 범위(visibility) 관리
  - 프로필별 작성 글 페이지네이션(무한 스크롤 형태) 조회
- 댓글/좋아요/팔로우
  - 게시글/운동 댓글 CRUD, 좋아요 토글, 팔로우/언팔로우
- 운동/루틴/기록
  - 운동 데이터(Elasticsearch 연동), 루틴 관리, 개인 기록 관리
  - 배치/스케줄링 기반 주기 작업 처리 (@EnableScheduling)
- 알림
  - Firebase 기반 푸시 알림 전송
- 검색/인덱싱
  - Elasticsearch를 활용한 검색/추천 시나리오 대비 (docker-compose 제공)
- 운영/문서화
  - Springdoc OpenAPI(Swagger UI), Actuator 헬스/메트릭

## 기술 스택 (표)

| 분류 | 기술 | 버전 | 비고 |
|---|---|---|---|
| Language | Java | 17 | Gradle Toolchain |
| Framework | Spring Boot | 3.2.8 | Web, Security, Validation, Data JPA, AOP, Batch, Actuator, Thymeleaf |
| ORM | JPA + QueryDSL | 5.0.0 | querydsl-jpa:5.0.0:jakarta |
| Database | MySQL | 8.x | 운영 DB (mysql-connector-j) |
| In-memory DB | H2 | 2.x | 테스트/로컬 |
| Cache | Redis | - | spring-boot-starter-data-redis |
| Search | Elasticsearch | 8.7.1 | docker-compose 이미지 기준 |
| Auth | JWT | java-jwt 4.4.0 / nimbus-jose-jwt 9.15 | Access/Refresh 토큰 |
| Storage | AWS S3 | - | spring-cloud-starter-aws 2.2.6.RELEASE |
| Docs | springdoc-openapi | 2.2.0 | Swagger UI 제공 |
| Push | Firebase Admin SDK | 9.2.0 | 푸시 알림 |
| Build | Gradle | - | 래퍼 포함 |

## 보안/인증 흐름
- 무상태(Stateless) 인증: 세션 미사용, JWT로 인증
- JwtAuthenticationFilter가 토큰을 검증하고 SecurityContext에 주체 저장
- ExceptionFilter로 시큐리티 단계 예외 일괄 처리
- 화이트리스트: /api/v1/auth, /v3/api-docs/**, /api/v1/swagger-ui/**, /api/v1/scheduled/** 등
- 그 외 /api/v1/** 경로는 ROLE_USER 권한 필요

### 화이트리스트 및 권한 표

아래 표는 SecurityConfig의 설정(permitAll vs 인증 필요)을 요약합니다.

| 패턴 | 접근 | 설명 |
|---|---|---|
| /api/v1/auth | Public | 로그인/회원가입 엔드포인트 집합 |
| /api/v1/auth-refresh | Public | 토큰 갱신 |
| /api/v1/logout | Public | 로그아웃 |
| /api/v1/scheduled/** | Public | 스케줄링 관련 공개 엔드포인트 |
| /api/v1/swagger-ui/**, /api/v1/swagger-ui.html | Public | Swagger UI |
| /v3/api-docs/**, /api-docs/** | Public | OpenAPI 문서 |
| /actuator/** | Public | 헬스/메트릭 |
| /api/v1/admin/* | Public | 관리용 공개 엔드포인트(구성에 따라 변경 가능) |
| /test | Public | 테스트용 |
| /api/v1/** (그 외) | ROLE_USER | 그 외 API는 인증 필요 |

## 엔드포인트 안내(발췌)
아래는 대표 엔드포인트 일부입니다. 전체 스키마는 Swagger UI에서 확인하세요.

| Method | Path | Auth | 설명 |
|---|---|---|---|
| POST | /api/v1/auth | Public | 로그인/회원가입 |
| POST | /api/v1/auth-refresh | Public | 토큰 갱신 |
| POST | /api/v1/logout | Public | 로그아웃 처리 |
| POST | /api/v1/article | ROLE_USER | 게시글 등록 (multipart: 본문 JSON + 파일) |
| GET | /api/v1/article/{articleId} | ROLE_USER | 게시글 상세 조회 |
| GET | /api/v1/my-page/articles/{memberId}?type=...&lastId&size=12 | ROLE_USER | 프로필별 게시글 목록 |
| PUT/PATCH | /api/v1/article/{articleId} | ROLE_USER | 게시글 수정 |
| DELETE | /api/v1/article/{articleId} | ROLE_USER | 게시글 삭제 |
| GET | /actuator/health | Public | 헬스 체크 |
| GET | /api/v1/swagger-ui/index.html | Public | Swagger UI |

정확한 파라미터·응답 스키마는 Swagger UI에서 확인하세요.

- Swagger UI: http://localhost:8080/api/v1/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 디렉터리 구조(요약)
- src/main/java/team9499/commitbody
  - domain/* — 도메인별 패키지(Article, Exercise, Member, Follow, Like, Comment, Routine, Record 등)
    - controller, service, repository, dto, domain(엔티티)
  - global/* — 공통(보안/설정/유틸/페이로드/레디스/알림 등)
    - config(Security/Cors), security(필터/EntryPoint/AccessDenied), authorization, notification, redis, payload, utils
- src/main/resources — application-*.yml, static, templates, firebase/*
- 루트 — build.gradle, docker-compose.yml(Elasticsearch 로컬용)

## 빠른 시작(로컬)
사전 요구사항: Java 17, Gradle, (선택) Docker

1) 설정 준비

src/main/resources/application.yml 또는 프로필별 application-*.yml에 다음 예시를 참고해 값을 채웁니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/commitbody
    username: root
    password: your-password
  jpa:
    hibernate:
      ddl-auto: update
  data:
    redis:
      host: localhost
      port: 6379
    elasticsearch:
      uris: http://localhost:9200

jwt:
  secret: your-jwt-secret
  access-token-exp: 3600
  refresh-token-exp: 1209600

cloud:
  aws:
    s3:
      bucket: your-bucket
    credentials:
      access-key: your-access-key
      secret-key: your-secret-key
    region:
      static: ap-northeast-2

firebase:
  config-path: classpath:firebase/service-account.json
```

2) (선택) Elasticsearch 로컬 기동

```bash
docker compose up -d
```

3) 애플리케이션 실행

macOS/Linux
```bash
./gradlew bootRun
```

Windows
```powershell
gradlew.bat bootRun
```

4) API 문서 접속
- Swagger UI: http://localhost:8080/api/v1/swagger-ui/index.html
- OpenAPI: http://localhost:8080/v3/api-docs

### 환경 변수/설정 키 표

| Property | 설명 | 예시 |
|---|---|---|
| spring.datasource.url | MySQL 연결 URL | jdbc:mysql://localhost:3306/commitbody |
| spring.datasource.username | DB 사용자 | root |
| spring.datasource.password | DB 비밀번호 | your-password |
| spring.data.redis.host | Redis 호스트 | localhost |
| spring.data.redis.port | Redis 포트 | 6379 |
| spring.data.elasticsearch.uris | ES 접속 URI | http://localhost:9200 |
| jwt.secret | JWT 서명 키 | your-jwt-secret |
| jwt.access-token-exp | Access 만료(초) | 3600 |
| jwt.refresh-token-exp | Refresh 만료(초) | 1209600 |
| cloud.aws.s3.bucket | S3 버킷명 | your-bucket |
| cloud.aws.credentials.access-key | AWS 액세스 키 | your-access-key |
| cloud.aws.credentials.secret-key | AWS 시크릿 키 | your-secret-key |
| cloud.aws.region.static | 리전 | ap-northeast-2 |
| firebase.config-path | Firebase 서비스 계정 경로 | classpath:firebase/service-account.json |

### 빌드/실행 요약 표

| 작업 | macOS / Linux | Windows |
|---|---|---|
| 의존성 다운로드/빌드 | ./gradlew build | gradlew.bat build |
| 테스트 실행 | ./gradlew test | gradlew.bat test |
| 애플리케이션 실행 | ./gradlew bootRun | gradlew.bat bootRun |
| ES 기동(선택) | docker compose up -d | docker compose up -d |

## 개발/테스트
- 테스트: JUnit + Spring Security Test, H2 포함
- Actuator: /actuator/** 헬스/메트릭 확인
- Swagger 예시 응답으로 클라이언트와의 스펙 합의 용이

### 기본값/정책 표

| 항목 | 값 | 출처/비고 |
|---|---|---|
| 프로필 게시글 페이지 사이즈 | 12 | @PageableDefault(size = 12) |
| 인증 방식 | Stateless JWT | SecurityConfig |
| CORS Origin | * | 모든 오리진 허용(AllowedOriginPatterns) |
| 허용 메서드 | HEAD, POST, GET, DELETE, PUT | SecurityConfig |
| CSRF | Disabled | CookieCsrfTokenRepository 설정 후 disable |

## 배포/운영 고려 사항
- 무상태 서버 구조로 수평 확장 용이
- Redis로 토큰 블랙리스트/세션성 데이터 관리
- S3로 이미지 저장 및 CDN 연동 권장
- Elasticsearch는 매니지드 클러스터 또는 VPC 내 배포 권장

## 라이선스/기여
- 내부 프로젝트 기준. 외부 공개 시 라이선스 명시 필요
- 이슈/PR 템플릿 도입 권장

—
본 문서는 현재 코드베이스(컨트롤러/설정/Gradle 의존성 등)에 기반하여 작성되었습니다. 보완할 내용(정확한 엔드포인트, 환경 변수 키 등)이 있다면 이슈로 알려주세요.
