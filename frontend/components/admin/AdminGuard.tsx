"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { fetchMe } from "@/services/api/userService";

export default function AdminGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [ok, setOk] = useState(false);

  useEffect(() => {
    let cancelled = false;
    fetchMe()
      .then((me) => {
        if (cancelled) return;
        if (me.role !== "ROLE_ADMIN") {
          router.replace("/dashboard");
          return;
        }
        setOk(true);
      })
      .catch(() => router.replace("/dashboard"));
    return () => {
      cancelled = true;
    };
  }, [router]);

  if (!ok) {
    return (
      <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-6">
        <div className="text-sm text-slate-200">Checking access…</div>
      </div>
    );
  }

  return <>{children}</>;
}

