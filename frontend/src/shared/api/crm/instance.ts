import axios from 'axios';

export const crmApi = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
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