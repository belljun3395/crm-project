# AWS-GCP VPN 연결 모듈

AWS와 GCP 간 고가용성 VPN 연결을 구성하는 Terraform 모듈입니다.

## 기능

- ✅ 이중화된 VPN 터널 (HA)
- ✅ BGP 동적 라우팅
- ✅ IPsec 암호화
- ✅ 자동 라우트 전파

## 사용 예시

```hcl
module "vpn" {
  source = "../../modules/aws-gcp-vpn"

  name_prefix                 = "crm-dr"
  aws_vpc_id                  = module.networking.vpc_id
  aws_private_route_table_ids = module.networking.private_route_table_ids
  gcp_network_id              = "projects/my-project/global/networks/my-vpc"
  gcp_region                  = "asia-northeast3"
  
  tunnel1_preshared_key = var.vpn_key1
  tunnel2_preshared_key = var.vpn_key2
  
  advertised_ip_ranges = ["10.20.0.0/20"]
}
```

## 입력 변수

| 변수 | 설명 | 타입 | 기본값 | 필수 |
|------|------|------|--------|------|
| name_prefix | 리소스 이름 접두사 | string | - | yes |
| aws_vpc_id | AWS VPC ID | string | - | yes |
| gcp_network_id | GCP 네트워크 ID | string | - | yes |
| gcp_region | GCP 리전 | string | - | yes |
| tunnel1_preshared_key | 터널 1 사전 공유 키 | string | - | yes |
| tunnel2_preshared_key | 터널 2 사전 공유 키 | string | - | yes |

## 출력 값

| 출력 | 설명 |
|------|------|
| aws_vpn_gateway_id | AWS VPN Gateway ID |
| gcp_vpn_gateway_id | GCP HA VPN Gateway ID |
| tunnel_1_status | 터널 1 상태 |
| tunnel_2_status | 터널 2 상태 |

## 요구사항

- Terraform >= 1.5.0
- AWS Provider >= 5.0
- Google Provider >= 5.0
