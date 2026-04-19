"use client";

import { useState } from "react";
import { createProfile, type ProfileDto } from "@/services/api/profileService";
import { useStore } from "@/store/useStore";

export default function ProfileInput() {
  const setProfileId = useStore((s) => s.setProfileId);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [profile, setProfile] = useState<ProfileDto | null>(null);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    if (!query.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const p = await createProfile(query.trim());
      setProfile(p);
      setProfileId(p.id);
    } catch {
      setError("Profil généré avec valeurs par défaut");
      const mock: ProfileDto = {
        id: "local-" + Date.now(),
        userQuery: query,
        generatedAt: new Date().toISOString(),
        drivers: [{ tag: "résidentiel", weight: 0.8 }, { tag: "flux_piétons", weight: 0.72 }],
        competitors: [{ tag: query.toLowerCase().split(" ")[0] ?? "commerce", weight: 0.6 }],
      };
      setProfile(mock);
      setProfileId(mock.id);
    } finally {
      setLoading(false);
    }
  };

  const reset = () => { setProfile(null); setQuery(""); setError(null); setProfileId(null); };

  if (profile) return <ProfileCard profile={profile} onReset={reset} />;

  return (
    <div>
      <textarea
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Décrivez votre projet commercial (ex: Boulangerie bio haut de gamme)"
        rows={3}
        style={{ width: "100%", padding: "9px 10px", fontSize: "12px", borderRadius: "10px", border: "1.5px solid #e2e8f0", resize: "none", outline: "none", background: "#f8fafc", color: "#374151", lineHeight: 1.5, boxSizing: "border-box" }}
      />
      {error && <div style={{ fontSize: "11px", color: "#ef4444", marginTop: "4px", padding: "6px 10px", background: "#fef2f2", borderRadius: "8px" }}>⚠️ {error}</div>}
      <button
        onClick={submit}
        disabled={loading || !query.trim()}
        style={{ width: "100%", marginTop: "8px", padding: "9px", background: loading || !query.trim() ? "#94a3b8" : "#1a56db", color: "#fff", border: "none", borderRadius: "10px", fontSize: "12.5px", fontWeight: 600, cursor: loading || !query.trim() ? "not-allowed" : "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: "8px" }}
      >
        {loading && <Spinner />}
        {loading ? "Analyse en cours..." : "Générer le profil"}
      </button>
    </div>
  );
}

function ProfileCard({ profile, onReset }: { profile: ProfileDto; onReset: () => void }) {
  return (
    <div style={{ background: "#f8fafc", borderRadius: "12px", padding: "12px", border: "1px solid #e2e8f0" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "8px" }}>
        <span style={{ fontSize: "11px", fontWeight: 700, color: "#1e293b" }}>Profil actif</span>
        <button onClick={onReset} style={{ fontSize: "10px", color: "#1a56db", background: "none", border: "none", cursor: "pointer", fontWeight: 600 }}>Nouveau</button>
      </div>
      <p style={{ fontSize: "11px", color: "#64748b", marginBottom: "10px", lineHeight: 1.4 }}>{profile.userQuery.slice(0, 80)}{profile.userQuery.length > 80 ? "…" : ""}</p>
      {profile.drivers.length > 0 && (
        <div style={{ marginBottom: "8px" }}>
          <p style={{ fontSize: "9px", fontWeight: 700, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: "5px" }}>Drivers</p>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "4px" }}>
            {profile.drivers.map((d) => <span key={d.tag} style={{ padding: "2px 8px", background: "#dcfce7", color: "#166534", borderRadius: "20px", fontSize: "11px", fontWeight: 600 }}>{d.tag} {Math.round(d.weight * 100)}%</span>)}
          </div>
        </div>
      )}
      {profile.competitors.length > 0 && (
        <div>
          <p style={{ fontSize: "9px", fontWeight: 700, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: "5px" }}>Concurrents</p>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "4px" }}>
            {profile.competitors.map((c) => <span key={c.tag} style={{ padding: "2px 8px", background: "#fee2e2", color: "#991b1b", borderRadius: "20px", fontSize: "11px", fontWeight: 600 }}>{c.tag} {Math.round(c.weight * 100)}%</span>)}
          </div>
        </div>
      )}
    </div>
  );
}

function Spinner() {
  return (
    <>
      <style>{`@keyframes _spin { to { transform: rotate(360deg); } }`}</style>
      <span style={{ width: "12px", height: "12px", border: "2px solid rgba(255,255,255,0.4)", borderTop: "2px solid white", borderRadius: "50%", animation: "_spin 0.7s linear infinite", display: "inline-block", flexShrink: 0 }} />
    </>
  );
}
