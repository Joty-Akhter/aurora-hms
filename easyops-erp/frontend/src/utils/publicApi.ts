import axios, { AxiosInstance } from 'axios';
import { API_GATEWAY_BASE_URL } from '../services/api';

/** Axios client for unauthenticated public endpoints (no token, no login redirect). */
export function createPublicApiClient(): AxiosInstance {
  return axios.create({
    baseURL: API_GATEWAY_BASE_URL,
    headers: { 'Content-Type': 'application/json' },
  });
}
