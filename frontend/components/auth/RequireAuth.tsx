"use client";

import { getToken } from "@/lib/auth";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (!getToken()) {
      const from = pathname ? encodeURIComponent(pathname) : "";
      router.replace(from ? `/login?from=${from}` : "/login");
      return;
    }
    setReady(true);
  }, [router, pathname]);

  if (!ready) {
    return (
      <div className="grid min-h-[40vh] place-items-center text-sm text-[color:var(--color-text-muted)]">
        Loading…
      </div>
    );
  }
  return <>{children}</>;
}
