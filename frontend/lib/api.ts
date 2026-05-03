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

/** Matches backend {@code UserProfileDTO} */
export interface UserProfile {
  id: string;
  email: string;
  companyName: string | null;
  role: string;
  createdAt?: string;
}

export interface UpdateProfilePayload {
  companyName?: string;
  currentPassword?: string;
  newPassword?: string;
}

/** Same-origin proxy by default (see next.config rewrites). */
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL?.trim() ?? "";

function extractErrorMessage(data: unknown, fallback: string): string {
  if (typeof data === "string") {
    return data || fallback;
  }
  if (!data || typeof data !== "object") {
    return fallback;
  }
  const o = data as Record<string, unknown>;
  if (typeof o.message === "string" && o.message.trim()) {
    return o.message;
  }
  if (typeof o.error === "string" && o.error.trim()) {
    return o.error;
  }
  return fallback;
}

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
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text };
    }
  }

  if (!response.ok) {
    const message = extractErrorMessage(data, "Request failed");
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

/** Backend {@link RegisterRequest}: email, password, companyName */
export function register(payload: {
  email: string;
  password: string;
  companyName: string;
}): Promise<AuthResponse> {
  return apiRequest<RawAuthResponse>("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify({
      email: payload.email,
      password: payload.password,
      companyName: payload.companyName,
    }),
  }).then(normalizeAuthResponse);
}

export function getMyProfile(): Promise<UserProfile> {
  return apiRequest<UserProfile>("/api/v1/users/me", {}, true);
}

export function updateMyProfile(payload: UpdateProfilePayload): Promise<UserProfile> {
  return apiRequest<UserProfile>(
    "/api/v1/users/me",
    {
      method: "PUT",
      body: JSON.stringify(payload),
    },
    true,
  );
}

function normalizeAuthResponse(raw: RawAuthResponse): AuthResponse {
  const token = raw.token ?? raw.accessToken;
  const type = raw.type ?? raw.tokenType ?? "Bearer";

  if (!token) {
    throw new Error("Authentication token is missing in server response");
  }

  return { token, type };
}
