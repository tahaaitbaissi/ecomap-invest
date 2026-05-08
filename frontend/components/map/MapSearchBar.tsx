"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import axios from "axios";
import { latLngToCell } from "h3-js";
import { searchPlaces, type GeocodingSuggestion } from "@/services/api/geocodingService";
import { getHexagonByIndex, type HexagonDto } from "@/services/api/hexagonService";
import { searchPois, type PoiSearchResult } from "@/services/api/poiService";
import { useStore } from "@/store/useStore";

const RECENT_KEY = "ecomap.search.recent";
const DEBOUNCE_MS = 250;
const H3_INPUT = /^[0-9a-f]{15,16}$/i;

function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(t);
  }, [value, delayMs]);
  return debounced;
}

type RecentV1 =
  | {
      v: 1;
      kind: "place";
      displayName: string;
      lat: number;
      lng: number;
      bbox?: [number, number, number, number];
    }
  | {
      v: 1;
      kind: "poi";
      id: string;
      displayName: string;
      lat: number;
      lng: number;
      typeTag: string;
      address: string;
    }
  | { v: 1; kind: "hex"; h3Index: string; displayName: string };

function loadRecent(): RecentV1[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(RECENT_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((x) => x && typeof x === "object" && (x as RecentV1).v === 1) as RecentV1[];
  } catch {
    return [];
  }
}

function saveRecent(items: RecentV1[]) {
  try {
    localStorage.setItem(RECENT_KEY, JSON.stringify(items.slice(0, 5)));
  } catch {
    /* ignore */
  }
}

function pushRecent(entry: RecentV1) {
  const cur = loadRecent().filter(
    (e) =>
      !(
        e.kind === entry.kind &&
        (entry.kind === "hex"
          ? e.kind === "hex" && e.h3Index === entry.h3Index
          : entry.kind === "poi"
            ? e.kind === "poi" && e.id === entry.id
            : e.kind === "place" &&
              e.displayName === entry.displayName &&
              e.lat === entry.lat &&
              e.lng === entry.lng)
      ),
  );
  saveRecent([entry, ...cur].slice(0, 5));
}

function centroid(boundary: { lat: number; lng: number }[]): { lat: number; lng: number } {
  if (boundary.length === 0) return { lat: 0, lng: 0 };
  let sLat = 0;
  let sLng = 0;
  for (const p of boundary) {
    sLat += p.lat;
    sLng += p.lng;
  }
  return { lat: sLat / boundary.length, lng: sLng / boundary.length };
}

function isIgnorableAbort(e: unknown): boolean {
  if (axios.isCancel(e)) return true;
  if (e instanceof DOMException && e.name === "AbortError") return true;
  if (axios.isAxiosError(e) && (e.code === "ERR_CANCELED" || e.name === "CanceledError")) return true;
  return false;
}

type FlatRow =
  | { key: string; section: "recent"; recent: RecentV1 }
  | { key: string; section: "lieux"; place: GeocodingSuggestion }
  | { key: string; section: "poi"; poi: PoiSearchResult }
  | { key: string; section: "hex"; hex: HexagonDto };

