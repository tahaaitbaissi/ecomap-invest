"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Header from "@/components/profile/Header";
import Sidebar, { type SidebarViewId } from "@/components/profile/Sidebar";
import Map from "@/components/profile/Map";
import { getToken } from "@/lib/auth";

export default function DashboardPage() {
  const router = useRouter();
  const [sidebarView, setSidebarView] = useState<SidebarViewId>("heatmap");

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
          <Sidebar activeView={sidebarView} onActiveViewChange={setSidebarView} />
          <Map simulationMode={sidebarView === "whatif"} />
        </div>
      </div>
    </div>
  );
}
