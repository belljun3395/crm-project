// axios 인스턴스 및 공통 설정 파일
// 여러 도메인에서 공통으로 사용할 axios 인스턴스를 정의합니다.
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  // 필요시 공통 헤더, 인터셉터 등 추가
});

export default api;
