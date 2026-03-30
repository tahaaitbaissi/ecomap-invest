"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Header from "@/components/profile/Header";
import Sidebar from "@/components/profile/Sidebar";
import Map from "@/components/profile/Map";
import { getToken } from "@/lib/auth";

export default function DashboardPage() {
  const router = useRouter();

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
    }
  }, [router]);

  return (
    <div className="min-h-screen bg-slate-50">
      <Header />
      <div className="mx-auto max-w-[1600px] px-4 md:px-8 py-5">
        <div className="grid grid-cols-1 lg:grid-cols-[340px_1fr] gap-5">
          <Sidebar />
          <Map />
        </div>
      </div>
    </div>
  );
}
