# Domain and UseCase Flows Documentation

## 개요

이 문서는 CRM 시스템의 Domain Entity와 UseCase의 플로우를 정리한 문서입니다. 

CRM 시스템은 **User**, **Email**, **Event** 세 가지 주요 모듈로 구성되며, Clean Architecture와 Domain-Driven Design (DDD) 원칙을 따라 설계되었습니다.

## 아키텍처 개요

```mermaid
graph TB
    subgraph "Interface Layer"
        UC[UserController]
        EC[EmailController]
        EVC[EventController]
    end
    
    subgraph "Application Layer"
        UUC[User UseCases]
        EUC[Email UseCases]
        EVUC[Event UseCases]
    end
    
    subgraph "Domain Layer"
        subgraph "User Module"
            U[User]
            UR[UserRepository]
        end
        
        subgraph "Email Module"
            ET[EmailTemplate]
            ETH[EmailTemplateHistory]
            ESH[EmailSendHistory]
            SE[ScheduledEvent]
            ER[EmailRepository]
        end
        
        subgraph "Event Module"
            E[Event]
            C[Campaign]
            CE[CampaignEvents]
            EVR[EventRepository]
        end
    end
    
    subgraph "Infrastructure"
        DB[(MySQL)]
        REDIS[(Redis)]
        MAIL[Mail Service]
    end
    
    UC --> UUC
    EC --> EUC
    EVC --> EVUC
    
    UUC --> U
    UUC --> UR
    
    EUC --> ET
    EUC --> ETH
    EUC --> ESH
    EUC --> SE
    EUC --> ER
    
    EVUC --> E
    EVUC --> C
    EVUC --> CE
    EVUC --> EVR
    
    UR --> DB
    ER --> DB
    EVR --> DB
    
    UUC --> REDIS
    EVUC --> REDIS
    EUC --> MAIL
```

## Domain Entities

### User Module

#### User Entity
- **테이블**: `users`
- **주요 속성**:
  - `id`: 사용자 ID (PK)
  - `externalId`: 외부 시스템 연동용 ID
  - `userAttributes`: JSON 형태의 사용자 속성 (이메일, 기타 정보)
  - `createdAt`, `updatedAt`: 생성/수정 시간

```mermaid
erDiagram
    User {
        bigint id PK
        varchar external_id
        json user_attributes
        datetime created_at
        datetime updated_at
    }
```

### Email Module

#### EmailTemplate Entity
- **테이블**: `email_templates`
- **주요 속성**:
  - `id`: 템플릿 ID (PK)
  - `templateName`: 템플릿 이름
  - `subject`: 이메일 제목
  - `body`: 이메일 본문 (HTML)
  - `variables`: 템플릿 변수 정의
  - `version`: 템플릿 버전

#### EmailTemplateHistory Entity
- **테이블**: `email_template_histories`
- **용도**: 템플릿 버전 관리

#### EmailSendHistory Entity
- **테이블**: `email_send_histories`
- **주요 속성**:
  - `userId`: 수신자 사용자 ID
  - `userEmail`: 수신자 이메일
  - `sendStatus`: 발송 상태
  - `emailBody`: 발송된 이메일 본문

#### ScheduledEvent Entity
- **테이블**: `scheduled_events`
- **주요 속성**:
  - `eventId`: 스케줄 이벤트 ID
  - `eventClass`: 이벤트 클래스명
  - `completed`: 완료 여부
  - `canceled`: 취소 여부
  - `scheduledAt`: 스케줄 시간

```mermaid
erDiagram
    EmailTemplate {
        bigint id PK
        varchar template_name
        varchar subject
        text body
        json variables
        float version
        datetime created_at
    }
    
    EmailTemplateHistory {
        bigint id PK
        bigint template_id FK
        varchar subject
        text body
        json variables
        float version
        datetime created_at
    }
    
    EmailSendHistory {
        bigint id PK
        bigint user_id FK
        varchar user_email
        varchar email_message_id
        text email_body
        varchar send_status
        datetime created_at
        datetime updated_at
    }
    
    ScheduledEvent {
        bigint id PK
        varchar event_id
        varchar event_class
        text event_payload
        boolean completed
        boolean is_not_consumed
        boolean canceled
        varchar scheduled_at
        datetime created_at
    }
    
    EmailTemplate ||--o{ EmailTemplateHistory : "has versions"
    User ||--o{ EmailSendHistory : "receives emails"
```

### Event Module

#### Campaign Entity
- **테이블**: `campaigns`
- **주요 속성**:
  - `id`: 캠페인 ID (PK)
  - `name`: 캠페인 이름 (고유)
  - `properties`: 캠페인 속성 정의 (JSON)

#### Event Entity
- **테이블**: `events`
- **주요 속성**:
  - `id`: 이벤트 ID (PK)
  - `name`: 이벤트 이름
  - `userId`: 사용자 ID
  - `properties`: 이벤트 속성 (JSON)

#### CampaignEvents Entity
- **테이블**: `campaign_events`
- **용도**: Campaign과 Event 간의 다대다 관계 매핑

