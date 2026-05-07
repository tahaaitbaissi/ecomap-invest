import axios from "@/lib/axiosInstance";

export interface TagWeightDto {
  tag: string;
  weight: number;
}

export interface DynamicProfileResponse {
  id: string;
  userId: string;
  name: string;
  userQuery: string;
  generatedAt: string;
  updatedAt: string;
  archivedAt: string | null;
  drivers: TagWeightDto[];
  competitors: TagWeightDto[];
}

export interface ProfileTagOption {
  tag: string;
  label: string;
  group: string;
  description: string;
  aliases: string[];
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
  size = 100,
  includeArchived = false,
): Promise<PageDto<DynamicProfileResponse>> {
  const res = await axios.get<PageDto<DynamicProfileResponse>>("/api/v1/profile/my", {
    params: {
      page,
      size,
      sort: "generatedAt,desc",
      includeArchived,
    },
  });
  return res.data;
}

/** Convenience for UIs that only need the first page of rows. */
export async function fetchMyProfilesAsList(size = 100): Promise<DynamicProfileResponse[]> {
  const p = await fetchMyProfiles(0, size);
  return p.content;
}

export async function fetchProfileTags(): Promise<ProfileTagOption[]> {
  const res = await axios.get<ProfileTagOption[]>("/api/v1/profile/tags");
  return res.data;
}

export async function generateDynamicProfile(query: string): Promise<DynamicProfileResponse> {
  const res = await axios.post<DynamicProfileResponse>("/api/v1/profile/generate", { query });
  return res.data;
}

export interface UpdateDynamicProfileRequest {
  name?: string;
  drivers?: TagWeightDto[];
  competitors?: TagWeightDto[];
}

export async function updateDynamicProfile(
  id: string,
  payload: UpdateDynamicProfileRequest,
): Promise<DynamicProfileResponse> {
  const res = await axios.patch<DynamicProfileResponse>(`/api/v1/profile/${id}`, payload);
  return res.data;
}

export async function duplicateDynamicProfile(id: string): Promise<DynamicProfileResponse> {
  const res = await axios.post<DynamicProfileResponse>(`/api/v1/profile/${id}/duplicate`);
  return res.data;
}

export async function archiveDynamicProfile(id: string): Promise<void> {
  await axios.delete(`/api/v1/profile/${id}`);
}

export interface HexDemographicsSnapshot {
  usingDemographics: boolean | null;
  densityCapUsed: number | null;
  populationTermAverage: number | null;
}

export interface HexTagContributionRow {
  tag: string;
  weight: number;
  countInsideAcrossLeaves: number;
  weightedContributionAcrossLeaves: number;
}

export interface HexExplanationContextDto {
  profileId: string;
  profileName: string;
  profileUserQuery: string;
  h3Index: string;
  h3InputResolution: number;
  aggregatedFromGridLeaves: boolean;
  gridLeafResolution: number;
  gridLeafCount: number;
  aggregationInterpretationNote: string;
  centerLat: number;
  centerLng: number;
  averageRawAcrossLeaves: number;
  normalizationStretchLow: number;
  normalizationStretchHigh: number;
  normalizationFlat: boolean;
  computedDisplayScore: number | null;
  averagePopulationTerm: number;
  summedWeightedDriversAcrossLeaves: number;
  summedWeightedCompetitorsAcrossLeaves: number;
  drivers: HexTagContributionRow[];
  competitors: HexTagContributionRow[];
  demographics: HexDemographicsSnapshot | null;
  totalCompetitorPoisUnweightedAcrossLeaves: number;
  populationDensityAvg: number | null;
  avgIncomeAvg: number | null;
}

export async function fetchHexDetails(
  profileId: string,
  h3Index: string,
  viewportCellCount?: number,
): Promise<HexExplanationContextDto> {
  const res = await axios.get<HexExplanationContextDto>(
    `/api/v1/profile/${profileId}/hex-details`,
    {
      params: {
        h3Index,
        ...(viewportCellCount != null && viewportCellCount >= 1
          ? { viewportCellCount }
          : {}),
      },
    },
  );
  return res.data;
}
