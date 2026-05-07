"use client";

import { useEffect, useState } from "react";
import AdminGuard from "@/components/admin/AdminGuard";
import Pagination from "@/components/admin/Pagination";
import { adminListUsers, adminSetUserRole } from "@/services/api/admin/adminUsersService";
import Input from "@/components/ui/Input";

export default function AdminUsersPage() {
  const [page, setPage] = useState(0);
  const [size] = useState(25);
  const [total, setTotal] = useState(0);
  const [items, setItems] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [emailLike, setEmailLike] = useState("");

  useEffect(() => {
    refresh(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function refresh(p = page) {
    setLoading(true);
    setError(null);
    try {
      const res = await adminListUsers({
        page: p,
        size,
        emailLike: emailLike.trim() ? emailLike.trim() : undefined,
      });
      setItems(res.items);
      setTotal(res.totalElements);
      setPage(res.page);
    } catch (e: any) {
      setError(e?.response?.data ?? e?.message ?? "Failed to load users");
    } finally {
      setLoading(false);
    }
  }

  async function setRole(id: string, role: "ROLE_INVESTOR" | "ROLE_ADMIN") {
    setLoading(true);
    setError(null);
    try {
      await adminSetUserRole(id, role);
      await refresh(page);
    } catch (e: any) {
      setError(e?.response?.data ?? e?.message ?? "Role update failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AdminGuard>
      <div className="flex flex-col gap-4">
        <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="text-lg font-extrabold">Users</div>
            <button
              className="ds-btn ds-btn-primary px-3 py-2 text-sm disabled:opacity-50"
              onClick={() => refresh(0)}
              disabled={loading}
            >
              Search
            </button>
          </div>
          <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-3">
            <label className="flex flex-col gap-1 text-xs text-[color:var(--color-text-secondary)]">
              Email contains
              <Input
                value={emailLike}
                onChange={(e) => setEmailLike(e.target.value)}
                placeholder="e.g. admin"
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
                  <th className="py-2 pr-3">Email</th>
                  <th className="py-2 pr-3">Company</th>
                  <th className="py-2 pr-3">Role</th>
                  <th className="py-2 pr-3"></th>
                </tr>
              </thead>
              <tbody>
                {items.map((u) => (
                  <tr key={u.id} className="border-t border-[color:var(--color-border)]">
                    <td className="py-2 pr-3 text-[color:var(--color-text-primary)]">{u.email}</td>
                    <td className="py-2 pr-3 text-[color:var(--color-text-secondary)]">{u.companyName ?? "-"}</td>
                    <td className="py-2 pr-3 text-[color:var(--color-text-secondary)]">{u.role}</td>
                    <td className="py-2 pr-3 text-right">
                      <select
                        className="rounded-[12px] border border-[color:var(--color-border)] bg-transparent px-3 py-2 text-sm text-[color:var(--color-text-primary)] outline-none focus:ring-2 focus:ring-[color:rgba(47,107,255,0.35)]"
                        value={u.role}
                        disabled={loading}
                        onChange={(e) =>
                          setRole(u.id, e.target.value as "ROLE_INVESTOR" | "ROLE_ADMIN")
                        }
                      >
                        <option value="ROLE_INVESTOR">ROLE_INVESTOR</option>
                        <option value="ROLE_ADMIN">ROLE_ADMIN</option>
                      </select>
                    </td>
                  </tr>
                ))}
                {items.length === 0 ? (
                  <tr>
                    <td className="py-6 text-center text-[color:var(--color-text-muted)]" colSpan={4}>
                      {loading ? "Loading…" : "No users"}
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

