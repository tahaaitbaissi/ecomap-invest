import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import MapSearchBar from "./MapSearchBar";
import { searchPlaces } from "@/services/api/geocodingService";
import { getHexagonByIndex } from "@/services/api/hexagonService";
import { searchPois } from "@/services/api/poiService";

const mockSetMapFlyTo = vi.fn();
const mockSetSearchPin = vi.fn();
const mockSetSelectedHexIndex = vi.fn();
const mockSetSearchHighlightHex = vi.fn();

vi.mock("@/store/useStore", () => ({
  useStore: (sel: (s: Record<string, unknown>) => unknown) =>
    sel({
      setMapFlyTo: mockSetMapFlyTo,
      setSearchPin: mockSetSearchPin,
      setSelectedHexIndex: mockSetSelectedHexIndex,
      setSearchHighlightHex: mockSetSearchHighlightHex,
    }),
}));

vi.mock("h3-js", () => ({
  latLngToCell: vi.fn(() => "8939aab940fffff"),
}));

vi.mock("@/services/api/geocodingService", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/services/api/geocodingService")>();
  return { ...actual, searchPlaces: vi.fn() };
});

vi.mock("@/services/api/poiService", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/services/api/poiService")>();
  return { ...actual, searchPois: vi.fn() };
});

vi.mock("@/services/api/hexagonService", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/services/api/hexagonService")>();
  return { ...actual, getHexagonByIndex: vi.fn() };
});

describe("MapSearchBar", () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    localStorage.clear();
    mockSetMapFlyTo.mockClear();
    mockSetSearchPin.mockClear();
    mockSetSelectedHexIndex.mockClear();
    mockSetSearchHighlightHex.mockClear();
    vi.mocked(searchPlaces).mockResolvedValue([]);
    vi.mocked(searchPois).mockResolvedValue([]);
    vi.mocked(getHexagonByIndex).mockResolvedValue({
      h3Index: "8939aab940fffff",
      score: null,
      boundary: [
        { lat: 33.5, lng: -7.6 },
        { lat: 33.51, lng: -7.6 },
      ],
    });
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it("debounces queries (250ms) before calling searchPlaces/searchPois", async () => {
    render(<MapSearchBar />);
    const input = screen.getByLabelText("Recherche carte");
    fireEvent.change(input, { target: { value: "Ma" } });
    fireEvent.change(input, { target: { value: "Mar" } });
    expect(searchPlaces).not.toHaveBeenCalled();
    await act(async () => {
      vi.advanceTimersByTime(250);
    });
    await waitFor(() => {
      expect(searchPlaces).toHaveBeenCalled();
    });
    expect(searchPlaces).toHaveBeenCalledWith("Mar", expect.any(Object));
  });

  it("renders grouped Lieux and POI sections", async () => {
    vi.mocked(searchPlaces).mockResolvedValue([{ displayName: "Place A", lat: 1, lng: 2 }]);
    vi.mocked(searchPois).mockResolvedValue([
      { id: "1", name: "Shop B", typeTag: "shop=x", lat: 3, lng: 4, address: "" },
    ]);
    render(<MapSearchBar />);
    const input = screen.getByLabelText("Recherche carte");
    fireEvent.focus(input);
    fireEvent.change(input, { target: { value: "xy" } });
    await act(async () => {
      vi.advanceTimersByTime(250);
    });
    await waitFor(() => {
      expect(screen.getByText("Lieux")).toBeTruthy();
      expect(screen.getByText("POI")).toBeTruthy();
    });
  });

  it("shows error banner when search rejects", async () => {
    vi.mocked(searchPlaces).mockRejectedValue(new Error("network"));
    render(<MapSearchBar />);
    const input = screen.getByLabelText("Recherche carte");
    fireEvent.change(input, { target: { value: "ab" } });
    await act(async () => {
      vi.advanceTimersByTime(250);
    });
    await waitFor(() => {
      expect(screen.getByText(/Recherche indisponible/)).toBeTruthy();
    });
  });

  it("persists and shows recent searches", async () => {
    localStorage.setItem(
      "ecomap.search.recent",
      JSON.stringify([
        {
          v: 1,
          kind: "place",
          displayName: "Saved",
          lat: 10,
          lng: 20,
        },
      ]),
    );
    render(<MapSearchBar />);
    const input = screen.getByLabelText("Recherche carte");
    fireEvent.focus(input);
    expect(await screen.findByText("Recherches récentes")).toBeTruthy();
    expect(screen.getByText("Saved")).toBeTruthy();
  });

  it("commits place selection to the store", async () => {
    vi.mocked(searchPlaces).mockResolvedValue([
      {
        displayName: "P",
        lat: 33.1,
        lng: -7.2,
        southLat: 33,
        westLng: -7.3,
        northLat: 33.2,
        eastLng: -7.1,
      },
    ]);
    render(<MapSearchBar />);
    const input = screen.getByLabelText("Recherche carte");
    fireEvent.change(input, { target: { value: "ab" } });
    await act(async () => {
      vi.advanceTimersByTime(250);
    });
    await screen.findByText("P");
    fireEvent.click(screen.getByText("P"));
    expect(mockSetMapFlyTo).toHaveBeenCalledWith(
      expect.objectContaining({
        lat: 33.1,
        lng: -7.2,
        label: "P",
        bbox: [33, -7.3, 33.2, -7.1],
      }),
    );
    expect(mockSetSearchPin).toHaveBeenCalledWith(
      expect.objectContaining({ lat: 33.1, lng: -7.2, label: "P" }),
    );
  });

  it("selects highlighted row on Enter", async () => {
    vi.mocked(searchPlaces).mockResolvedValue([{ displayName: "Only", lat: 1, lng: 2 }]);
    render(<MapSearchBar />);
    const input = screen.getByLabelText("Recherche carte");
    fireEvent.change(input, { target: { value: "ab" } });
    await act(async () => {
      vi.advanceTimersByTime(250);
    });
    await screen.findByText("Only");
    fireEvent.keyDown(input, { key: "Enter" });
    expect(mockSetMapFlyTo).toHaveBeenCalled();
  });
});
