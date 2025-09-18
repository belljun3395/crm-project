# 스케줄러 시스템 개선: AWS EventBridge → Redis + Kafka 하이브리드

## 개요

기존 AWS EventBridge에 종속된 스케줄링 시스템을 벤더 독립적인 Redis + Kafka 하이브리드 시스템으로 개선했습니다. 이를 통해 클라우드 벤더 종속성을 제거하고, 더 유연하고 확장 가능한 스케줄링 시스템을 구축했습니다.

## 아키텍처 변경사항

### Before (AWS EventBridge 기반)
```
ScheduleTaskServiceImpl → AwsSchedulerService → AWS EventBridge → SQS → Application
```

### After (Redis + Kafka 하이브리드)
```
ScheduleTaskServiceImpl → SchedulerProvider Interface
                        ├── AwsSchedulerProvider (AWS EventBridge)
                        └── RedisSchedulerProvider (Redis + Kafka)
                            ├── Redis Sorted Set (정확한 스케줄 관리)
                            ├── RedisScheduleMonitoringService (폴링)
                            ├── KafkaScheduledTaskExecutor (이벤트 발행)
                            └── ScheduledTaskConsumer (실제 작업 처리)
```

## 핵심 컴포넌트

### 1. SchedulerProvider 인터페이스
- 벤더 독립적인 스케줄러 추상화
- AWS와 Redis+Kafka 구현체를 통일된 인터페이스로 관리

### 2. RedisSchedulerProvider
- Redis Sorted Set을 활용한 정확한 시간 기반 스케줄링
- Lua 스크립트를 사용한 원자적 연산 보장
- 멀티 인스턴스 환경에서 중복 실행 방지

### 3. RedisScheduleMonitoringService
- 1초마다 만료된 스케줄 폴링
- 비동기 병렬 처리로 성능 최적화
- 실패한 작업은 재시도를 위해 Redis에 유지

### 4. KafkaScheduledTaskExecutor
- Kafka를 통한 안정적인 이벤트 전파
- 높은 처리량과 영속성 보장
- 기존 SQS와 공존 가능

### 5. ScheduledTaskConsumer
- Kafka 메시지 수신 및 실제 비즈니스 로직 처리
- 수동 커밋을 통한 정확한 처리 보장

## 설정 방법

### 1. 환경별 설정

#### Local 환경 (AWS 기본값)
```yaml
scheduler:
  provider: aws
```

#### Development/Production 환경 (Redis+Kafka 권장)
```yaml
scheduler:
  provider: redis-kafka
  
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    consumer:
      group-id: crm-scheduler-group
```

### 2. 환경 변수
```bash
# Redis+Kafka 사용 시
SCHEDULER_PROVIDER=redis-kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# AWS 사용 시
SCHEDULER_PROVIDER=aws
AWS_SCHEDULE_ROLE_ARN=arn:aws:iam::account:role/role-name
AWS_SCHEDULE_SQS_ARN=arn:aws:sqs:region:account:queue-name
```

## 장점

### 1. 벤더 독립성
- AWS EventBridge에 종속되지 않음
- 온프레미스 및 멀티클라우드 환경 지원
- 비용 최적화 가능

### 2. 높은 정확성
- Redis Sorted Set을 통한 정확한 시간 기반 스케줄링
- 1초 단위의 정밀한 실행 시간 보장

### 3. 확장성
- Kafka를 통한 높은 처리량
- 멀티 인스턴스 환경에서 안정적 동작
- 수평 확장 가능

### 4. 안정성
- Redis 영속성을 통한 데이터 보존
- Kafka의 메시지 영속성과 재처리 보장
- 장애 복구 능력

### 5. 유연성
- 설정을 통한 런타임 전환 가능
- 기존 AWS 기반 시스템과 호환

## 성능 비교

| 항목 | AWS EventBridge | Redis + Kafka |
|------|----------------|---------------|
| 정확도 | 분 단위 | 초 단위 |
| 지연시간 | 높음 | 낮음 |
| 처리량 | 제한적 | 높음 |
| 비용 | 사용량 기반 | 인프라 기반 |
| 벤더 종속성 | 높음 | 없음 |

## 마이그레이션 가이드

### 1. 점진적 마이그레이션
1. 새로운 컴포넌트 배포
2. 설정을 통해 기존 AWS → Redis+Kafka 전환
3. 모니터링 및 검증
4. 기존 AWS 설정 정리

### 2. 롤백 방안
```yaml
# 문제 발생 시 즉시 AWS로 롤백
scheduler:
  provider: aws
```

## 모니터링

### 1. Redis 메트릭
- `scheduled:tasks` Sorted Set 크기
- 만료된 작업 처리 지연시간
- Redis 연결 상태

### 2. Kafka 메트릭
- `scheduled-tasks-execution` 토픽 처리량
- Consumer lag
- 실패한 메시지 수

### 3. 애플리케이션 로그
- 스케줄 생성/삭제/실행 로그
- 에러 및 재시도 로그
- 성능 지표

## 테스트

### 1. 단위 테스트
- `RedisSchedulerProviderTest`
- `KafkaScheduledTaskExecutorTest`
- `RedisScheduleMonitoringServiceTest`

### 2. 통합 테스트
- `RedisKafkaSchedulerIntegrationTest`
- 실제 Redis와 Kafka를 사용한 전체 플로우 검증

### 3. 테스트 실행
```bash
# Redis+Kafka 스케줄러 테스트
./gradlew test --tests="*RedisKafkaScheduler*"

# 전체 스케줄러 관련 테스트
./gradlew test --tests="*scheduler*"
```

## 향후 개선 사항

1. **Dead Letter Queue (DLQ) 구현**
   - 실패한 작업의 별도 처리 로직

2. **스케줄 백업/복원 기능**
   - Redis 장애 시 데이터 복구 방안

3. **동적 스케줄링**
   - 런타임에 스케줄 패턴 변경

4. **웹 UI 대시보드**
   - 스케줄 현황 시각화 및 관리

5. **메트릭 및 알림**
   - Prometheus/Grafana 연동
   - 장애 알림 시스템

## 결론

이번 개선을 통해 AWS EventBridge 종속성을 제거하고, 더 정확하고 확장 가능한 스케줄링 시스템을 구축했습니다. Redis + Kafka 하이브리드 접근법은 각각의 강점을 살려 높은 성능과 안정성을 제공합니다.