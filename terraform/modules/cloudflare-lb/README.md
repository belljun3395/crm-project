# CloudFlare 글로벌 로드밸런서 모듈

CloudFlare를 사용한 글로벌 로드밸런싱 및 자동 장애조치를 구성하는 Terraform 모듈입니다.

## 기능

- ✅ Health Check 기반 자동 장애조치
- ✅ Dynamic Latency Routing
- ✅ Session Affinity
- ✅ Geographic Routing
- ✅ 실시간 모니터링

## 사용 예시

```hcl
module "cloudflare_lb" {
  source = "../../modules/cloudflare-lb"

  name_prefix           = "crm"
  cloudflare_account_id = var.cf_account_id
  cloudflare_zone_id    = var.cf_zone_id
  domain_name           = "crm.example.com"
  
  # AWS Origin
  aws_origins = [{
    name    = "aws-primary"
    address = "aws-lb-123.elb.amazonaws.com"
    enabled = true
    weight  = 1.0
  }]
  
  # GCP Origin
  gcp_origins = [{
    name    = "gcp-secondary"
    address = "35.123.45.67"
    enabled = true
    weight  = 1.0
  }]
  
  steering_policy    = "dynamic_latency"
  session_affinity   = "cookie"
  enable_failover    = true
  notification_email = "ops@example.com"
}
```

## 주요 입력 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| steering_policy | 라우팅 정책 | "dynamic_latency" |
| session_affinity | 세션 고정 | "cookie" |
| health_check_interval | 헬스체크 간격(초) | 60 |
| health_check_path | 헬스체크 경로 | "/health" |

## 출력 값

| 출력 | 설명 |
|------|------|
| load_balancer_id | 로드밸런서 ID |
| aws_pool_id | AWS 풀 ID |
| gcp_pool_id | GCP 풀 ID |

## 요구사항

- Terraform >= 1.5.0
- CloudFlare Provider >= 4.0
