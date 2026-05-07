"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { searchAddress, type GeocodingResult } from "@/services/api/geocodingService";
import { useStore } from "@/store/useStore";

function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(t);
  }, [value, delayMs]);
  return debounced;
}

export default function MapSearchBar() {
  const setMapFlyTo = useStore((s) => s.setMapFlyTo);

  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<GeocodingResult[]>([]);
  const [activeIdx, setActiveIdx] = useState(0);

  const debouncedQuery = useDebouncedValue(query, 220);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const trimmed = useMemo(() => debouncedQuery.trim(), [debouncedQuery]);

  useEffect(() => {
    let cancelled = false;
    if (trimmed.length < 3) {
      setLoading(false);
      setResults([]);
      setActiveIdx(0);
      return;
    }
    setLoading(true);
    void searchAddress(trimmed)
      .then((r) => {
        if (cancelled) return;
        setResults(r);
        setActiveIdx(0);
      })
      .finally(() => {
        if (cancelled) return;
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [trimmed]);

  useEffect(() => {
    const onDocMouseDown = (e: MouseEvent) => {
      const el = containerRef.current;
      if (!el) return;
      if (!el.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDocMouseDown);
    return () => document.removeEventListener("mousedown", onDocMouseDown);
  }, []);

  const commit = (r: GeocodingResult) => {
    setQuery(r.displayName);
    setOpen(false);
    setResults([]);
    setActiveIdx(0);
    setMapFlyTo({ lat: r.lat, lng: r.lng, label: r.displayName });
  };

  const hasResults = results.length > 0;
  const showDropdown = open && (loading || hasResults || trimmed.length >= 3);

  return (
    <div ref={containerRef} className="relative">
      <div className="flex items-center gap-2 rounded-2xl border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.04)] px-3 py-2 shadow-sm backdrop-blur-md">
        <SearchIcon />
        <input
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={(e) => {
            if (e.key === "Escape") {
              setOpen(false);
              return;
            }
            if (e.key === "ArrowDown") {
              e.preventDefault();
              setActiveIdx((i) => Math.min(i + 1, Math.max(0, results.length - 1)));
              return;
            }
            if (e.key === "ArrowUp") {
              e.preventDefault();
              setActiveIdx((i) => Math.max(i - 1, 0));
              return;
            }
            if (e.key === "Enter") {
              if (results[activeIdx]) {
                e.preventDefault();
                commit(results[activeIdx]);
              }
            }
          }}
          placeholder="Search address… (e.g. Casablanca, Maarif, Anfa)"
          className="min-w-0 flex-1 bg-transparent text-sm text-[color:var(--color-text-primary)] placeholder:text-[color:var(--color-text-muted)] focus:outline-none"
          aria-label="Search address"
        />
        {query.trim().length > 0 ? (
          <button
            type="button"
            onClick={() => {
              setQuery("");
              setResults([]);
              setActiveIdx(0);
              setOpen(false);
            }}
            className="rounded-lg p-1 text-[color:var(--color-text-secondary)] transition hover:bg-[color:rgba(234,240,255,0.06)] hover:text-[color:var(--color-text-primary)]"
            aria-label="Clear search"
          >
            <XIcon />
          </button>
        ) : null}
      </div>

      {showDropdown && (
        <div
          className="absolute left-0 right-0 top-[calc(100%+8px)] z-[var(--app-shell-z-popover)] overflow-hidden rounded-2xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] shadow-xl"
          role="listbox"
          aria-label="Search results"
        >
          {loading && (
            <div className="flex items-center gap-2 px-3 py-2 text-xs text-[color:var(--color-text-secondary)]">
              <span className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-[color:var(--color-border)] border-t-[color:var(--color-accent)]" />
              Searching…
            </div>
          )}

          {!loading && trimmed.length >= 3 && results.length === 0 && (
            <div className="px-3 py-2 text-xs text-[color:var(--color-text-secondary)]">No results.</div>
          )}

          {!loading &&
            results.map((r, idx) => {
              const active = idx === activeIdx;
              return (
                <button
                  key={`${r.lat},${r.lng},${r.displayName}`}
                  type="button"
                  onMouseEnter={() => setActiveIdx(idx)}
                  onClick={() => commit(r)}
                  className={[
                    "flex w-full items-start gap-2 px-3 py-2 text-left text-sm",
                    active ? "bg-[color:rgba(234,240,255,0.04)]" : "bg-transparent",
                    "hover:bg-[color:rgba(234,240,255,0.04)]",
                  ].join(" ")}
                  role="option"
                  aria-selected={active}
                >
                  <PinIcon />
                  <span className="line-clamp-2 text-[color:var(--color-text-primary)]">{r.displayName}</span>
                </button>
              );
            })}
        </div>
      )}
    </div>
  );
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

