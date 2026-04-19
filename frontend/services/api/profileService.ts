import axios from "@/lib/axiosInstance";

export interface ProfileDriver {
  tag: string;
  weight: number;
}

export interface ProfileDto {
  id: string;
  userQuery: string;
  generatedAt: string;
  drivers: ProfileDriver[];
  competitors: ProfileDriver[];
}

export async function createProfile(userQuery: string): Promise<ProfileDto> {
  const res = await axios.post<ProfileDto>("/api/v1/profile", { userQuery });
  return res.data;
}

export async function getMyProfiles(): Promise<ProfileDto[]> {
  const res = await axios.get<ProfileDto[]>("/api/v1/profile/my");
  return res.data;
}
