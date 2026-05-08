"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Input from "@/components/ui/Input";
import { fetchMe } from "@/services/api/userService";
import {
  adminAuditFootTraffic,
  adminListFootTrafficParams,
  adminRecomputeFootTraffic,
  adminUpsertFootTrafficParams,
  type AdminFootTrafficParamsResponse,
  type AdminFootTrafficParamsUpsertRequest,
} from "@/services/api/admin/adminFootTrafficService";

function parseJsonArray(raw: string, expectedLen: number): number[] {
  const v = JSON.parse(raw);
  if (!Array.isArray(v) || v.length !== expectedLen) {
    throw new Error(`Expected JSON array of length ${expectedLen}`);
  }
  return v.map((x) => Number(x));
}

export default function AdminFootTrafficPage() {
  const router = useRouter();
  const [guard, setGuard] = useState<"checking" | "ok">("checking");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const [params, setParams] = useState<AdminFootTrafficParamsResponse[]>([]);
  const [selected, setSelected] = useState<string>("");

  const active = useMemo(() => params.find((p) => p.archetype === selected) ?? null, [params, selected]);

  const [form, setForm] = useState<AdminFootTrafficParamsUpsertRequest | null>(null);
  const [wdJson, setWdJson] = useState<string>("[]");
  const [satJson, setSatJson] = useState<string>("[]");
  const [sunJson, setSunJson] = useState<string>("[]");
  const [seasonJson, setSeasonJson] = useState<string>("[]");

  const [recomputePrefix, setRecomputePrefix] = useState<string>("");
  const [auditH3, setAuditH3] = useState<string>("");
  const [audit, setAudit] = useState<any>(null);

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

  useEffect(() => {
    if (guard !== "ok") return;
    setLoading(true);
    setError(null);
    adminListFootTrafficParams()
      .then((rows) => {
        setParams(rows);
        if (!selected && rows.length) setSelected(rows[0].archetype);
      })
      .catch((e: any) => setError(e?.response?.data ?? e?.message ?? "Failed to load params"))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [guard]);

  useEffect(() => {
    if (!active) return;
    setForm({
      baseDailyMin: active.baseDailyMin,
      baseDailyMax: active.baseDailyMax,
      poiDensityCap: active.poiDensityCap,
      popDensityCap: active.popDensityCap,
      incomeWeight: active.incomeWeight,
      hourlyCurveWd: active.hourlyCurveWd,
      hourlyCurveSat: active.hourlyCurveSat,
      hourlyCurveSun: active.hourlyCurveSun,
      seasonalScalers: active.seasonalScalers,
      dayScalerSat: active.dayScalerSat,
      dayScalerSun: active.dayScalerSun,
      noiseSigma: active.noiseSigma,
    });
    setWdJson(JSON.stringify(active.hourlyCurveWd));
    setSatJson(JSON.stringify(active.hourlyCurveSat));
    setSunJson(JSON.stringify(active.hourlyCurveSun));
    setSeasonJson(JSON.stringify(active.seasonalScalers));
  }, [active]);

  async function save() {
    if (!form || !selected) return;
    setLoading(true);
    setError(null);
    setInfo(null);
    try {
      const payload: AdminFootTrafficParamsUpsertRequest = {
        ...form,
        hourlyCurveWd: parseJsonArray(wdJson, 24),
        hourlyCurveSat: parseJsonArray(satJson, 24),
        hourlyCurveSun: parseJsonArray(sunJson, 24),
        seasonalScalers: parseJsonArray(seasonJson, 12),
      };
      const updated = await adminUpsertFootTrafficParams(selected, payload);
      setParams((prev) => prev.map((p) => (p.archetype === selected ? updated : p)));
      setInfo("Saved.");
    } catch (e: any) {
      setError(e?.response?.data ?? e?.message ?? "Save failed");
    } finally {
      setLoading(false);
    }
  }

  async function recompute() {
    setLoading(true);
    setError(null);
    setInfo(null);
    try {
      const r = await adminRecomputeFootTraffic({ h3Prefix: recomputePrefix.trim() || undefined });
      setInfo(`Recompute done: ${r.cellsProcessed} cells (v=${r.trafficVersion}).`);
    } catch (e: any) {
      setError(e?.response?.data ?? e?.message ?? "Recompute failed");
    } finally {
      setLoading(false);
    }
  }

  async function runAudit() {
    if (!auditH3.trim()) return;
    setLoading(true);
    setError(null);
    setInfo(null);
    try {
      const r = await adminAuditFootTraffic(auditH3.trim());
      setAudit(r);
    } catch (e: any) {
      setError(e?.response?.data ?? e?.message ?? "Audit failed");
      setAudit(null);
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
        <div className="text-lg font-extrabold">Foot traffic</div>
        <div className="mt-2 text-xs text-[color:var(--color-text-muted)]">
          Edit zone parameters (archetypes) and trigger recompute. Curves are JSON arrays.
        </div>
        {error ? <div className="mt-3 text-sm text-red-300">{error}</div> : null}
        {info ? <div className="mt-3 text-sm text-emerald-300">{info}</div> : null}
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
          <div className="text-sm font-bold">Zone params</div>
          <div className="mt-3 flex flex-col gap-3">
            <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
              Archetype
              <select
                className="w-full rounded-[12px] border border-[color:var(--color-border)] bg-transparent px-4 py-3 text-sm"
                value={selected}
                onChange={(e) => setSelected(e.target.value)}
                disabled={loading}
              >
                {params.map((p) => (
                  <option key={p.archetype} value={p.archetype}>
                    {p.archetype}
                  </option>
                ))}
              </select>
            </label>

            {form ? (
              <>
                <div className="grid grid-cols-2 gap-3">
                  <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                    baseDailyMin
                    <Input
                      type="number"
                      value={form.baseDailyMin}
                      onChange={(e) => setForm({ ...form, baseDailyMin: Number(e.target.value) })}
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                    baseDailyMax
                    <Input
                      type="number"
                      value={form.baseDailyMax}
                      onChange={(e) => setForm({ ...form, baseDailyMax: Number(e.target.value) })}
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                    poiDensityCap
                    <Input
                      type="number"
                      value={form.poiDensityCap}
                      onChange={(e) => setForm({ ...form, poiDensityCap: Number(e.target.value) })}
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                    popDensityCap
                    <Input
                      type="number"
                      value={form.popDensityCap}
                      onChange={(e) => setForm({ ...form, popDensityCap: Number(e.target.value) })}
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                    incomeWeight
                    <Input
                      type="number"
                      value={form.incomeWeight}
                      onChange={(e) => setForm({ ...form, incomeWeight: Number(e.target.value) })}
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                    noiseSigma
                    <Input
                      type="number"
                      value={form.noiseSigma}
                      onChange={(e) => setForm({ ...form, noiseSigma: Number(e.target.value) })}
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                    dayScalerSat
                    <Input
                      type="number"
                      value={form.dayScalerSat}
                      onChange={(e) => setForm({ ...form, dayScalerSat: Number(e.target.value) })}
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                    dayScalerSun
                    <Input
                      type="number"
                      value={form.dayScalerSun}
                      onChange={(e) => setForm({ ...form, dayScalerSun: Number(e.target.value) })}
                    />
                  </label>
                </div>

                <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                  hourlyCurveWd (24)
                  <textarea
                    className="min-h-[86px] w-full rounded-[12px] border border-[color:var(--color-border)] bg-transparent px-4 py-3 text-sm"
                    value={wdJson}
                    onChange={(e) => setWdJson(e.target.value)}
                  />
                </label>
                <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                  hourlyCurveSat (24)
                  <textarea
                    className="min-h-[86px] w-full rounded-[12px] border border-[color:var(--color-border)] bg-transparent px-4 py-3 text-sm"
                    value={satJson}
                    onChange={(e) => setSatJson(e.target.value)}
                  />
                </label>
                <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                  hourlyCurveSun (24)
                  <textarea
                    className="min-h-[86px] w-full rounded-[12px] border border-[color:var(--color-border)] bg-transparent px-4 py-3 text-sm"
                    value={sunJson}
                    onChange={(e) => setSunJson(e.target.value)}
                  />
                </label>
                <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                  seasonalScalers (12)
                  <textarea
                    className="min-h-[72px] w-full rounded-[12px] border border-[color:var(--color-border)] bg-transparent px-4 py-3 text-sm"
                    value={seasonJson}
                    onChange={(e) => setSeasonJson(e.target.value)}
                  />
                </label>

                <button
                  className="ds-btn ds-btn-primary px-3 py-2 text-sm disabled:opacity-50"
                  onClick={save}
                  disabled={loading || !selected}
                >
                  Save params
                </button>
              </>
            ) : (
              <div className="text-xs text-[color:var(--color-text-muted)]">{loading ? "Loading…" : "No params loaded."}</div>
            )}
          </div>
        </div>

        <div className="flex flex-col gap-4">
          <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
            <div className="text-sm font-bold">Recompute</div>
            <div className="mt-3 flex flex-wrap items-end gap-3">
              <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                Optional H3 prefix filter
                <Input
                  value={recomputePrefix}
                  onChange={(e) => setRecomputePrefix(e.target.value)}
                  placeholder="8923"
                />
              </label>
              <button
                className="ds-btn ds-btn-secondary px-3 py-2 text-sm disabled:opacity-50"
                onClick={recompute}
                disabled={loading}
              >
                Run recompute
              </button>
            </div>
          </div>

          <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
            <div className="text-sm font-bold">Cell audit</div>
            <div className="mt-3 flex flex-wrap items-end gap-3">
              <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                H3 index
                <Input value={auditH3} onChange={(e) => setAuditH3(e.target.value)} placeholder="8928308280fffff" />
              </label>
              <button
                className="ds-btn ds-btn-secondary px-3 py-2 text-sm disabled:opacity-50"
                onClick={runAudit}
                disabled={loading || !auditH3.trim()}
              >
                Fetch audit
              </button>
            </div>
            {audit ? (
              <pre className="mt-3 max-h-[360px] overflow-auto rounded-lg border border-[color:var(--color-border)] bg-black/20 p-3 text-xs">
                {JSON.stringify(audit, null, 2)}
              </pre>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  );
}

