import axios from 'axios';

// 환경변수에서 API URL 가져오기, 없으면 상대 경로 사용
const getBaseURL = (): string => {
  // 개발 환경에서는 환경변수 사용
  if (process.env.REACT_APP_API_BASE_URL) {
    return process.env.REACT_APP_API_BASE_URL;
  }

  // 프로덕션 환경에서는 상대 경로 사용
  if (process.env.NODE_ENV === 'production') {
    return '/api/v1';
  }

  // 기본값 (로컬 개발)
  return 'http://localhost:8080/api/v1';
};

export const crmApi = axios.create({
  baseURL: getBaseURL(),
  timeout: 10000,
});

// Request interceptor
crmApi.interceptors.request.use(
  (config) => {
    // Add auth headers if needed
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
crmApi.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // Handle common errors
    console.error('API Error:', error);
    return Promise.reject(error);
  }
);