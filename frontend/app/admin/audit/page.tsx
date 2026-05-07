"use client";

import { useEffect, useState } from "react";
import AdminGuard from "@/components/admin/AdminGuard";
import Pagination from "@/components/admin/Pagination";
import { adminListAuditLogs } from "@/services/api/admin/adminAuditService";
import Input from "@/components/ui/Input";

export default function AdminAuditPage() {
  const [userEmail, setUserEmail] = useState("");
  const [action, setAction] = useState("");
  const [success, setSuccess] = useState<"" | "true" | "false">("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");

  const [page, setPage] = useState(0);
  const [size] = useState(50);
  const [total, setTotal] = useState(0);
  const [items, setItems] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    refresh(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function refresh(p = page) {
    setLoading(true);
    setError(null);
    try {
      const res = await adminListAuditLogs({
        userEmail: userEmail.trim() ? userEmail.trim() : undefined,
        action: action.trim() ? action.trim() : undefined,
        success: success === "" ? undefined : success === "true",
        from: from.trim() ? from.trim() : undefined,
        to: to.trim() ? to.trim() : undefined,
        page: p,
        size,
      });
      setItems(res.items);
      setTotal(res.totalElements);
      setPage(res.page);
    } catch (e: any) {
      setError(e?.response?.data ?? e?.message ?? "Failed to load audit logs");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AdminGuard>
      <div className="flex flex-col gap-4">
        <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="text-lg font-extrabold">Audit logs</div>
            <button
              className="ds-btn ds-btn-primary px-3 py-2 text-sm disabled:opacity-50"
              onClick={() => refresh(0)}
              disabled={loading}
            >
              Apply filters
            </button>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-5">
            <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
              User email contains
              <Input
                value={userEmail}
                onChange={(e) => setUserEmail(e.target.value)}
              />
            </label>
            <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
              Action contains
              <Input
                value={action}
                onChange={(e) => setAction(e.target.value)}
              />
            </label>
            <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
              Success
              <select
                className="w-full rounded-[12px] border border-[color:var(--color-border)] bg-transparent px-3 py-3 text-sm text-[color:var(--color-text-primary)] outline-none focus:ring-2 focus:ring-[color:rgba(47,107,255,0.35)]"
                value={success}
                onChange={(e) => setSuccess(e.target.value as any)}
              >
                <option value="">Any</option>
                <option value="true">true</option>
                <option value="false">false</option>
              </select>
            </label>
            <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
              From (Instant)
              <Input
                value={from}
                onChange={(e) => setFrom(e.target.value)}
                placeholder="2026-01-01T00:00:00Z"
              />
            </label>
            <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
              To (Instant)
              <Input
                value={to}
                onChange={(e) => setTo(e.target.value)}
                placeholder="2026-01-31T23:59:59Z"
              />
            </label>
          </div>

          {error ? <div className="mt-3 text-sm text-red-300">{error}</div> : null}
        </div>

        <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
          <Pagination page={page} size={size} totalElements={total} onPage={(p) => refresh(p)} />

          <div className="mt-3 overflow-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase text-[color:var(--color-text-muted)]">
                <tr>
                  <th className="py-2 pr-3">Time</th>
                  <th className="py-2 pr-3">User</th>
                  <th className="py-2 pr-3">Action</th>
                  <th className="py-2 pr-3">Success</th>
                  <th className="py-2 pr-3">Duration</th>
                  <th className="py-2 pr-3">Error</th>
                </tr>
              </thead>
              <tbody>
                {items.map((l) => (
                  <tr key={l.id} className="border-t border-[color:var(--color-border)] align-top">
                    <td className="py-2 pr-3 text-[color:var(--color-text-secondary)]">{l.occurredAt ?? "-"}</td>
                    <td className="py-2 pr-3 text-[color:var(--color-text-secondary)]">{l.userEmail ?? "-"}</td>
                    <td className="py-2 pr-3 text-[color:var(--color-text-primary)]">
                      {l.action ?? "-"}
                      <div className="text-xs text-[color:var(--color-text-muted)]">{l.method ?? ""}</div>
                      {l.argsSummary ? (
                        <div className="mt-1 line-clamp-2 text-xs text-[color:var(--color-text-muted)]">{l.argsSummary}</div>
                      ) : null}
                    </td>
                    <td className="py-2 pr-3 text-[color:var(--color-text-secondary)]">
                      {l.success == null ? "-" : l.success ? "true" : "false"}
                    </td>
                    <td className="py-2 pr-3 text-[color:var(--color-text-secondary)]">
                      {l.durationMs == null ? "-" : `${l.durationMs}ms`}
                    </td>
                    <td className="py-2 pr-3 text-[color:var(--color-text-secondary)]">
                      {l.errorClass ? (
                        <>
                          <div className="text-red-200">{l.errorClass}</div>
                          <div className="text-xs text-red-300">{l.errorMessage ?? ""}</div>
                        </>
                      ) : (
                        "-"
                      )}
                    </td>
                  </tr>
                ))}
                {items.length === 0 ? (
                  <tr>
                    <td className="py-6 text-center text-[color:var(--color-text-muted)]" colSpan={6}>
                      {loading ? "Loading…" : "No logs"}
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
      </div>
    </AdminGuard>
  );
}

