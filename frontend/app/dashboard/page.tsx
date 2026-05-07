"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Header from "@/components/profile/Header";
import Sidebar, { type SidebarViewId } from "@/components/profile/Sidebar";
import Map from "@/components/profile/Map";
import RightPanel from "@/components/profile/RightPanel";
import { getToken } from "@/lib/auth";
import { useStore } from "@/store/useStore";

export default function DashboardPage() {
  const router = useRouter();
  const [sidebarView, setSidebarView] = useState<SidebarViewId>("heatmap");
  const setActiveView = useStore((s) => s.setActiveView);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
    }
  }, [router]);

  useEffect(() => {
    setActiveView(sidebarView);
  }, [sidebarView, setActiveView]);

  return (
    <div className="flex h-[100dvh] flex-col bg-slate-50">
      <Header />
      <div className="flex min-h-0 flex-1 overflow-hidden">
        <Sidebar activeView={sidebarView} onActiveViewChange={setSidebarView} />
        <main className="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden p-4 md:p-5">
          <Map simulationMode={sidebarView === "whatif"} />
        </main>
        <RightPanel />
      </div>
    </div>
  );
}
