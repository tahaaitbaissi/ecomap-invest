"use client";

import { fetchMe } from "@/services/api/userService";
import { getToken } from "@/lib/auth";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

const ADMIN_ROLE = "ROLE_ADMIN";

export function RequireAdmin({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    async function check() {
      if (!getToken()) {
        router.replace("/login?from=%2Fadmin");
        return;
      }
      try {
        const me = await fetchMe();
        if (me.role !== ADMIN_ROLE) {
          router.replace("/dashboard");
          return;
        }
        setReady(true);
      } catch {
        router.replace("/login?from=%2Fadmin");
      }
    }
    void check();
  }, [router]);

  if (!ready) {
    return (
      <div className="grid min-h-[40vh] place-items-center text-sm text-[color:var(--color-text-muted)]">
        Verifying admin…
      </div>
    );
  }
  return <>{children}</>;
}
