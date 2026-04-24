"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import ProfilePanel from "@/components/profile/ProfilePanel";
import { clearToken } from "@/lib/auth";
import { getMyProfile } from "@/lib/api";

export default function Header() {
  const router = useRouter();
  const [profileOpen, setProfileOpen] = useState(false);
  const [initials, setInitials] = useState("..");

  useEffect(() => {
    const loadProfile = async () => {
      try {
        const profile = await getMyProfile();
        setInitials(getInitials(profile.name));
      } catch {
        clearToken();
        router.replace("/login");
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
      <header style={{ height: "72px", padding: "0 48px", backgroundColor: "#1a56db" }} className="sticky top-0 z-30 w-full flex items-center justify-between">
        <button onClick={() => router.push("/dashboard")} className="flex items-center shrink-0" style={{ background: "transparent", border: "none", cursor: "pointer" }}>
          <Image
            src="/logoNoBg.svg"
            alt="EcoMap Invest"
            width={220}
            height={64}
            style={{ height: "62px", width: "auto", objectFit: "contain" }}
            priority
            unoptimized
          />
        </button>

        <div className="flex items-center" style={{ gap: "16px" }}>
          <div
            className="flex items-center"
            style={{
              gap: "10px",
              backgroundColor: "#166534",
              color: "#fff",
              fontSize: "15px",
              fontWeight: 600,
              padding: "9px 20px",
              borderRadius: "999px",
              userSelect: "none",
              whiteSpace: "nowrap",
            }}
          >
            <span
              style={{ width: "10px", height: "10px", borderRadius: "50%", backgroundColor: "#4ade80", flexShrink: 0, display: "inline-block" }}
            />
            Connected
          </div>

          <button
            onClick={handleLogout}
            className="flex items-center hover:opacity-80 transition-opacity"
            style={{
              gap: "8px",
              color: "#fff",
              fontSize: "15px",
              fontWeight: 500,
              padding: "9px 20px",
              borderRadius: "999px",
              border: "1.5px solid rgba(255,255,255,0.4)",
              background: "transparent",
              cursor: "pointer",
              whiteSpace: "nowrap",
            }}
          >
            <LogoutIcon />
            Logout
          </button>

          <button
            onClick={() => setProfileOpen(true)}
            aria-label="Open profile panel"
            className="flex items-center justify-center hover:scale-105 transition-transform"
            style={{
              position: "relative",
              width: "44px",
              height: "44px",
              borderRadius: "50%",
              background: "linear-gradient(135deg, #38bdf8, #6366f1)",
              border: "2.5px solid rgba(255,255,255,0.7)",
              color: "#fff",
              fontWeight: 700,
              fontSize: "14px",
              cursor: "pointer",
              flexShrink: 0,
              outline: "none",
            }}
          >
            {initials || "?"}
            <span
              style={{
                position: "absolute",
                top: "-1px",
                right: "-1px",
                width: "13px",
                height: "13px",
                backgroundColor: "#4ade80",
                borderRadius: "50%",
                border: "2.5px solid #1a56db",
              }}
            />
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
