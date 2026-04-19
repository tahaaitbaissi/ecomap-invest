"use client";

import { useEffect, useState } from "react";
import { getMyProfiles, type ProfileDto } from "@/services/api/profileService";
import { useStore } from "@/store/useStore";

export default function ProfileHistory() {
  const setProfileId = useStore((s) => s.setProfileId);
  const [profiles, setProfiles] = useState<ProfileDto[]>([]);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    getMyProfiles()
      .then((p) => setProfiles(p))
      .catch(() => setProfiles([]));
  }, []);

  if (profiles.length === 0) return null;

  return (
    <div style={{ position: "relative" }}>
      <button
        onClick={() => setOpen((v) => !v)}
        style={{ width: "100%", padding: "7px 10px", borderRadius: "8px", border: "1px solid #e2e8f0", background: "#f8fafc", color: "#374151", fontSize: "12px", cursor: "pointer", display: "flex", justifyContent: "space-between", alignItems: "center" }}
      >
        <span>Historique ({profiles.length})</span>
        <span style={{ fontSize: "9px" }}>{open ? "▲" : "▼"}</span>
      </button>
      {open && (
        <div style={{ position: "absolute", top: "100%", left: 0, right: 0, zIndex: 50, background: "#fff", border: "1px solid #e2e8f0", borderRadius: "8px", boxShadow: "0 8px 24px rgba(0,0,0,0.1)", marginTop: "4px", maxHeight: "160px", overflowY: "auto" }}>
          {profiles.map((p) => (
            <button
              key={p.id}
              onClick={() => { setProfileId(p.id); setOpen(false); }}
              style={{ width: "100%", padding: "8px 10px", border: "none", background: "none", textAlign: "left", cursor: "pointer", fontSize: "11px", color: "#374151", borderBottom: "1px solid #f1f5f9", display: "block" }}
              onMouseEnter={(e) => { e.currentTarget.style.background = "#f8fafc"; }}
              onMouseLeave={(e) => { e.currentTarget.style.background = "none"; }}
            >
              <div style={{ fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                {p.userQuery.slice(0, 50)}{p.userQuery.length > 50 ? "…" : ""}
              </div>
              <div style={{ fontSize: "10px", color: "#94a3b8", marginTop: "2px" }}>
                {new Date(p.generatedAt).toLocaleDateString("fr-FR")}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
