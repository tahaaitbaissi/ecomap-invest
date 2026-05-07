import Link from "next/link";
import PageShell from "@/components/ui/PageShell";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-[color:var(--color-bg-page)] text-[color:var(--color-text-primary)]">
      <PageShell className="flex gap-6 py-6">
        <aside className="w-64 shrink-0">
          <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
            <div className="text-sm font-extrabold tracking-wide text-[color:var(--color-text-primary)]">Admin console</div>
            <nav className="mt-4 flex flex-col gap-2 text-sm">
              <Link className="rounded-md px-2 py-1 text-[color:var(--color-text-secondary)] hover:bg-[color:rgba(234,240,255,0.04)] hover:text-[color:var(--color-text-primary)]" href="/admin">
                Overview
              </Link>
              <Link className="rounded-md px-2 py-1 text-[color:var(--color-text-secondary)] hover:bg-[color:rgba(234,240,255,0.04)] hover:text-[color:var(--color-text-primary)]" href="/admin/pois">
                POIs
              </Link>
              <Link className="rounded-md px-2 py-1 text-[color:var(--color-text-secondary)] hover:bg-[color:rgba(234,240,255,0.04)] hover:text-[color:var(--color-text-primary)]" href="/admin/demographics">
                Demographics
              </Link>
              <Link className="rounded-md px-2 py-1 text-[color:var(--color-text-secondary)] hover:bg-[color:rgba(234,240,255,0.04)] hover:text-[color:var(--color-text-primary)]" href="/admin/users">
                Users
              </Link>
              <Link className="rounded-md px-2 py-1 text-[color:var(--color-text-secondary)] hover:bg-[color:rgba(234,240,255,0.04)] hover:text-[color:var(--color-text-primary)]" href="/admin/audit">
                Audit logs
              </Link>
              <Link className="rounded-md px-2 py-1 text-[color:var(--color-text-secondary)] hover:bg-[color:rgba(234,240,255,0.04)] hover:text-[color:var(--color-text-primary)]" href="/admin/batch">
                Batch jobs
              </Link>
            </nav>
          </div>
        </aside>
        <main className="min-w-0 flex-1">{children}</main>
      </PageShell>
    </div>
  );
}