```mermaid
erDiagram
    Campaign {
        bigint id PK
        varchar name UK
        json properties
        datetime created_at
    }
    
    Event {
        bigint id PK
        varchar name
        bigint user_id FK
        json properties
        datetime created_at
    }
    
    CampaignEvents {
        bigint id PK
        bigint campaign_id FK
        bigint event_id FK
        datetime created_at
    }
    
    User ||--o{ Event : "generates"
    Campaign ||--o{ CampaignEvents : "contains"
    Event ||--o{ CampaignEvents : "belongs to"
```

## UseCase 플로우

### User Module UseCases

#### 1. EnrollUserUseCase
사용자를 등록하거나 업데이트합니다.

```mermaid
sequenceDiagram
    participant Client
    participant UserController
    participant EnrollUserUseCase
    participant UserRepository
    participant Database
    
    Client->>UserController: POST /api/v1/users
    UserController->>EnrollUserUseCase: execute(EnrollUserUseCaseIn)
    
    alt ID가 제공된 경우
        EnrollUserUseCase->>UserRepository: findById(id)
        UserRepository->>Database: SELECT FROM users WHERE id = ?
        Database-->>UserRepository: User entity
        UserRepository-->>EnrollUserUseCase: User
        EnrollUserUseCase->>EnrollUserUseCase: updateAttributes()
        EnrollUserUseCase->>UserRepository: save(user)
    else ID가 제공되지 않은 경우
        EnrollUserUseCase->>EnrollUserUseCase: User.new()
        EnrollUserUseCase->>UserRepository: save(newUser)
    end
    
    UserRepository->>Database: INSERT/UPDATE users
    Database-->>UserRepository: Saved user
    UserRepository-->>EnrollUserUseCase: User
    EnrollUserUseCase-->>UserController: EnrollUserUseCaseOut
    UserController-->>Client: ApiResponse
```

#### 2. BrowseUserUseCase
전체 사용자 목록을 조회합니다.

#### 3. GetTotalUserCountUseCase
총 사용자 수를 조회합니다.

### Email Module UseCases

#### 1. SendNotificationEmailUseCase
사용자들에게 알림 이메일을 발송합니다.

```mermaid
sequenceDiagram
    participant Client
    participant EmailController
    participant SendNotificationEmailUseCase
    participant EmailTemplateRepository
    participant UserRepository
    participant MailService
    participant Database
    
    Client->>EmailController: POST /api/v1/emails/send/notifications
    EmailController->>SendNotificationEmailUseCase: execute(SendNotificationEmailUseCaseIn)
    
    SendNotificationEmailUseCase->>EmailTemplateRepository: findById(templateId)
    EmailTemplateRepository->>Database: SELECT FROM email_templates
    Database-->>EmailTemplateRepository: EmailTemplate
    EmailTemplateRepository-->>SendNotificationEmailUseCase: NotificationEmailTemplatePropertiesModel
    
    SendNotificationEmailUseCase->>UserRepository: getTargetUsers()
    UserRepository->>Database: SELECT FROM users
    Database-->>UserRepository: List<User>
    UserRepository-->>SendNotificationEmailUseCase: List<User>
    
    SendNotificationEmailUseCase->>SendNotificationEmailUseCase: generateNotificationDto()
    
    loop 각 사용자에 대해 (병렬 처리)
        SendNotificationEmailUseCase->>MailService: send(SendEmailInDto)
        MailService-->>SendNotificationEmailUseCase: 발송 결과
    end
    
    SendNotificationEmailUseCase-->>EmailController: SendNotificationEmailUseCaseOut
    EmailController-->>Client: ApiResponse
```

#### 2. PostTemplateUseCase
이메일 템플릿을 생성하거나 수정합니다.

#### 3. PostEmailNotificationSchedulesUseCase
이메일 알림을 스케줄링합니다.

#### 4. CancelNotificationEmailUseCase
스케줄된 이메일 알림을 취소합니다.

### Event Module UseCases

#### 1. PostEventUseCase
이벤트를 생성하고 캠페인과 연결합니다.

```mermaid
sequenceDiagram
    participant Client
    participant EventController
    participant PostEventUseCase
    participant UserRepository
    participant EventRepository
    participant CampaignRepository
    participant CampaignEventsRepository
    participant CampaignCacheManager
    participant Database
    
    Client->>EventController: POST /api/v1/events
    EventController->>PostEventUseCase: execute(PostEventUseCaseIn)
    
    PostEventUseCase->>UserRepository: findByExternalId(externalId)
    UserRepository->>Database: SELECT FROM users WHERE external_id = ?
    Database-->>UserRepository: User
    UserRepository-->>PostEventUseCase: userId
    
    par 병렬 처리
        PostEventUseCase->>EventRepository: save(Event.new())
        EventRepository->>Database: INSERT INTO events
        Database-->>EventRepository: Event
        EventRepository-->>PostEventUseCase: savedEvent
    and
        alt campaignName이 제공된 경우
            PostEventUseCase->>CampaignCacheManager: loadAndSaveIfMiss()
            CampaignCacheManager->>CampaignRepository: findCampaignByName()
            CampaignRepository->>Database: SELECT FROM campaigns WHERE name = ?
            Database-->>CampaignRepository: Campaign
            CampaignRepository-->>CampaignCacheManager: Campaign
            CampaignCacheManager-->>PostEventUseCase: Campaign
        end
    end
    
    alt Campaign이 존재하고 속성이 일치하는 경우
        PostEventUseCase->>PostEventUseCase: allMatchPropertyKeys() 검증
        PostEventUseCase->>CampaignEventsRepository: save(CampaignEvents.new())
        CampaignEventsRepository->>Database: INSERT INTO campaign_events
        PostEventUseCase-->>EventController: EVENT_SAVE_WITH_CAMPAIGN
    else 속성이 불일치하는 경우
        PostEventUseCase-->>EventController: PROPERTIES_MISMATCH
    else Campaign이 없는 경우
        PostEventUseCase-->>EventController: EVENT_SAVE_BUT_NOT_CAMPAIGN
    end
    
    EventController-->>Client: ApiResponse
```

