import axios from "axios";
import { clearToken, getToken } from "@/lib/auth";

/**
 * Empty → same-origin `/api/...` so Next.js can proxy to Spring (no browser CORS).
 * Set NEXT_PUBLIC_API_URL only if you intentionally call the API host directly.
 */
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL?.trim() ?? "";

const instance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

instance.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

instance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      clearToken();
    }
    return Promise.reject(error);
  },
);

export default instance;
