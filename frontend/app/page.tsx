"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { getToken } from "@/lib/auth";

export default function Home() {
  const router = useRouter();

  useEffect(() => {
    if (getToken()) {
      router.replace("/dashboard");
      return;
    }
    router.replace("/login");
  }, [router]);

  return (
    <main className="min-h-screen grid place-items-center bg-slate-100">
      <p className="text-slate-600 text-sm">Redirecting...</p>
    </main>
  );
}