#### 2. PostCampaignUseCase
캠페인을 생성합니다.

#### 3. SearchEventsUseCase
이벤트를 검색합니다.

## 주요 특징

### 1. 반응형 프로그래밍
- **Spring WebFlux** 사용으로 모든 데이터베이스 작업이 비동기(`suspend` functions)
- **R2DBC**를 통한 반응형 데이터베이스 접근

### 2. JSON 저장 패턴
- **User.userAttributes**: 사용자의 동적 속성 저장 (이메일 등)
- **Campaign.properties**: 캠페인의 속성 정의
- **Event.properties**: 이벤트의 속성 값
- **EmailTemplate.variables**: 템플릿 변수 정의

### 3. 캐싱 전략
- **Redis**를 통한 분산 캐싱
- **CampaignCacheManager**: 캠페인 정보 캐싱
- **UserCacheManager**: 사용자 정보 캐싱

### 4. 이벤트 기반 아키텍처
- **Domain Events**: 모듈 간 느슨한 결합
- **PostEmailTemplateEvent**: 템플릿 수정 시 발생
- 비동기 이벤트 처리

### 5. 이메일 시스템
- **다중 프로바이더 지원**: AWS SES, JavaMail
- **템플릿 버전 관리**: EmailTemplateHistory를 통한 버전 관리
- **스케줄링**: ScheduledEvent를 통한 예약 발송
- **병렬 처리**: 대용량 이메일 발송 시 병렬 처리 (concurrency = 10)

### 6. 캠페인-이벤트 연동
- 이벤트 생성 시 캠페인과 자동 연결
- 속성 키 검증을 통한 데이터 일관성 보장
- 캐싱을 통한 성능 최적화

## Entity 간의 관계도

```mermaid
graph TB
    subgraph "User Module"
        User[User<br/>- externalId<br/>- userAttributes JSON<br/>- 이메일 정보 포함]
    end
    
    subgraph "Email Module"
        EmailTemplate[EmailTemplate<br/>- templateName<br/>- subject, body<br/>- variables JSON<br/>- version]
        EmailTemplateHistory[EmailTemplateHistory<br/>- 템플릿 버전 이력]
        EmailSendHistory[EmailSendHistory<br/>- 발송 이력<br/>- sendStatus]
        ScheduledEvent[ScheduledEvent<br/>- 예약 이벤트<br/>- scheduledAt]
    end
    
    subgraph "Event Module"
        Campaign[Campaign<br/>- name unique<br/>- properties JSON<br/>- 속성 정의]
        Event[Event<br/>- name<br/>- properties JSON<br/>- 속성 값]
        CampaignEvents[CampaignEvents<br/>- 연결 테이블]
    end
    
    User -->|generates| Event
    User -->|receives| EmailSendHistory
    
    EmailTemplate -->|has versions| EmailTemplateHistory
    EmailTemplate -->|used for| EmailSendHistory
    
    Campaign -->|contains| CampaignEvents
    Event -->|belongs to| CampaignEvents
    
    Event -.->|속성 검증| Campaign
```

## 데이터 플로우

```mermaid
graph LR
    subgraph "사용자 등록 플로우"
        A1[사용자 등록 요청] --> A2[User Entity 생성]
        A2 --> A3[JSON 속성 저장]
        A3 --> A4[이메일 추출 가능]
    end
    
    subgraph "이벤트 생성 플로우"
        B1[이벤트 생성 요청] --> B2[User 검증]
        B2 --> B3[Event Entity 생성]
        B3 --> B4[Campaign 연결]
        B4 --> B5[속성 일치 검증]
        B5 --> B6[CampaignEvents 생성]
    end
    
    subgraph "이메일 발송 플로우"
        C1[이메일 발송 요청] --> C2[Template 조회]
        C2 --> C3[Target User 조회]
        C3 --> C4[이메일 내용 생성]
        C4 --> C5[병렬 발송]
        C5 --> C6[발송 이력 저장]
    end
    
    A4 -.-> C3
    B6 -.-> C3
```

이 문서는 CRM 시스템의 현재 구현 상태를 반영하여 작성되었으며, 코드 변경 시 자동으로 업데이트됩니다.