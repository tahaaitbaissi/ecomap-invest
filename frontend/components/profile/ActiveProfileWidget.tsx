"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { fetchMyProfilesAsList } from "@/services/api/profileService";
import { useStore } from "@/store/useStore";

export default function ActiveProfileWidget() {
  const profiles = useStore((s) => s.commercialProfiles);
  const selected = useStore((s) => s.selectedCommercialProfile);
  const profileId = useStore((s) => s.profileId);
  const setProfileId = useStore((s) => s.setProfileId);
  const setProfiles = useStore((s) => s.setCommercialProfiles);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (profiles.length > 0) return;
    setLoading(true);
    void fetchMyProfilesAsList(50)
      .then(setProfiles)
      .catch(() => setProfiles([]))
      .finally(() => setLoading(false));
  }, [profiles.length, setProfiles]);

  return (
    <div style={{ padding: "0 12px", display: "flex", flexDirection: "column", gap: "8px" }}>
      <div style={{ border: "1px solid #e2e8f0", borderRadius: "12px", background: "#f8fafc", padding: "10px" }}>
        <div style={{ fontSize: "11px", fontWeight: 700, color: "#1e293b", marginBottom: "6px" }}>
          Active profile
        </div>
        {profiles.length > 0 ? (
          <select
            value={profileId ?? ""}
            onChange={(e) => setProfileId(e.target.value || null)}
            style={{
              width: "100%",
              border: "1px solid #cbd5e1",
              borderRadius: "8px",
              padding: "7px",
              fontSize: "12px",
              color: "#334155",
              background: "#fff",
            }}
          >
            {profiles.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name || p.userQuery}
              </option>
            ))}
          </select>
        ) : (
          <p style={{ fontSize: "11px", color: "#64748b", lineHeight: 1.5 }}>
            {loading ? "Loading profiles..." : "No scoring profile yet."}
          </p>
        )}
        {selected && (
          <p style={{ marginTop: "7px", fontSize: "10px", color: "#64748b", lineHeight: 1.4 }}>
            {selected.drivers.length} drivers · {selected.competitors.length} competitors
          </p>
        )}
      </div>
      <Link
        href="/dashboard/profiles"
        style={{
          display: "block",
          textAlign: "center",
          borderRadius: "10px",
          background: "#1a56db",
          color: "#fff",
          padding: "8px 10px",
          fontSize: "12px",
          fontWeight: 700,
          textDecoration: "none",
        }}
      >
        Manage profiles
      </Link>
    </div>
  );
}
