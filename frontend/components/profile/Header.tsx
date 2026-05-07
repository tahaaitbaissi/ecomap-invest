"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import ProfilePanel from "@/components/profile/ProfilePanel";
import MapSearchBar from "@/components/map/MapSearchBar";
import { clearToken, getToken } from "@/lib/auth";
import { getMyProfile } from "@/lib/api";

export default function Header() {
  const router = useRouter();
  const [profileOpen, setProfileOpen] = useState(false);
  const [initials, setInitials] = useState("..");

  useEffect(() => {
    const loadProfile = async () => {
      try {
        const profile = await getMyProfile();
        setInitials(getInitials(profile.companyName ?? profile.email));
      } catch {
        if (!getToken()) {
          router.replace("/login");
        }
      }
    };
    void loadProfile();
  }, [router]);

  const handleProfileChange = (newInitials: string) => {
    setInitials(newInitials);
  };

  const handleLogout = () => {
    clearToken();
    router.replace("/login");
  };

  return (
    <>
      <header
        className="sticky top-0 z-[var(--app-shell-z-header)] flex h-[var(--app-header-h)] w-full shrink-0 items-center border-b border-white/10 bg-[var(--brand-blue)]/95 px-4 backdrop-blur-md md:px-8"
      >
        <div className="flex min-w-0 flex-1 items-center gap-3">
          <button
            onClick={() => router.push("/dashboard")}
            className="flex shrink-0 items-center rounded-lg p-1 transition hover:bg-white/10"
            type="button"
            aria-label="Go to dashboard"
          >
            <Image
              src="/logoNoBg.svg"
              alt="EcoMap Invest"
              width={200}
              height={56}
              className="h-12 w-auto"
              priority
              unoptimized
            />
          </button>

          <div className="hidden min-w-0 flex-1 justify-center md:flex">
            <div className="w-full max-w-[680px]">
              <MapSearchBar />
            </div>
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-2 md:gap-3">
          <div className="hidden items-center gap-2 rounded-full bg-emerald-700/90 px-3 py-1.5 text-sm font-semibold text-white md:flex">
            <span className="h-2.5 w-2.5 rounded-full bg-emerald-300" />
            Connected
          </div>

          <button
            type="button"
            onClick={handleLogout}
            className="inline-flex items-center gap-2 rounded-full border border-white/30 bg-white/0 px-3 py-2 text-sm font-medium text-white transition hover:bg-white/10"
          >
            <LogoutIcon />
            <span className="hidden sm:inline">Logout</span>
          </button>

          <button
            type="button"
            onClick={() => setProfileOpen(true)}
            aria-label="Open profile panel"
            className="relative inline-flex h-11 w-11 items-center justify-center rounded-full border-2 border-white/70 bg-gradient-to-br from-sky-400 to-indigo-500 text-sm font-bold text-white shadow-sm transition-transform hover:scale-[1.03]"
          >
            {initials || "?"}
            <span className="absolute -right-0.5 -top-0.5 h-3.5 w-3.5 rounded-full border-2 border-[var(--brand-blue)] bg-emerald-400" />
          </button>
        </div>
      </header>

      <ProfilePanel open={profileOpen} onClose={() => setProfileOpen(false)} onProfileChange={handleProfileChange} />
    </>
  );
}

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "..";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0] ?? ""}${parts[1][0] ?? ""}`.toUpperCase();
}

function LogoutIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" />
      <line x1="21" y1="12" x2="9" y2="12" />
    </svg>
  );
}
