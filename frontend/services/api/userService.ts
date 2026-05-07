import axios from "@/lib/axiosInstance";

export interface UserProfileDTO {
  id: string;
  email: string;
  companyName: string | null;
  role: string;
  createdAt: string;
}

export async function fetchMe(): Promise<UserProfileDTO> {
  const res = await axios.get<UserProfileDTO>("/api/v1/users/me");
  return res.data;
}

