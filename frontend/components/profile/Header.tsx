"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import ProfilePanel from "@/components/profile/ProfilePanel";
import MapSearchBar from "@/components/map/MapSearchBar";
import ThemeToggle from "@/components/theme/ThemeToggle";
import { clearToken, getToken } from "@/lib/auth";
import { getMyProfile } from "@/lib/api";
import logo from "@/app/logo.png";

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
        className="ds-accent-topbar ds-accent-underline sticky top-0 z-[var(--app-shell-z-header)] flex h-[var(--app-header-h)] w-full shrink-0 items-center border-b border-[color:var(--color-border)] bg-[color:var(--color-bg-page)]/88 px-4 backdrop-blur-md md:px-8"
      >
        <div className="flex min-w-0 flex-1 items-center gap-3">
          <button
            onClick={() => router.push("/dashboard")}
            className="group flex shrink-0 items-center gap-2 rounded-xl px-2 py-1.5 transition hover:bg-[color:rgba(234,240,255,0.04)]"
            type="button"
            aria-label="Go to dashboard"
          >
            <BrandMark />
            <div className="hidden sm:flex sm:flex-col sm:leading-none">
              <span className="text-[15px] font-extrabold tracking-tight text-[color:var(--color-text-primary)]">
                EcoMap <span className="text-[color:var(--color-accent)]">Invest</span>
              </span>
            </div>
          </button>

          <div className="hidden min-w-0 flex-1 justify-center md:flex">
            <div className="w-full max-w-[680px]">
              <MapSearchBar />
            </div>
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-2 md:gap-3">
          <div className="hidden md:block">
            <ThemeToggle />
          </div>

          <button
            type="button"
            onClick={handleLogout}
            className="inline-flex items-center gap-2 rounded-full border border-[color:var(--color-border)] bg-transparent px-3 py-2 text-sm font-semibold text-[color:var(--color-text-primary)] transition hover:bg-[color:rgba(234,240,255,0.04)]"
          >
            <LogoutIcon />
            <span className="hidden sm:inline">Logout</span>
          </button>

          <button
            type="button"
            onClick={() => setProfileOpen(true)}
            aria-label="Open profile panel"
            className="relative inline-flex h-11 w-11 items-center justify-center rounded-full border border-[color:var(--color-border)] bg-[color:var(--color-bg-elev)] text-sm font-extrabold text-[color:var(--color-text-primary)] shadow-sm transition hover:bg-[color:rgba(234,240,255,0.04)]"
          >
            {initials || "?"}
            <span className="absolute -right-0.5 -top-0.5 h-3.5 w-3.5 rounded-full border-2 border-[color:var(--color-bg-page)] bg-emerald-400" />
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

function BrandMark() {
  return (
    <span className="relative inline-flex h-9 w-9 items-center justify-center rounded-xl border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.03)]">
      <Image src={logo} alt="" className="h-6 w-6" priority />
      <span className="absolute -right-1 -top-1 h-2.5 w-2.5 rounded-full bg-emerald-400 ring-2 ring-[color:var(--color-bg-page)]" />
    </span>
  );
}
