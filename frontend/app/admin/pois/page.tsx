"use client";

import { useEffect, useMemo, useState } from "react";
import AdminGuard from "@/components/admin/AdminGuard";
import Pagination from "@/components/admin/Pagination";
import Input from "@/components/ui/Input";
import {
  adminCreatePoi,
  adminDeletePoi,
  adminSearchPois,
  adminUpdatePoi,
  type AdminPoiResponse,
  type AdminPoiUpsertRequest,
} from "@/services/api/admin/adminPoiService";

export default function AdminPoisPage() {
  const [typeTag, setTypeTag] = useState("");
  const [nameLike, setNameLike] = useState("");
  const [sort, setSort] = useState<"importedAt,desc" | "name,asc" | "typeTag,asc" | "osmId,asc">(
    "importedAt,desc",
  );

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(50);
  const [total, setTotal] = useState(0);

  const [items, setItems] = useState<AdminPoiResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [editing, setEditing] = useState<AdminPoiResponse | null>(null);
  const [draft, setDraft] = useState<AdminPoiUpsertRequest>({
    typeTag: "",
    lat: 0,
    lng: 0,
  });

  const canSubmit = useMemo(() => {
    return draft.typeTag.trim().length >= 2 && Number.isFinite(draft.lat) && Number.isFinite(draft.lng);
  }, [draft]);

  async function refresh(p = page) {
    setLoading(true);
    setError(null);
    try {
      const res = await adminSearchPois({
        typeTag: typeTag.trim() ? typeTag.trim() : undefined,
        nameLike: nameLike.trim() ? nameLike.trim() : undefined,
        page: p,
        size,
        sort,
      });
      setItems(res.items);
      setTotal(res.totalElements);
      setPage(res.page);
      setSize(res.size);
    } catch (e: any) {
      setError(e?.response?.data ?? e?.message ?? "Failed to search");
    } finally {
      setLoading(false);
    }
  }

  async function submit() {
    if (!canSubmit) return;
    setLoading(true);
    setError(null);
    try {
      if (editing) {
        await adminUpdatePoi(editing.id, draft);
      } else {
        await adminCreatePoi(draft);
      }
      setEditing(null);
      setDraft({ typeTag: "", lat: 0, lng: 0 });
      await refresh(0);
    } catch (e: any) {
      setError(e?.response?.data ?? e?.message ?? "Save failed");
    } finally {
      setLoading(false);
    }
  }

  async function del(id: string) {
    if (!confirm("Delete this POI?")) return;
    setLoading(true);
    setError(null);
    try {
      await adminDeletePoi(id);
      await refresh(0);
    } catch (e: any) {
      setError(e?.response?.data ?? e?.message ?? "Delete failed");
    } finally {
      setLoading(false);
    }
  }

  function startCreate() {
    setEditing(null);
    setDraft({ typeTag: "", lat: 0, lng: 0 });
  }

  function startEdit(p: AdminPoiResponse) {
    setEditing(p);
    setDraft({
      osmId: p.osmId ?? undefined,
      name: p.name ?? undefined,
      address: p.address ?? undefined,
      typeTag: p.typeTag,
      lat: p.lat,
      lng: p.lng,
      priceLevel: p.priceLevel ?? undefined,
      rating: p.rating ?? undefined,
    });
  }

  useEffect(() => {
    refresh(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sort, size]);

  return (
    <AdminGuard>
      <div className="flex flex-col gap-4">
        <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="text-lg font-extrabold">POIs</div>
            <div className="flex gap-2">
              <button
                className="ds-btn ds-btn-secondary px-3 py-2 text-sm disabled:opacity-50"
                onClick={startCreate}
                disabled={loading}
              >
                New POI
              </button>
              <button
                className="ds-btn ds-btn-primary px-3 py-2 text-sm disabled:opacity-50"
                onClick={() => refresh(0)}
                disabled={loading}
              >
                Search
              </button>
            </div>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-4">
            <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
              Type tag
              <Input
                value={typeTag}
                onChange={(e) => setTypeTag(e.target.value)}
                placeholder='e.g. "category=school"'
              />
            </label>
            <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
              Name contains
              <Input
                value={nameLike}
                onChange={(e) => setNameLike(e.target.value)}
                placeholder="e.g. marjane"
              />
            </label>
            <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
              Sort
              <select
                className="w-full rounded-[12px] border border-[color:var(--color-border)] bg-transparent px-3 py-3 text-sm text-[color:var(--color-text-primary)] outline-none focus:ring-2 focus:ring-[color:rgba(47,107,255,0.35)]"
                value={sort}
                onChange={(e) => setSort(e.target.value as any)}
              >
                <option value="importedAt,desc">ImportedAt ↓</option>
                <option value="name,asc">Name ↑</option>
                <option value="typeTag,asc">Type ↑</option>
                <option value="osmId,asc">OSM id ↑</option>
              </select>
            </label>
            <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
              Page size
              <select
                className="w-full rounded-[12px] border border-[color:var(--color-border)] bg-transparent px-3 py-3 text-sm text-[color:var(--color-text-primary)] outline-none focus:ring-2 focus:ring-[color:rgba(47,107,255,0.35)]"
                value={size}
                onChange={(e) => setSize(Number(e.target.value))}
              >
                <option value={25}>25</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
                <option value={200}>200</option>
              </select>
            </label>
          </div>

          {error ? <div className="mt-3 text-sm text-red-300">{error}</div> : null}
        </div>

        <div className="grid grid-cols-1 gap-4 lg:grid-cols-5">
          <div className="lg:col-span-3 rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
            <Pagination page={page} size={size} totalElements={total} onPage={(p) => refresh(p)} />
            <div className="mt-3 overflow-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-xs uppercase text-[color:var(--color-text-muted)]">
                  <tr>
                    <th className="py-2 pr-3">Name</th>
                    <th className="py-2 pr-3">Type</th>
                    <th className="py-2 pr-3">Imported</th>
                    <th className="py-2 pr-3">Lat/Lng</th>
                    <th className="py-2 pr-3"></th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((p) => (
                    <tr key={p.id} className="border-t border-[color:var(--color-border)]">
                      <td className="py-2 pr-3">
                        <div className="text-[color:var(--color-text-primary)]">{p.name ?? "(no name)"}</div>
                        <div className="text-xs text-[color:var(--color-text-muted)]">{p.osmId}</div>
                      </td>
                      <td className="py-2 pr-3 text-[color:var(--color-text-secondary)]">{p.typeTag}</td>
                      <td className="py-2 pr-3 text-[color:var(--color-text-secondary)]">{p.importedAt ?? "-"}</td>
                      <td className="py-2 pr-3 text-[color:var(--color-text-secondary)]">
                        {p.lat.toFixed(6)}, {p.lng.toFixed(6)}
                      </td>
                      <td className="py-2 pr-3 text-right">
                        <button
                          className="ds-btn ds-btn-secondary px-2 py-1 text-xs"
                          onClick={() => startEdit(p)}
                          disabled={loading}
                        >
                          Edit
                        </button>
                        <button
                          className="ds-btn ds-btn-secondary ml-1 border-red-500/30 px-2 py-1 text-xs text-red-300 hover:bg-red-500/10"
                          onClick={() => del(p.id)}
                          disabled={loading}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                  {items.length === 0 ? (
                    <tr>
                      <td className="py-6 text-center text-slate-400" colSpan={5}>
                        {loading ? "Loading…" : "No results"}
                      </td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
            <div className="mt-3">
              <Pagination page={page} size={size} totalElements={total} onPage={(p) => refresh(p)} />
            </div>
          </div>

          <div className="lg:col-span-2 rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
            <div className="text-sm font-extrabold text-[color:var(--color-text-primary)]">{editing ? "Edit POI" : "Create POI"}</div>
            <div className="mt-3 grid grid-cols-1 gap-3">
              <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                OSM id (optional)
                <Input
                  value={draft.osmId ?? ""}
                  onChange={(e) => setDraft((d) => ({ ...d, osmId: e.target.value }))}
                  placeholder='e.g. "node/123" or leave empty for manual:*'
                />
              </label>
              <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                Name
                <Input
                  value={draft.name ?? ""}
                  onChange={(e) => setDraft((d) => ({ ...d, name: e.target.value }))}
                />
              </label>
              <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                Address
                <Input
                  value={draft.address ?? ""}
                  onChange={(e) => setDraft((d) => ({ ...d, address: e.target.value }))}
                />
              </label>
              <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                Type tag *
                <Input
                  value={draft.typeTag}
                  onChange={(e) => setDraft((d) => ({ ...d, typeTag: e.target.value }))}
                  placeholder="category=office"
                />
              </label>
              <div className="grid grid-cols-2 gap-3">
                <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                  Lat *
                  <Input
                    type="number"
                    value={draft.lat}
                    onChange={(e) => setDraft((d) => ({ ...d, lat: Number(e.target.value) }))}
                  />
                </label>
                <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                  Lng *
                  <Input
                    type="number"
                    value={draft.lng}
                    onChange={(e) => setDraft((d) => ({ ...d, lng: Number(e.target.value) }))}
                  />
                </label>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                  Price level
                  <Input
                    type="number"
                    value={draft.priceLevel ?? ""}
                    onChange={(e) =>
                      setDraft((d) => ({
                        ...d,
                        priceLevel: e.target.value === "" ? null : Number(e.target.value),
                      }))
                    }
                  />
                </label>
                <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
                  Rating
                  <Input
                    type="number"
                    value={draft.rating ?? ""}
                    onChange={(e) =>
                      setDraft((d) => ({
                        ...d,
                        rating: e.target.value === "" ? null : Number(e.target.value),
                      }))
                    }
                  />
                </label>
              </div>

              <button
                className="ds-btn ds-btn-primary mt-1 w-full disabled:opacity-50"
                onClick={submit}
                disabled={loading || !canSubmit}
              >
                {editing ? "Save changes" : "Create"}
              </button>
            </div>
          </div>
        </div>
      </div>
    </AdminGuard>
  );
}

