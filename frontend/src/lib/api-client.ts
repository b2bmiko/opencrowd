import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const apiClient = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: inject auth token
apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const stored = localStorage.getItem('opencrowd_auth');
  if (stored) {
    const user = JSON.parse(stored);
    if (user.access_token) {
      config.headers.Authorization = `Bearer ${user.access_token}`;
    }
  }
  return config;
});

// Response interceptor: transform errors
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    if (error.response?.status === 401) {
      // Token expired or invalid — redirect to login
      localStorage.removeItem('opencrowd_auth');
      window.location.href = '/';
    }
    return Promise.reject(transformError(error));
  },
);

function transformError(error: AxiosError<ApiErrorResponse>): AppError {
  if (error.response?.data) {
    return {
      code: error.response.data.code || 'UNKNOWN_ERROR',
      message: error.response.data.message || 'An unexpected error occurred',
      status: error.response.status,
      details: error.response.data.details,
    };
  }
  return {
    code: 'NETWORK_ERROR',
    message: error.message || 'Network error',
    status: 0,
  };
}

// Types
export interface ApiErrorResponse {
  code: string;
  message: string;
  details?: { field: string; message: string }[];
  correlationId?: string;
  timestamp?: string;
}

export interface AppError {
  code: string;
  message: string;
  status: number;
  details?: { field: string; message: string }[];
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface ListResponse<T> {
  data: T[];
  count: number;
  timestamp: string;
}
