import { clearToken, getToken } from "@/lib/auth";

export interface AuthResponse {
  token: string;
  type: string;
}

interface RawAuthResponse {
  token?: string;
  type?: string;
  accessToken?: string;
  tokenType?: string;
}

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  name: string;
  role: "USER" | "ADMIN";
}

export interface UpdateProfilePayload {
  username?: string;
  email?: string;
  name?: string;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  authenticated = false,
): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");

  if (authenticated) {
    const token = getToken();
    if (!token) {
      throw new Error("Missing authentication token");
    }
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const message = typeof data === "string" ? data : data?.message ?? "Request failed";
    if (response.status === 401) {
      clearToken();
    }
    throw new Error(message);
  }

  return data as T;
}

export function login(email: string, password: string): Promise<AuthResponse> {
  return apiRequest<RawAuthResponse>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  }).then(normalizeAuthResponse);
}

export function register(payload: {
  username: string;
  email: string;
  name: string;
  password: string;
}): Promise<AuthResponse> {
  return apiRequest<RawAuthResponse>("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify(payload),
  }).then(normalizeAuthResponse);
}

export function getMyProfile(): Promise<UserProfile> {
  return apiRequest<UserProfile>("/api/v1/users/me", {}, true);
}

export function updateMyProfile(payload: UpdateProfilePayload): Promise<UserProfile> {
  return apiRequest<UserProfile>("/api/v1/users/me", {
    method: "PUT",
    body: JSON.stringify(payload),
  }, true);
}

function normalizeAuthResponse(raw: RawAuthResponse): AuthResponse {
  const token = raw.token ?? raw.accessToken;
  const type = raw.type ?? raw.tokenType ?? "Bearer";

  if (!token) {
    throw new Error("Authentication token is missing in server response");
  }

  return { token, type };
}
