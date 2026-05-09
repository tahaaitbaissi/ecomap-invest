import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import ActiveProfileWidget from "./ActiveProfileWidget";

vi.mock("@/services/api/profileService", () => ({
  fetchMyProfilesAsList: vi.fn().mockResolvedValue([]),
}));

const mockSetProfileId = vi.fn();
vi.mock("@/store/useStore", () => ({
  useStore: (sel: (s: Record<string, unknown>) => unknown) =>
    sel({
      commercialProfiles: [
        {
          id: "p1",
          userId: "u",
          name: "My profile",
          userQuery: "Coffee near schools",
          generatedAt: "",
          updatedAt: "",
          archivedAt: null,
          drivers: [],
          competitors: [],
        },
      ],
      selectedCommercialProfile: null,
      profileId: null,
      setProfileId: mockSetProfileId,
      setCommercialProfiles: vi.fn(),
    }),
}));

describe("ActiveProfileWidget", () => {
  it("renders neutral option and can select it", () => {
    render(<ActiveProfileWidget />);
    const select = screen.getByRole("combobox");
    expect(screen.getByText("Aucun profil (neutre)")).toBeTruthy();
    fireEvent.change(select, { target: { value: "" } });
    expect(mockSetProfileId).toHaveBeenCalledWith(null);
  });
});

