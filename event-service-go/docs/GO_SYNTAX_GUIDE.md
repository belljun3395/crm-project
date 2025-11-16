# Go 문법 가이드 - Event Service 프로젝트 기반

이 문서는 Event Service Go 프로젝트에서 사용된 실제 코드를 예제로 Go의 핵심 문법을 설명합니다.

## 목차
1. [패키지와 임포트](#1-패키지와-임포트)
2. [구조체 (Struct)](#2-구조체-struct)
3. [메서드 (Method)](#3-메서드-method)
4. [인터페이스 (Interface)](#4-인터페이스-interface)
5. [포인터 (Pointer)](#5-포인터-pointer)
6. [에러 처리](#6-에러-처리)
7. [defer 문](#7-defer-문)
8. [Goroutines와 채널](#8-goroutines와-채널)
9. [슬라이스와 맵](#9-슬라이스와-맵)
10. [JSON 태그](#10-json-태그)
11. [타입 어서션](#11-타입-어서션)
12. [컨텍스트 (Context)](#12-컨텍스트-context)
13. [함수 타입과 클로저](#13-함수-타입과-클로저)
14. [빌드 태그](#14-빌드-태그)

---

## 1. 패키지와 임포트

### 패키지 선언
```go
package main  // 실행 가능한 프로그램의 진입점
```

```go
package service  // 라이브러리 패키지
```

**규칙:**
- 모든 Go 파일은 `package` 선언으로 시작
- `package main`은 실행 파일을 만들 때 사용
- 다른 패키지명은 라이브러리로 사용

### 임포트 (Import)

**프로젝트 예제: `main.go`**
```go
import (
    "context"              // 표준 라이브러리
    "fmt"
    "net/http"

    "event-service-go/internal/config"      // 내부 패키지
    "event-service-go/internal/middleware"

    "github.com/gin-gonic/gin"              // 외부 패키지
    "go.uber.org/zap"
)
```

**규칙:**
- 표준 라이브러리 먼저
- 빈 줄
- 프로젝트 내부 패키지
- 빈 줄
- 외부 패키지

**별칭 사용:**
```go
import (
    swaggerFiles "github.com/swaggo/files"        // 별칭으로 임포트
    ginSwagger "github.com/swaggo/gin-swagger"
    apperrors "event-service-go/pkg/errors"       // 패키지명 충돌 방지
)
```

---

## 2. 구조체 (Struct)

### 기본 구조체

**프로젝트 예제: `internal/model/model.go`**
```go
type Event struct {
    ID         int64      `json:"id" db:"id"`
    Name       string     `json:"name" db:"name"`
    UserID     int64      `json:"userId" db:"user_id"`
    Properties Properties `json:"properties" db:"properties"`
    CreatedAt  time.Time  `json:"createdAt" db:"created_at"`
}
```

**포인트:**
- 대문자로 시작하는 필드는 **exported** (public)
- 소문자로 시작하는 필드는 **unexported** (private)
- 백틱(`)으로 태그 지정 가능

### 중첩 구조체

**프로젝트 예제: `internal/config/config.go`**
```go
type Config struct {
    Server   ServerConfig    // 중첩 구조체
    Database DatabaseConfig
    Redis    RedisConfig
}

type ServerConfig struct {
    Port string
    Env  string
}
```

### 구조체 초기화

```go
// 1. 필드명 명시 (권장)
event := &model.Event{
    Name:      "purchase",
    UserID:    1,
    CreatedAt: time.Now(),
}

// 2. 순서대로 (비권장 - 유지보수 어려움)
event := &model.Event{1, "purchase", 1, props, time.Now()}

// 3. 빈 구조체 생성 후 할당
var event model.Event
event.Name = "purchase"
event.UserID = 1
```

---

## 3. 메서드 (Method)

### 값 리시버 (Value Receiver)

**프로젝트 예제: `internal/model/model.go`**
```go
// Properties 타입에 대한 메서드
func (p Properties) GetKeys() []string {
    keys := make([]string, len(p))
    for i, prop := range p {
        keys[i] = prop.Key
    }
    return keys
}
```

**사용:**
```go
properties := model.Properties{
    {Key: "product", Value: "laptop"},
}
keys := properties.GetKeys()  // ["product"]
```

### 포인터 리시버 (Pointer Receiver)

**프로젝트 예제: `internal/repository/event_repository.go`**
```go
type EventRepository struct {
    db *gorm.DB
}

// 포인터 리시버 - 구조체 수정 가능
func (r *EventRepository) Create(ctx context.Context, event *model.Event) error {
    return r.db.WithContext(ctx).Create(event).Error
}
```

**포인터 리시버를 사용하는 경우:**
1. 구조체를 수정해야 할 때
2. 구조체가 클 때 (복사 비용 절감)
3. 일관성 유지 (한 타입의 메서드는 모두 동일한 리시버 타입 사용 권장)

---

## 4. 인터페이스 (Interface)

### 암시적 인터페이스 구현

Go는 **명시적 선언 없이** 메서드만 구현하면 인터페이스를 만족합니다.

**예제: `database/sql/driver` 인터페이스 구현**
```go
// driver.Valuer 인터페이스 (표준 라이브러리에 정의됨)
// type Valuer interface {
//     Value() (Value, error)
// }

// Properties가 암시적으로 Valuer 인터페이스 구현
func (p Properties) Value() (driver.Value, error) {
    return json.Marshal(p)
}

// Scanner 인터페이스도 구현
func (p *Properties) Scan(value interface{}) error {
    if value == nil {
        *p = Properties{}
        return nil
    }
    bytes, ok := value.([]byte)
    if !ok {
        return errors.New("failed to unmarshal Properties value")
    }
    return json.Unmarshal(bytes, p)
}
```

### 빈 인터페이스 (Empty Interface)

**프로젝트 예제: `internal/dto/response.go`**
```go
type SuccessResponse struct {
    Success bool        `json:"success"`
    Data    interface{} `json:"data"`  // 어떤 타입이든 가능
}
```

**사용:**
```go
// 어떤 타입이든 담을 수 있음
response := SuccessResponse{
    Success: true,
    Data:    eventResponse,  // *CreateEventResponse
}

response2 := SuccessResponse{
    Success: true,
    Data:    []EventDTO{...},  // []EventDTO
}
```

---

## 5. 포인터 (Pointer)

### 포인터 기본

```go
// 값 타입
var count int = 10
var name string = "test"

// 포인터 타입 (앞에 * 붙음)
var eventPtr *model.Event
var userPtr *model.User
```

### &와 * 연산자

```go
// & : 주소 연산자 (address-of)
event := model.Event{Name: "purchase"}
eventPtr := &event  // event의 주소

// * : 역참조 연산자 (dereference)
name := (*eventPtr).Name  // 포인터를 통해 값 접근
name := eventPtr.Name     // Go는 자동으로 역참조 (동일)
```

**프로젝트 예제: `internal/service/event_service.go`**
```go
func (s *EventService) CreateEvent(
    ctx context.Context,
    req *dto.CreateEventRequest,  // 포인터로 받음 (복사 비용 절감)
) (*dto.CreateEventResponse, error) {

    // 새로운 이벤트 생성 (포인터로)
    event := &model.Event{
        Name:       req.Name,
        UserID:     user.ID,
        Properties: properties,
        CreatedAt:  time.Now(),
    }

    // 포인터를 전달 (Create 메서드가 ID 설정 가능)
    if err := s.eventRepo.Create(ctx, event); err != nil {
        return nil, err
    }

    // event.ID가 설정되어 있음 (포인터라서 가능)
    return &dto.CreateEventResponse{
        ID: event.ID,
    }, nil
}
```

### nil 포인터 체크

**프로젝트 예제: `internal/service/event_service.go`**
```go
user, err := s.userRepo.FindByExternalID(ctx, req.ExternalID)
if err != nil {
    return nil, apperrors.NewDatabaseError("Failed to find user", err)
}
if user == nil {  // nil 체크 필수!
    return nil, apperrors.NewUserNotFoundError(req.ExternalID)
}
```

---

## 6. 에러 처리

### 에러 반환

Go는 예외(exception) 대신 **에러 값을 반환**합니다.

**프로젝트 예제:**
```go
func (r *EventRepository) Create(ctx context.Context, event *model.Event) error {
    return r.db.WithContext(ctx).Create(event).Error
}

// 사용
err := repo.Create(ctx, event)
if err != nil {
    // 에러 처리
    log.Printf("Failed to create event: %v", err)
    return err
}
```

### 다중 반환값

**프로젝트 예제: `internal/repository/user_repository.go`**
```go
func (r *UserRepository) FindByExternalID(
    ctx context.Context,
    externalID string,
) (*model.User, error) {  // 값과 에러 둘 다 반환

    var user model.User
    if err := r.db.WithContext(ctx).
        Where("external_id = ?", externalID).
        First(&user).Error; err != nil {

        // GORM의 특정 에러 체크
        if errors.Is(err, gorm.ErrRecordNotFound) {
            return nil, nil  // 에러 없이 nil 반환
        }
        return nil, err  // 실제 에러 반환
    }
    return &user, nil  // 정상 반환
}
```

### 커스텀 에러 타입

**프로젝트 예제: `pkg/errors/errors.go`**
```go
type AppError struct {
    Code       string
    Message    string
    StatusCode int
    Err        error
}

// error 인터페이스 구현
func (e *AppError) Error() string {
    if e.Err != nil {
        return fmt.Sprintf("%s: %v", e.Message, e.Err)
    }
    return e.Message
}

// error wrapping 지원
func (e *AppError) Unwrap() error {
    return e.Err
}
```

### errors.Is와 errors.As

```go
import "errors"

// errors.Is: 특정 에러인지 확인
if errors.Is(err, gorm.ErrRecordNotFound) {
    // 레코드를 찾을 수 없음
}

// errors.As: 특정 타입의 에러로 변환
var appErr *apperrors.AppError
if errors.As(err, &appErr) {
    // appErr 사용 가능
    statusCode := appErr.StatusCode
}
```

**프로젝트 예제: `internal/handler/event_handler.go`**
```go
func (h *EventHandler) respondWithError(c *gin.Context, err error) {
    if appErr, ok := err.(*apperrors.AppError); ok {
        // 타입 어서션으로 AppError 체크
        c.JSON(appErr.StatusCode, dto.ErrorResponse{
            Success: false,
            Error:   appErr.Message,
            Code:    appErr.Code,
        })
        return
    }

    // 일반 에러
    c.JSON(http.StatusInternalServerError, dto.ErrorResponse{
        Success: false,
        Error:   "Internal server error",
    })
}
```

---

## 7. defer 문

`defer`는 함수가 **종료되기 직전에** 실행됩니다.

### 기본 사용

**프로젝트 예제: `main.go`**
```go
func main() {
    app, err := wire.InitializeApp(cfg)
    if err != nil {
        fmt.Printf("Failed to initialize app: %v\n", err)
        os.Exit(1)
    }
    defer app.Logger.Sync()  // main 함수 종료 시 로거 flush

    // ... 나머지 코드 ...

    // main이 끝날 때 app.Logger.Sync() 자동 호출됨
}
```

### 여러 defer의 실행 순서

**LIFO (Last In, First Out) 순서**로 실행됩니다.

```go
func example() {
    defer fmt.Println("1")
    defer fmt.Println("2")
    defer fmt.Println("3")
    fmt.Println("function body")
}

// 출력:
// function body
// 3
// 2
// 1
```

### panic 복구

**프로젝트 예제: `internal/middleware/recovery.go`**
```go
func Recovery(logger *zap.Logger) gin.HandlerFunc {
    return func(c *gin.Context) {
        defer func() {
            if err := recover(); err != nil {  // panic 발생 시 복구
                requestID := GetRequestID(c)

                logger.Error("Panic recovered",
                    zap.Any("error", err),
                    zap.String("request_id", requestID),
                    zap.Stack("stack"),  // 스택 트레이스
                )

                c.JSON(http.StatusInternalServerError, dto.ErrorResponse{
                    Success: false,
                    Error:   "Internal server error",
                })

                c.Abort()
            }
        }()

        c.Next()  // 다음 핸들러 실행 (panic 발생 가능)
    }
}
```

### defer와 리소스 정리

```go
// 파일 닫기
file, err := os.Open("file.txt")
if err != nil {
    return err
}
defer file.Close()  // 함수 종료 시 자동으로 파일 닫힘

// DB 연결 정리
db, err := sql.Open("mysql", dsn)
if err != nil {
    return err
}
defer db.Close()

// Context 취소
ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
defer cancel()  // 함수 종료 시 자동으로 컨텍스트 취소
```

---

## 8. Goroutines와 채널

### Goroutine 기본

**Goroutine**은 Go의 경량 스레드입니다.

**프로젝트 예제: `main.go`**
```go
// HTTP 서버를 고루틴으로 실행
go func() {
    app.Logger.Info("Server listening", zap.String("port", cfg.Server.Port))
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        app.Logger.Fatal("Failed to start server", zap.Error(err))
    }
}()

// 메인 고루틴은 계속 진행
log.Println("Server started in background")
```

### 채널 (Channel)

채널은 고루틴 간 **통신**을 위한 파이프입니다.

**프로젝트 예제: `main.go`**
```go
// OS 시그널을 받을 채널 생성
quit := make(chan os.Signal, 1)  // 버퍼 크기 1

// 시그널을 채널로 전달하도록 설정
signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)

// 채널에서 값이 올 때까지 블로킹
<-quit  // SIGINT나 SIGTERM이 올 때까지 대기

log.Println("Shutting down server...")
```

### 채널 패턴

```go
// 1. 채널 생성
ch := make(chan int)       // unbuffered 채널
ch := make(chan int, 10)   // buffered 채널 (크기 10)

// 2. 채널에 값 전송
ch <- 42

// 3. 채널에서 값 수신
value := <-ch

// 4. 채널 닫기
close(ch)

// 5. range로 채널 읽기
for value := range ch {
    fmt.Println(value)
}

// 6. select로 여러 채널 처리
select {
case msg := <-ch1:
    fmt.Println("Received from ch1:", msg)
case msg := <-ch2:
    fmt.Println("Received from ch2:", msg)
case <-time.After(1 * time.Second):
    fmt.Println("Timeout")
}
```

---

## 9. 슬라이스와 맵

### 슬라이스 (Slice)

슬라이스는 **동적 배열**입니다.

**프로젝트 예제: `internal/model/model.go`**
```go
type Properties []Property  // Property의 슬라이스

// 슬라이스 생성
props := make([]Property, 0)           // 길이 0, 용량 0
props := make([]Property, 0, 10)       // 길이 0, 용량 10
props := []Property{}                  // 리터럴
props := []Property{
    {Key: "product", Value: "laptop"},
    {Key: "amount", Value: "1200"},
}
```

**슬라이스 연산:**
```go
// append: 요소 추가
props = append(props, Property{Key: "color", Value: "silver"})

// len: 길이
count := len(props)

// cap: 용량
capacity := cap(props)

// 슬라이싱
first := props[0]           // 첫 번째 요소
last := props[len(props)-1] // 마지막 요소
subslice := props[1:3]      // 인덱스 1부터 2까지

// range로 순회
for i, prop := range props {
    fmt.Printf("%d: %s = %s\n", i, prop.Key, prop.Value)
}

// 인덱스 무시
for _, prop := range props {
    fmt.Printf("%s = %s\n", prop.Key, prop.Value)
}
```

**프로젝트 예제: `internal/service/event_service.go`**
```go
// 슬라이스 초기화 with make
properties := make(model.Properties, len(req.Properties))
for i, p := range req.Properties {
    properties[i] = model.Property{Key: p.Key, Value: p.Value}
}

// 빈 슬라이스로 시작하고 append
userIDs := make([]int64, 0, len(events))  // 용량 미리 할당
for _, event := range events {
    userIDs = append(userIDs, event.UserID)
}
```

### 맵 (Map)

맵은 **키-값 쌍**을 저장합니다.

**프로젝트 예제: `internal/service/event_service.go`**
```go
// 맵 생성
userMap := make(map[int64]*model.User)

// 값 할당
for i := range users {
    userMap[users[i].ID] = &users[i]
}

// 값 조회
user, exists := userMap[userID]
if exists {
    fmt.Println("Found:", user.ExternalID)
}

// 값 조회 (존재 여부 체크 없이)
user := userMap[userID]  // 없으면 nil 반환

// 삭제
delete(userMap, userID)

// range로 순회
for id, user := range userMap {
    fmt.Printf("%d: %s\n", id, user.ExternalID)
}
```

**중복 제거에 맵 사용:**
```go
// Set처럼 사용 (Go에는 Set이 없음)
userIDSet := make(map[int64]bool)

for _, event := range events {
    if !userIDSet[event.UserID] {  // 처음 보는 ID
        userIDs = append(userIDs, event.UserID)
        userIDSet[event.UserID] = true
    }
}
```

---

## 10. JSON 태그

### 구조체 태그

**프로젝트 예제: `internal/dto/event.go`**
```go
type PropertyDTO struct {
    Key   string `json:"key" binding:"required" example:"product"`
    Value string `json:"value" binding:"required" example:"laptop"`
}
```

**태그 종류:**
- `json:"fieldName"`: JSON 필드명 지정
- `json:"fieldName,omitempty"`: 값이 비어있으면 생략
- `json:"-"`: JSON에서 제외
- `binding:"required"`: Gin 검증 태그
- `db:"column_name"`: 데이터베이스 컬럼명 (GORM)

### JSON 인코딩/디코딩

```go
import "encoding/json"

// 구조체 -> JSON (Marshal)
event := model.Event{Name: "purchase", UserID: 1}
jsonBytes, err := json.Marshal(event)
if err != nil {
    return err
}
jsonString := string(jsonBytes)

// JSON -> 구조체 (Unmarshal)
var event model.Event
err := json.Unmarshal(jsonBytes, &event)  // 포인터 전달 필수!
if err != nil {
    return err
}
```

**프로젝트 예제: `internal/model/model.go`**
```go
// Value: 구조체를 JSON으로 변환하여 DB에 저장
func (p Properties) Value() (driver.Value, error) {
    return json.Marshal(p)
}

// Scan: DB의 JSON을 구조체로 변환
func (p *Properties) Scan(value interface{}) error {
    bytes, ok := value.([]byte)
    if !ok {
        return errors.New("failed to unmarshal")
    }
    return json.Unmarshal(bytes, p)
}
```

---

## 11. 타입 어서션

### 기본 타입 어서션

```go
var i interface{} = "hello"

// 타입 어서션 (panic 가능)
s := i.(string)
fmt.Println(s)  // "hello"

// 안전한 타입 어서션 (panic 없음)
s, ok := i.(string)
if ok {
    fmt.Println("String:", s)
} else {
    fmt.Println("Not a string")
}
```

**프로젝트 예제: `internal/handler/event_handler.go`**
```go
func (h *EventHandler) respondWithError(c *gin.Context, err error) {
    // AppError 타입인지 체크
    if appErr, ok := err.(*apperrors.AppError); ok {
        // ok가 true면 appErr 사용 가능
        c.JSON(appErr.StatusCode, dto.ErrorResponse{
            Success: false,
            Error:   appErr.Message,
            Code:    appErr.Code,
        })
        return
    }

    // AppError가 아닌 경우
    c.JSON(http.StatusInternalServerError, dto.ErrorResponse{
        Success: false,
        Error:   "Internal server error",
    })
}
```

### Type Switch

```go
func describe(i interface{}) string {
    switch v := i.(type) {
    case int:
        return fmt.Sprintf("Integer: %d", v)
    case string:
        return fmt.Sprintf("String: %s", v)
    case bool:
        return fmt.Sprintf("Boolean: %t", v)
    default:
        return fmt.Sprintf("Unknown type: %T", v)
    }
}

describe(42)      // "Integer: 42"
describe("hello") // "String: hello"
describe(true)    // "Boolean: true"
```

---

## 12. 컨텍스트 (Context)

Context는 **요청 범위의 데이터, 취소 신호, 타임아웃**을 전달합니다.

### Context 생성

```go
import "context"

// 1. Background context (최상위)
ctx := context.Background()

// 2. TODO context (임시)
ctx := context.TODO()

// 3. WithCancel (취소 가능)
ctx, cancel := context.WithCancel(context.Background())
defer cancel()

// 4. WithTimeout (타임아웃)
ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
defer cancel()

// 5. WithValue (값 전달)
ctx := context.WithValue(context.Background(), "userID", 123)
```

**프로젝트 예제: `main.go`**
```go
// Graceful shutdown with timeout
ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
defer cancel()

if err := srv.Shutdown(ctx); err != nil {
    app.Logger.Error("Server forced to shutdown", zap.Error(err))
}
```

### Context 사용

**프로젝트 예제: `internal/repository/event_repository.go`**
```go
func (r *EventRepository) Create(ctx context.Context, event *model.Event) error {
    // Context를 GORM에 전달
    return r.db.WithContext(ctx).Create(event).Error
}
```

**프로젝트 예제: `internal/handler/event_handler.go`**
```go
func (h *EventHandler) CreateEvent(c *gin.Context) {
    // Gin context에서 request context 가져오기
    resp, err := h.eventService.CreateEvent(c.Request.Context(), &req)
}
```

### Context 값 전달

**프로젝트 예제: `internal/middleware/request_id.go`**
```go
func RequestID() gin.HandlerFunc {
    return func(c *gin.Context) {
        requestID := uuid.New().String()

        // Gin context에 값 저장
        c.Set(RequestIDKey, requestID)

        c.Next()
    }
}

// 값 조회
func GetRequestID(c *gin.Context) string {
    if requestID, exists := c.Get(RequestIDKey); exists {
        return requestID.(string)  // 타입 어서션
    }
    return ""
}
```

---

## 13. 함수 타입과 클로저

### 함수를 값으로

Go에서 함수는 **일급 객체**입니다.

```go
// 함수 타입 정의
type HandlerFunc func(ctx context.Context) error

// 함수를 변수에 할당
var handler HandlerFunc = func(ctx context.Context) error {
    return nil
}

// 함수를 인자로 전달
func execute(fn HandlerFunc) error {
    return fn(context.Background())
}
```

**프로젝트 예제: `internal/middleware/logger.go`**
```go
// Gin의 HandlerFunc 타입
// type HandlerFunc func(*Context)

func Logger(logger *zap.Logger) gin.HandlerFunc {
    // 클로저: logger 변수를 캡처
    return func(c *gin.Context) {
        start := time.Now()

        c.Next()  // 다음 핸들러 실행

        // logger를 사용 (클로저로 캡처됨)
        logger.Info("Request processed",
            zap.Duration("latency", time.Since(start)),
        )
    }
}
```

### 클로저 (Closure)

**클로저**는 외부 변수를 캡처하는 함수입니다.

```go
func counter() func() int {
    count := 0  // 이 변수가 캡처됨

    return func() int {
        count++      // 외부 변수 수정
        return count
    }
}

c1 := counter()
fmt.Println(c1())  // 1
fmt.Println(c1())  // 2
fmt.Println(c1())  // 3

c2 := counter()    // 새로운 클로저
fmt.Println(c2())  // 1 (독립적인 count 변수)
```

**프로젝트 예제: `main.go`**
```go
// 고루틴에서 클로저 사용
go func() {
    // app, srv, cfg 변수를 캡처
    app.Logger.Info("Server listening", zap.String("port", cfg.Server.Port))
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        app.Logger.Fatal("Failed to start server", zap.Error(err))
    }
}()
```

---

## 14. 빌드 태그

**빌드 태그**는 조건부 컴파일을 위해 사용됩니다.

**프로젝트 예제: `internal/wire/wire.go`**
```go
//go:build wireinject
// +build wireinject

package wire
```

**의미:**
- `wireinject` 태그가 있을 때만 컴파일됨
- Wire 도구가 코드를 생성할 때만 사용
- 일반 빌드에서는 무시됨

### 빌드 태그 사용법

```go
// Linux에서만 컴파일
//go:build linux

// Windows에서만 컴파일
//go:build windows

// 여러 조건 (AND)
//go:build linux && amd64

// 여러 조건 (OR)
//go:build linux || darwin

// NOT 조건
//go:build !windows
```

---

## 추가: 자주 사용되는 패턴

### 1. 옵션 패턴 (Functional Options)

```go
type Server struct {
    host string
    port int
    timeout time.Duration
}

type Option func(*Server)

func WithHost(host string) Option {
    return func(s *Server) {
        s.host = host
    }
}

func WithPort(port int) Option {
    return func(s *Server) {
        s.port = port
    }
}

func NewServer(opts ...Option) *Server {
    s := &Server{
        host: "localhost",
        port: 8080,
        timeout: 30 * time.Second,
    }

    for _, opt := range opts {
        opt(s)
    }

    return s
}

// 사용
server := NewServer(
    WithHost("0.0.0.0"),
    WithPort(9000),
)
```

### 2. 생성자 패턴

**프로젝트에서 일관되게 사용:**
```go
func NewEventRepository(db *gorm.DB) *EventRepository {
    return &EventRepository{db: db}
}

func NewEventService(
    eventRepo *EventRepository,
    campaignRepo *CampaignRepository,
    campaignEventsRepo *CampaignEventsRepository,
    userRepo *UserRepository,
    logger *zap.Logger,
) *EventService {
    return &EventService{
        eventRepo:          eventRepo,
        campaignRepo:       campaignRepo,
        campaignEventsRepo: campaignEventsRepo,
        userRepo:           userRepo,
        logger:             logger,
    }
}
```

### 3. 에러 체크 패턴

```go
// 일반적인 패턴
if err != nil {
    return err
}

// 에러 래핑
if err != nil {
    return fmt.Errorf("failed to create event: %w", err)
}

// 조기 반환 (Early Return)
func process() error {
    if err := step1(); err != nil {
        return err
    }

    if err := step2(); err != nil {
        return err
    }

    if err := step3(); err != nil {
        return err
    }

    return nil
}
```

---

## 학습 순서 권장

1. **기본 문법** (1-4일)
   - 패키지와 임포트
   - 구조체와 메서드
   - 슬라이스와 맵

2. **핵심 개념** (5-7일)
   - 포인터
   - 에러 처리
   - 인터페이스

3. **동시성** (8-10일)
   - Goroutines
   - 채널
   - Context

4. **실전 패턴** (11-14일)
   - JSON 처리
   - 의존성 주입
   - 미들웨어 패턴

---

## 참고 자료

- [Go 공식 문서](https://golang.org/doc/)
- [A Tour of Go](https://tour.golang.org/)
- [Effective Go](https://golang.org/doc/effective_go)
- [Go by Example](https://gobyexample.com/)

## 프로젝트에서 학습하기

각 패키지의 코드를 읽으며 위 문법들이 어떻게 사용되는지 확인해보세요:

1. `pkg/errors/errors.go` - 커스텀 에러 타입
2. `internal/model/model.go` - 구조체와 메서드
3. `internal/repository/*.go` - 인터페이스 패턴, GORM 사용
4. `internal/service/*.go` - 비즈니스 로직, 에러 처리
5. `internal/middleware/*.go` - 클로저와 미들웨어 패턴
6. `internal/wire/wire.go` - 의존성 주입
7. `main.go` - 애플리케이션 구조, 고루틴, Context
