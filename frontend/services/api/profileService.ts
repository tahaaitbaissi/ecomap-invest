import axios from "@/lib/axiosInstance";

export interface TagWeightDto {
  tag: string;
  weight: number;
}

export interface DynamicProfileResponse {
  id: string;
  userId: string;
  userQuery: string;
  generatedAt: string;
  drivers: TagWeightDto[];
  competitors: TagWeightDto[];
}

/** Spring Data `Page<DynamicProfileResponse>` JSON. */
export interface PageDto<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
}

/**
 * Paged list of the current user’s dynamic profiles (newest first by default).
 */
export async function fetchMyProfiles(
  page = 0,
  size = 100
): Promise<PageDto<DynamicProfileResponse>> {
  const res = await axios.get<PageDto<DynamicProfileResponse>>(
    "/api/v1/profile/my",
    {
      params: {
        page,
        size,
        sort: "generatedAt,desc",
      },
    }
  );
  return res.data;
}

/** Convenience for UIs that only need the first page of rows. */
export async function fetchMyProfilesAsList(
  size = 100
): Promise<DynamicProfileResponse[]> {
  const p = await fetchMyProfiles(0, size);
  return p.content;
}