export default function MapSearchBar() {
  const setMapFlyTo = useStore((s) => s.setMapFlyTo);
  const setSearchPin = useStore((s) => s.setSearchPin);
  const setSelectedHexIndex = useStore((s) => s.setSelectedHexIndex);
  const setSearchHighlightHex = useStore((s) => s.setSearchHighlightHex);

  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const [places, setPlaces] = useState<GeocodingSuggestion[]>([]);
  const [pois, setPois] = useState<PoiSearchResult[]>([]);
  const [hexHit, setHexHit] = useState<HexagonDto | null>(null);
  const [activeIdx, setActiveIdx] = useState(0);
  const [recent, setRecent] = useState<RecentV1[]>([]);

  const debouncedQuery = useDebouncedValue(query, DEBOUNCE_MS);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const trimmed = useMemo(() => debouncedQuery.trim(), [debouncedQuery]);

  const refreshRecent = useCallback(() => setRecent(loadRecent()), []);

  useEffect(() => {
    refreshRecent();
  }, [refreshRecent]);

  useEffect(() => {
    const ac = new AbortController();
    if (trimmed.length < 2) {
      setLoading(false);
      setPlaces([]);
      setPois([]);
      setHexHit(null);
      setError(false);
      setActiveIdx(0);
      return () => ac.abort();
    }

    setLoading(true);
    setError(false);

    const isH3 = H3_INPUT.test(trimmed);
    const tasks: Promise<unknown>[] = [
      searchPlaces(trimmed, { limit: 8, signal: ac.signal }),
      searchPois(trimmed, { limit: 10, signal: ac.signal }),
    ];
    if (isH3) {
      tasks.push(getHexagonByIndex(trimmed.toLowerCase(), ac.signal));
    }

    void Promise.all(tasks)
      .then((results) => {
        const p0 = results[0] as GeocodingSuggestion[];
        const p1 = results[1] as PoiSearchResult[];
        setPlaces(p0);
        setPois(p1);
        if (isH3) {
          setHexHit(results[2] as HexagonDto);
        } else {
          setHexHit(null);
        }
        setActiveIdx(0);
      })
      .catch((e) => {
        if (isIgnorableAbort(e)) return;
        setError(true);
        setPlaces([]);
        setPois([]);
        setHexHit(null);
      })
      .finally(() => {
        if (!ac.signal.aborted) setLoading(false);
      });

    return () => ac.abort();
  }, [trimmed]);

  const flatRows: FlatRow[] = useMemo(() => {
    const rows: FlatRow[] = [];
    const qEmpty = query.trim().length === 0;
    if (open && qEmpty && recent.length) {
      for (const r of recent) {
        const key =
          r.kind === "hex"
            ? `recent-hex-${r.h3Index}`
            : r.kind === "poi"
              ? `recent-poi-${r.id}`
              : `recent-place-${r.displayName}-${r.lat}-${r.lng}`;
        rows.push({ key, section: "recent", recent: r });
      }
      return rows;
    }

    for (const p of places) {
      rows.push({
        key: `place-${p.displayName}-${p.lat}-${p.lng}`,
        section: "lieux",
        place: p,
      });
    }
    for (const p of pois) {
      rows.push({ key: `poi-${p.id}`, section: "poi", poi: p });
    }
    if (hexHit && H3_INPUT.test(trimmed)) {
      rows.push({ key: `hex-${hexHit.h3Index}`, section: "hex", hex: hexHit });
    }
    return rows;
  }, [open, query, recent, places, pois, hexHit, trimmed]);

  useEffect(() => {
    if (activeIdx >= flatRows.length) {
      setActiveIdx(Math.max(0, flatRows.length - 1));
    }
  }, [flatRows.length, activeIdx]);

  useEffect(() => {
    const onDocMouseDown = (e: MouseEvent) => {
      const el = containerRef.current;
      if (!el) return;
      if (!el.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDocMouseDown);
    return () => document.removeEventListener("mousedown", onDocMouseDown);
  }, []);

  const commitPlace = useCallback(
    (p: GeocodingSuggestion) => {
      const bbox: [number, number, number, number] | undefined =
        p.southLat != null && p.westLng != null && p.northLat != null && p.eastLng != null
          ? [p.southLat, p.westLng, p.northLat, p.eastLng]
          : undefined;
      setMapFlyTo({
        lat: p.lat,
        lng: p.lng,
        label: p.displayName,
        bbox,
        targetZoom: bbox ? undefined : 16,
      });
      setSearchPin({ lat: p.lat, lng: p.lng, label: p.displayName });
      setSearchHighlightHex(null);
      pushRecent({
        v: 1,
        kind: "place",
        displayName: p.displayName,
        lat: p.lat,
        lng: p.lng,
        bbox,
      });
      refreshRecent();
    },
    [refreshRecent, setMapFlyTo, setSearchHighlightHex, setSearchPin],
  );

  const commitPoi = useCallback(
    async (p: PoiSearchResult) => {
      const { lat, lng } = p;
      setMapFlyTo({ lat, lng, label: p.name, targetZoom: 17 });
      setSearchPin({ lat, lng, label: p.name });
      try {
        const cell = latLngToCell(lat, lng, 9);
        const hex = await getHexagonByIndex(cell);
        setSearchHighlightHex(hex);
        setSelectedHexIndex(cell);
      } catch {
        try {
          const cell = latLngToCell(lat, lng, 9);
          setSelectedHexIndex(cell);
        } catch {
          setSelectedHexIndex(null);
        }
        setSearchHighlightHex(null);
      }
      pushRecent({
        v: 1,
        kind: "poi",
        id: p.id,
        displayName: p.name,
        lat,
        lng,
        typeTag: p.typeTag,
        address: p.address,
      });
      refreshRecent();
    },
    [refreshRecent, setMapFlyTo, setSearchHighlightHex, setSearchPin, setSelectedHexIndex],
  );

  const commitHex = useCallback(
    (h: HexagonDto) => {
      const c = centroid(h.boundary);
      setMapFlyTo({ lat: c.lat, lng: c.lng, label: `H3 ${h.h3Index}`, targetZoom: 16 });
      setSearchPin({ lat: c.lat, lng: c.lng, label: `H3 ${h.h3Index}` });
      setSearchHighlightHex(h);
      setSelectedHexIndex(h.h3Index);
      pushRecent({ v: 1, kind: "hex", h3Index: h.h3Index, displayName: `H3 ${h.h3Index}` });
      refreshRecent();
    },
    [refreshRecent, setMapFlyTo, setSearchHighlightHex, setSearchPin, setSelectedHexIndex],
  );

  const commitRecent = useCallback(
    (r: RecentV1) => {
      if (r.kind === "place") {
        setQuery(r.displayName);
        setOpen(false);
        const bbox = r.bbox;
        setMapFlyTo({
          lat: r.lat,
          lng: r.lng,
          label: r.displayName,
          bbox,
          targetZoom: bbox ? undefined : 16,
        });
        setSearchPin({ lat: r.lat, lng: r.lng, label: r.displayName });
        setSearchHighlightHex(null);
        return;
      }
      if (r.kind === "poi") {
        setQuery(r.displayName);
        setOpen(false);
        void commitPoi({
          id: r.id,
          name: r.displayName,
          typeTag: r.typeTag,
          lat: r.lat,
          lng: r.lng,
          address: r.address,
        });
        return;
      }
      setQuery(r.h3Index);
      setOpen(false);
      void getHexagonByIndex(r.h3Index)
        .then((h) => commitHex(h))
        .catch(() => setError(true));
    },
    [commitHex, commitPoi, setMapFlyTo, setSearchHighlightHex, setSearchPin],
  );

  const commitRow = useCallback(
    (row: FlatRow) => {
      if (row.section === "recent") {
        commitRecent(row.recent);
        return;
      }
      if (row.section === "lieux") {
        setQuery(row.place.displayName);
        setOpen(false);
        commitPlace(row.place);
        return;
      }
      if (row.section === "poi") {
        setQuery(row.poi.name);
        setOpen(false);
        void commitPoi(row.poi);
        return;
      }
      setQuery(row.hex.h3Index);
      setOpen(false);
      commitHex(row.hex);
    },
    [commitHex, commitPlace, commitPoi, commitRecent],
  );

  const showEmptyState =
    trimmed.length >= 2 &&
    !loading &&
    !error &&
    places.length === 0 &&
    pois.length === 0 &&
    !(hexHit && H3_INPUT.test(trimmed));

  const showDropdown = open && (loading || error || flatRows.length > 0 || showEmptyState);

  const sectionTitle = (section: FlatRow["section"]) => {
    switch (section) {
      case "recent":
        return "Recherches récentes";
      case "lieux":
        return "Lieux";
      case "poi":
        return "POI";
      case "hex":
        return "Hexagone";
      default:
        return "";
    }
  };

  return (
    <div ref={containerRef} className="relative w-full">
      <div className="flex items-center gap-2 rounded-2xl border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.04)] px-3 py-2 shadow-sm backdrop-blur-md">
        <SearchIcon />
        <input
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
          onFocus={() => {
            setOpen(true);
            refreshRecent();
          }}
          onKeyDown={(e) => {
            if (e.key === "Escape") {
              setOpen(false);
              return;
            }
            if (!flatRows.length) return;
            if (e.key === "ArrowDown") {
              e.preventDefault();
              setActiveIdx((i) => Math.min(i + 1, flatRows.length - 1));
              return;
            }
            if (e.key === "ArrowUp") {
              e.preventDefault();
              setActiveIdx((i) => Math.max(i - 1, 0));
              return;
            }
            if (e.key === "Enter") {
              const row = flatRows[activeIdx];
              if (row) {
                e.preventDefault();
                commitRow(row);
              }
            }
          }}
          placeholder="Lieux, POI ou index H3…"
          className="min-w-0 flex-1 bg-transparent text-sm text-[color:var(--color-text-primary)] placeholder:text-[color:var(--color-text-muted)] focus:outline-none"
          aria-label="Recherche carte"
          aria-expanded={open}
          aria-controls="map-search-suggestions"
        />
        {query.trim().length > 0 ? (
          <button
            type="button"
            onClick={() => {
              setQuery("");
              setPlaces([]);
              setPois([]);
              setHexHit(null);
              setActiveIdx(0);
              setOpen(false);
              setError(false);
            }}
            className="rounded-lg p-1 text-[color:var(--color-text-secondary)] transition hover:bg-[color:rgba(234,240,255,0.06)] hover:text-[color:var(--color-text-primary)]"
            aria-label="Effacer"
          >
            <XIcon />
          </button>
        ) : null}
      </div>

      {showDropdown && (
        <div
          id="map-search-suggestions"
          className="absolute left-0 right-0 top-[calc(100%+8px)] z-[var(--app-shell-z-popover)] max-h-[min(70vh,420px)] overflow-auto rounded-2xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] shadow-xl"
          role="listbox"
          aria-label="Suggestions"
        >
          {error && (
            <div className="border-b border-[color:var(--color-border)] bg-amber-500/10 px-3 py-2 text-xs text-amber-200">
              Recherche indisponible — réessayez
            </div>
          )}

          {loading && (
            <div className="flex items-center gap-2 px-3 py-2 text-xs text-[color:var(--color-text-secondary)]">
              <span className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-[color:var(--color-border)] border-t-[color:var(--color-accent)]" />
              Recherche…
            </div>
          )}

          {!loading && showEmptyState && (
            <div className="px-3 py-2 text-xs text-[color:var(--color-text-secondary)]">Aucun résultat.</div>
          )}

          {flatRows.map((row, idx) => {
            const prev = idx > 0 ? flatRows[idx - 1] : null;
            const showHeader = !prev || prev.section !== row.section;
            const active = idx === activeIdx;
            if (row.section === "recent") {
              const label =
                row.recent.kind === "hex"
                  ? row.recent.displayName
                  : row.recent.kind === "poi"
                    ? row.recent.displayName
                    : row.recent.displayName;
              return (
                <div key={row.key}>
                  {showHeader ? (
                    <div className="px-2 py-1.5 text-[10px] font-bold uppercase tracking-wide text-[color:var(--color-text-muted)]">
                      {sectionTitle(row.section)}
                    </div>
                  ) : null}
                  <button
                    type="button"
                    role="option"
                    aria-selected={active}
                    onMouseEnter={() => setActiveIdx(idx)}
                    onClick={() => commitRow(row)}
                    className={rowButtonClass(active)}
                  >
                    <ClockIcon />
                    <span className="line-clamp-2 text-[color:var(--color-text-primary)]">{label}</span>
                  </button>
                </div>
              );
            }
            if (row.section === "lieux") {
              return (
                <div key={row.key}>
                  {showHeader ? (
                    <div className="px-2 py-1.5 text-[10px] font-bold uppercase tracking-wide text-[color:var(--color-text-muted)]">
                      {sectionTitle(row.section)}
                    </div>
                  ) : null}
                  <button
                    type="button"
                    role="option"
                    aria-selected={active}
                    onMouseEnter={() => setActiveIdx(idx)}
                    onClick={() => commitRow(row)}
                    className={rowButtonClass(active)}
                  >
                    <PinIcon />
                    <span className="line-clamp-2 text-[color:var(--color-text-primary)]">{row.place.displayName}</span>
                  </button>
                </div>
              );
            }
            if (row.section === "poi") {
              return (
                <div key={row.key}>
                  {showHeader ? (
                    <div className="px-2 py-1.5 text-[10px] font-bold uppercase tracking-wide text-[color:var(--color-text-muted)]">
                      {sectionTitle(row.section)}
                    </div>
                  ) : null}
                  <button
                    type="button"
                    role="option"
                    aria-selected={active}
                    onMouseEnter={() => setActiveIdx(idx)}
                    onClick={() => commitRow(row)}
                    className={rowButtonClass(active)}
                  >
                    <ShopIcon />
                    <span className="flex min-w-0 flex-col text-left">
                      <span className="line-clamp-1 text-[color:var(--color-text-primary)]">{row.poi.name}</span>
                      <span className="line-clamp-1 text-[10px] text-[color:var(--color-text-muted)]">
                        {row.poi.typeTag}
                      </span>
                    </span>
                  </button>
                </div>
              );
            }
            return (
              <div key={row.key}>
                {showHeader ? (
                  <div className="px-2 py-1.5 text-[10px] font-bold uppercase tracking-wide text-[color:var(--color-text-muted)]">
                    {sectionTitle(row.section)}
                  </div>
                ) : null}
                <button
                  type="button"
                  role="option"
                  aria-selected={active}
                  onMouseEnter={() => setActiveIdx(idx)}
                  onClick={() => commitRow(row)}
                  className={rowButtonClass(active)}
                >
                  <HexIcon />
                  <span className="line-clamp-2 font-mono text-xs text-[color:var(--color-text-primary)]">
                    {row.hex.h3Index}
                  </span>
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function rowButtonClass(active: boolean) {
  return [
    "flex w-full items-start gap-2 px-3 py-2 text-left text-sm",
    active ? "bg-[color:rgba(234,240,255,0.04)]" : "bg-transparent",
    "hover:bg-[color:rgba(234,240,255,0.04)]",
  ].join(" ");
}

function SearchIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="shrink-0 text-[color:var(--color-text-secondary)]">
      <path
        d="M21 21l-4.3-4.3m1.8-5.2a7 7 0 11-14 0 7 7 0 0114 0z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function PinIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="mt-0.5 shrink-0 text-[color:var(--color-text-muted)]">
      <path
        d="M12 22s7-4.4 7-11a7 7 0 10-14 0c0 6.6 7 11 7 11z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="12" cy="11" r="2.5" stroke="currentColor" strokeWidth="2" />
    </svg>
  );
}

function ShopIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="mt-0.5 shrink-0 text-[color:var(--color-text-muted)]">
      <path d="M3 9h18v10a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" stroke="currentColor" strokeWidth="2" />
      <path d="M3 9l2-5h14l2 5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

function HexIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="mt-0.5 shrink-0 text-[color:var(--color-text-muted)]">
      <path
        d="M12 2l8 4.5v9L12 20l-8-4.5v-9L12 2z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function ClockIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="mt-0.5 shrink-0 text-[color:var(--color-text-muted)]">
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2" />
      <path d="M12 7v6l4 2" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

function XIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
      <path
        d="M18 6L6 18M6 6l12 12"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
