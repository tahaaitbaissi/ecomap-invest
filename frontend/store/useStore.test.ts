import { describe, expect, it } from "vitest";
import { useStore } from "./useStore";
import type { DynamicProfileResponse } from "@/services/api/profileService";

function mkProfile(id: string): DynamicProfileResponse {
  return {
    id,
    userId: "u",
    name: `P${id}`,
    userQuery: `Profile ${id}`,
    generatedAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    archivedAt: null,
    drivers: [],
    competitors: [],
  };
}

describe("useStore profile selection", () => {
  it("auto-selects first profile by default when profiles load", () => {
    useStore.setState({
      profileId: null,
      profileSelectionMode: "auto",
      commercialProfiles: [],
      selectedCommercialProfile: null,
    });

    useStore.getState().setCommercialProfiles([mkProfile("a"), mkProfile("b")]);
    expect(useStore.getState().profileId).toBe("a");
  });

  it("preserves explicit neutral selection across refreshes", () => {
    useStore.setState({
      profileId: null,
      profileSelectionMode: "manual",
      commercialProfiles: [],
      selectedCommercialProfile: null,
    });

    useStore.getState().setCommercialProfiles([mkProfile("a"), mkProfile("b")]);
    expect(useStore.getState().profileId).toBe(null);
  });
});

