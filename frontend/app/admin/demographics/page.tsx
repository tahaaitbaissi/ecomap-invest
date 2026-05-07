"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { fetchMe } from "@/services/api/userService";
import Input from "@/components/ui/Input";
import {
  adminDeleteDemographics,
  adminGetDemographics,
  adminUpsertDemographics,
} from "@/services/api/admin/adminDemographicsService";

export default function AdminDemographicsPage() {
  const router = useRouter();
  const [guard, setGuard] = useState<"checking" | "ok">("checking");

  const [h3Index, setH3Index] = useState("");
  const [populationDensity, setPopulationDensity] = useState<number>(0);
  const [avgIncome, setAvgIncome] = useState<number>(0);

  const [loaded, setLoaded] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchMe()
      .then((me) => {
        if (cancelled) return;
        if (me.role !== "ROLE_ADMIN") {
          router.replace("/dashboard");
          return;
        }
        setGuard("ok");
      })
      .catch(() => router.replace("/dashboard"));
    return () => {
      cancelled = true;
    };
  }, [router]);

  const canLookup = useMemo(() => h3Index.trim().length > 5, [h3Index]);
  const canSave = useMemo(
    () => canLookup && Number.isFinite(populationDensity) && Number.isFinite(avgIncome),
    [canLookup, populationDensity, avgIncome],
  );

  async function lookup() {
    if (!canLookup) return;
    setLoading(true);
    setError(null);
    try {
      const r = await adminGetDemographics(h3Index.trim());
      setPopulationDensity(r.populationDensity ?? 0);
      setAvgIncome(r.avgIncome ?? 0);
      setLastUpdated(r.lastUpdated ?? null);
      setLoaded(true);
    } catch (e: any) {
      setError(e?.response?.data ?? e?.message ?? "Not found");
      setLoaded(false);
      setLastUpdated(null);
    } finally {
      setLoading(false);
    }
  }

  async function save() {
    if (!canSave) return;
    setLoading(true);
    setError(null);
    try {
      const r = await adminUpsertDemographics(h3Index.trim(), { populationDensity, avgIncome });
      setLastUpdated(r.lastUpdated ?? null);
      setLoaded(true);
    } catch (e: any) {
      setError(e?.response?.data ?? e?.message ?? "Save failed");
    } finally {
      setLoading(false);
    }
  }

  async function del() {
    if (!canLookup) return;
    if (!confirm("Delete demographics row for this h3Index?")) return;
    setLoading(true);
    setError(null);
    try {
      await adminDeleteDemographics(h3Index.trim());
      setLoaded(false);
      setLastUpdated(null);
    } catch (e: any) {
      setError(e?.response?.data ?? e?.message ?? "Delete failed");
    } finally {
      setLoading(false);
    }
  }

  if (guard !== "ok") {
    return (
      <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-6">
        <div className="text-sm text-[color:var(--color-text-secondary)]">Checking access…</div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
        <div className="text-lg font-extrabold">Demographics</div>
        <div className="mt-3 flex flex-wrap items-end gap-3">
          <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
            H3 index
            <Input
              className="w-[420px]"
              value={h3Index}
              onChange={(e) => setH3Index(e.target.value)}
              placeholder="8928308280fffff"
            />
          </label>
          <button
            className="ds-btn ds-btn-primary px-3 py-2 text-sm disabled:opacity-50"
            onClick={lookup}
            disabled={loading || !canLookup}
          >
            Lookup
          </button>
          <button
            className="ds-btn ds-btn-secondary px-3 py-2 text-sm disabled:opacity-50"
            onClick={del}
            disabled={loading || !canLookup}
          >
            Delete row
          </button>
        </div>
        {error ? <div className="mt-3 text-sm text-red-300">{error}</div> : null}
      </div>

      <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
        <div className="text-sm text-[color:var(--color-text-secondary)]">
          Status:{" "}
          <span className="text-[color:var(--color-text-primary)]">{loaded ? "Loaded" : "Not loaded (you can still Save to upsert)"}</span>
        </div>
        {lastUpdated ? (
          <div className="mt-1 text-xs text-[color:var(--color-text-muted)]">Last updated: {lastUpdated}</div>
        ) : null}

        <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
          <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
            Population density
            <Input
              type="number"
              value={populationDensity}
              onChange={(e) => setPopulationDensity(Number(e.target.value))}
            />
          </label>
          <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
            Avg income
            <Input
              type="number"
              value={avgIncome}
              onChange={(e) => setAvgIncome(Number(e.target.value))}
            />
          </label>
        </div>
        <button
          className="ds-btn ds-btn-primary mt-4 px-3 py-2 text-sm disabled:opacity-50"
          onClick={save}
          disabled={loading || !canSave}
        >
          Save (upsert)
        </button>
      </div>
    </div>
  );
}

