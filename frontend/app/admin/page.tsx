"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { fetchMe } from "@/services/api/userService";

export default function AdminHome() {
  const router = useRouter();
  const [status, setStatus] = useState<"checking" | "ok" | "forbidden">("checking");

  useEffect(() => {
    let cancelled = false;
    fetchMe()
      .then((me) => {
        if (cancelled) return;
        if (me.role !== "ROLE_ADMIN") {
          setStatus("forbidden");
          router.replace("/dashboard");
          return;
        }
        setStatus("ok");
      })
      .catch(() => {
        if (cancelled) return;
        setStatus("forbidden");
        router.replace("/dashboard");
      });
    return () => {
      cancelled = true;
    };
  }, [router]);

  if (status !== "ok") {
    return (
      <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-6">
        <div className="text-sm text-[color:var(--color-text-secondary)]">Checking access…</div>
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-6">
      <div className="text-lg font-extrabold">Admin</div>
      <div className="mt-2 text-sm text-[color:var(--color-text-secondary)]">
        Use the left navigation to manage POIs, demographics, users, audit logs, and batch jobs.
      </div>
    </div>
  );
}

