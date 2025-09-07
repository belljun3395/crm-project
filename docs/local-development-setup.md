# 로컬 개발 환경 설정 가이드

## 개요

CRM 시스템의 로컬 개발 환경은 **Redis + Kafka 하이브리드 스케줄링 시스템**을 완벽히 지원하도록 구성되었습니다. Docker Compose를 통해 원클릭으로 전체 개발 환경을 구축할 수 있습니다.

## 필수 요구사항

- **Docker**: 20.10 이상
- **Docker Compose**: 3.8 이상  
- **메모리**: 최소 8GB RAM 권장
- **포트**: 다음 포트들이 사용 가능해야 함
  - `2181`, `7001-7006`, `8090`, `13306`, `18080-18081`, `29092`, `4566`

## 환경 구성

### 1. 서비스 시작

```bash
cd resources/crm-local-develop-environment
docker-compose up -d
```

### 2. 서비스 상태 확인

```bash
# 모든 서비스 상태 확인
docker-compose ps

# 특정 서비스 로그 확인
docker-compose logs -f kafka
docker-compose logs -f crm-redis-cluster
```

### 3. 헬스체크 확인

```bash
# 모든 서비스가 healthy 상태인지 확인
docker-compose ps --filter "health=healthy"
```

## 서비스 접속 정보

### 📊 **관리 도구**

| 서비스 | URL | 용도 |
|--------|-----|------|
| **Kafka UI** | http://localhost:8090 | Kafka 토픽/메시지 모니터링 |
| **Redis Insight** | http://localhost:18081 | Redis 클러스터 관리 |
| **Adminer** | http://localhost:18080 | MySQL 데이터베이스 관리 |
| **LocalStack** | http://localhost:4566 | AWS 서비스 모킹 |

### 🔗 **데이터베이스 연결**

```yaml
# MySQL
Host: localhost
Port: 13306
Username: root
Password: root
Database: crm
```

### 🔧 **애플리케이션 설정**

```yaml
# application-local.yml
spring:
  r2dbc:
    url: r2dbc:pool:mysql://localhost:13306/crm
    username: root
    password: root
  
  data:
    redis:
      cluster:
        nodes: localhost:7001,localhost:7002,localhost:7003,localhost:7004,localhost:7005,localhost:7006
        password: password
  
  kafka:
    bootstrap-servers: localhost:29092

# 스케줄러 설정
scheduler:
  provider: redis-kafka  # 또는 aws
```

## 스케줄링 시스템 테스트

### 1. Redis + Kafka 스케줄러 사용

```yaml
# application-local.yml에서 설정
scheduler:
  provider: redis-kafka
```

**확인 방법:**
- **Redis Insight**: http://localhost:18081 접속 후 `scheduled:tasks` 키 확인
- **Kafka UI**: http://localhost:8090 접속 후 `scheduled-tasks-execution` 토픽 메시지 확인

### 2. AWS 스케줄러 사용 (LocalStack)

```yaml
# application-local.yml에서 설정
scheduler:
  provider: aws
```

**확인 방법:**
- LocalStack 대시보드에서 EventBridge 스케줄 확인

## 개발 워크플로우

### 1. 코드 변경 후 테스트

```bash
# 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 또는 IDE에서 실행 시
# VM Options: -Dspring.profiles.active=local
```

### 2. 스케줄러 동작 모니터링

```bash
# 애플리케이션 로그에서 스케줄러 동작 확인
grep "schedule" logs/application.log

# Redis에서 직접 스케줄 확인
docker exec -it crm-redis-cluster redis-cli -a password -c ZRANGE scheduled:tasks 0 -1 WITHSCORES

# Kafka 토픽 메시지 확인
docker exec -it crm-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic scheduled-tasks-execution --from-beginning
```

### 3. 데이터 초기화

```bash
# 특정 서비스만 재시작
docker-compose restart crm-mysql crm-redis-cluster kafka

# 모든 데이터 초기화 (볼륨 삭제)
docker-compose down -v
docker-compose up -d
```

## 문제 해결

### 📌 **자주 발생하는 문제들**

#### 1. Redis 클러스터 생성 실패
```bash
# 해결방법
docker-compose down
docker-compose up -d crm-redis-cluster crm-redis-node-1 crm-redis-node-2 crm-redis-node-3 crm-redis-node-4 crm-redis-node-5
sleep 10
docker-compose up -d crm-redis-cluster-create
```

#### 2. Kafka 토픽이 생성되지 않음
```bash
# 수동 토픽 생성
docker exec -it crm-kafka kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --topic scheduled-tasks-execution --partitions 3 --replication-factor 1
```

#### 3. 포트 충돌 문제
```bash
# 사용 중인 포트 확인
netstat -tulpn | grep -E "(2181|7001|8090|13306|18080|18081|29092|4566)"

# 충돌하는 서비스 종료 후 재시작
docker-compose down
docker-compose up -d
```

#### 4. 메모리 부족 문제
```bash
# Docker 메모리 한도 확인 및 증가
docker system df
docker system prune -a

# Docker Desktop 설정에서 메모리를 8GB 이상으로 설정
```

### 📊 **모니터링 대시보드**

#### Kafka 상태 확인
- **URL**: http://localhost:8090
- **주요 메트릭**: 토픽별 메시지 수, Consumer Lag, Partition 상태

#### Redis 클러스터 상태 확인  
- **URL**: http://localhost:18081
- **주요 메트릭**: 클러스터 노드 상태, 메모리 사용량, 키 분포

#### MySQL 데이터 확인
- **URL**: http://localhost:18080
- **로그인**: Server: `crm-mysql8`, Username: `root`, Password: `root`

## 성능 최적화 팁

### 1. 메모리 설정
```bash
# Docker Compose에서 메모리 제한 설정 (필요시)
docker-compose.yml에 deploy.resources.limits.memory 추가
```

### 2. JVM 힙 메모리 조정
```bash
# 개발용 JVM 옵션
export JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC"
./gradlew bootRun
```

### 3. Kafka 성능 최적화
- Producer: `acks=all`, `retries=3`
- Consumer: `max.poll.records=10`
- Partition: 3개 (CPU 코어 수에 맞춤)

## 정리 및 종료

```bash
# 서비스 중지 (데이터 유지)
docker-compose stop

# 완전 정리 (데이터 삭제)
docker-compose down -v
docker system prune -f
```

## 추가 리소스

- **스케줄러 마이그레이션 가이드**: [scheduler-migration.md](./scheduler-migration.md)
- **API 문서**: http://localhost:8080/swagger-ui.html (애플리케이션 실행 시)
- **애플리케이션 헬스체크**: http://localhost:8080/actuator/health

---

## 🎯 Quick Start 체크리스트

- [ ] Docker 및 Docker Compose 설치 확인
- [ ] `docker-compose up -d` 실행
- [ ] 모든 서비스가 `healthy` 상태인지 확인  
- [ ] Kafka UI (8090), Redis Insight (18081), Adminer (18080) 접속 확인
- [ ] 애플리케이션을 `local` 프로파일로 실행
- [ ] 스케줄러 동작 테스트 수행

**모든 체크리스트를 완료하면 개발 준비 완료!** ✅