"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Header from "@/components/profile/Header";
import Sidebar from "@/components/profile/Sidebar";
import Map from "@/components/profile/Map";
import RightPanel from "@/components/profile/RightPanel";
import { getToken } from "@/lib/auth";

export default function DashboardPage() {
  const router = useRouter();

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
    }
  }, [router]);

  return (
    <div className="min-h-screen bg-slate-50" style={{ display: "flex", flexDirection: "column" }}>
      <Header />
      <div style={{ display: "flex", flex: 1, overflow: "hidden" }}>
        <Sidebar />
        <main style={{ flex: 1, padding: "20px", overflow: "auto", minHeight: 0 }}>
          <Map />
        </main>
        <RightPanel />
      </div>
    </div>
  );
}
