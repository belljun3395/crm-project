import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // 100명의 가상 유저(VU)가 30초 동안 테스트를 수행합니다.
  vus: 100,
  duration: '30s',
};

// 이 스크립트는 TARGET_URL 환경 변수를 통해 테스트할 서버 주소를 받습니다.
const targetUrl = __ENV.TARGET_URL || 'http://localhost:8081/api/v1/events';

// 테스트용 사용자 external IDs (미리 데이터베이스에 존재해야 함)
const testUsers = ['test-user-1', 'test-user-2', 'test-user-3', 'test-user-4', 'test-user-5'];

export default function () {
  // 랜덤하게 사용자 선택
  const randomUser = testUsers[Math.floor(Math.random() * testUsers.length)];
  
  const payload = JSON.stringify({
    name: 'benchmark-event',
    campaignName: 'benchmark-campaign',
    externalId: randomUser,
    properties: [
      { key: 'action', value: 'test' },
      { key: 'timestamp', value: new Date().toISOString() },
      { key: 'value', value: String(Math.floor(Math.random() * 1000)) }
    ]
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(targetUrl, payload, params);

  check(res, {
    'is status 201': (r) => r.status === 201, // Created
    'is status 200': (r) => r.status === 200, // OK
    'has success response': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true || body.data !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  sleep(0.1); // 짧은 sleep으로 더 높은 부하 생성
}
