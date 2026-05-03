"use client";

import { useEffect, useMemo, useState } from "react";
import type { CSSProperties, ReactNode } from "react";
import { getMyProfile, updateMyProfile } from "@/lib/api";

interface ProfilePanelProps {
  open: boolean;
  onClose: () => void;
  onProfileChange: (initials: string) => void;
}

interface FormState {
  companyName: string;
  email: string;
}

export default function ProfilePanel({ open, onClose, onProfileChange }: ProfilePanelProps) {
  const [toast, setToast] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState<FormState>({
    companyName: "",
    email: "",
  });

  const initials = useMemo(() => getInitials(form.companyName || form.email), [form.companyName, form.email]);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [onClose]);

  useEffect(() => {
    document.body.style.overflow = open ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [open]);

  useEffect(() => {
    if (!open) return;

    const loadProfile = async () => {
      setLoading(true);
      setError("");
      try {
        const profile = await getMyProfile();
        setForm({
          companyName: profile.companyName ?? "",
          email: profile.email,
        });
        onProfileChange(getInitials(profile.companyName ?? profile.email));
      } catch (e) {
        setError(e instanceof Error ? e.message : "Failed to load profile");
      } finally {
        setLoading(false);
      }
    };

    void loadProfile();
  }, [open, onProfileChange]);

  const set = (key: keyof FormState, value: string) => setForm((prev) => ({ ...prev, [key]: value }));

  const handleSave = async () => {
    setError("");
    setSaving(true);
    try {
      const updated = await updateMyProfile({
        companyName: form.companyName,
      });
      const nextInitials = getInitials(updated.companyName ?? updated.email);
      onProfileChange(nextInitials);
      onClose();
      setToast(true);
      setTimeout(() => setToast(false), 2800);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to save profile");
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div
        onClick={onClose}
        style={{
          position: "fixed",
          inset: 0,
          zIndex: 40,
          background: "rgba(15,23,42,0.45)",
          backdropFilter: "blur(4px)",
          opacity: open ? 1 : 0,
          pointerEvents: open ? "auto" : "none",
          transition: "opacity 0.2s ease",
        }}
      />

      <div
        style={{
          position: "fixed",
          top: "80px",
          right: "24px",
          zIndex: 50,
          width: "400px",
          maxHeight: "calc(100vh - 100px)",
          overflowY: "auto",
          background: "#ffffff",
          borderRadius: "20px",
          border: "1px solid #e2e8f0",
          boxShadow: "0 20px 60px rgba(26,86,219,0.18), 0 4px 16px rgba(0,0,0,0.08)",
          opacity: open ? 1 : 0,
          transform: open ? "translateY(0) scale(1)" : "translateY(-12px) scale(0.97)",
          pointerEvents: open ? "auto" : "none",
          transition: "opacity 0.22s cubic-bezier(.22,.68,0,1.2), transform 0.22s cubic-bezier(.22,.68,0,1.2)",
        }}
      >
        <div
          style={{
            background: "linear-gradient(135deg, #1a56db 0%, #3b82f6 60%, #818cf8 100%)",
            padding: "24px 22px 20px",
            position: "relative",
          }}
        >
          <button
            onClick={onClose}
            style={{
              position: "absolute",
              top: "14px",
              right: "14px",
              width: "30px",
              height: "30px",
              borderRadius: "50%",
              background: "rgba(255,255,255,0.2)",
              border: "none",
              color: "#fff",
              fontSize: "16px",
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            x
          </button>

          <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
            <div style={{ width: "64px", height: "64px", borderRadius: "50%", background: "rgba(255,255,255,0.25)", border: "3px solid rgba(255,255,255,0.6)", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 800, fontSize: "22px", color: "#fff" }}>
              {initials || "?"}
            </div>
            <div>
              <p style={{ color: "#fff", fontWeight: 700, fontSize: "17px", lineHeight: 1.2 }}>{form.companyName || "-"}</p>
              <p style={{ color: "rgba(255,255,255,0.72)", fontSize: "13px", marginTop: "4px" }}>{form.email || "-"}</p>
            </div>
          </div>
        </div>

        <Section title="Profile">
          {error && <p style={{ color: "#dc2626", fontSize: "12px", marginBottom: "8px" }}>{error}</p>}
          <Field label="Company name" value={form.companyName} onChange={(v) => set("companyName", v)} disabled={loading || saving} />
          <Field label="Email" type="email" value={form.email} onChange={() => {}} disabled />
        </Section>

        <div style={{ padding: "16px 22px 22px", display: "flex", gap: "10px" }}>
          <button
            onClick={onClose}
            style={{
              padding: "10px 18px",
              borderRadius: "999px",
              border: "1.5px solid #e2e8f0",
              background: "transparent",
              color: "#94a3b8",
              fontSize: "14px",
              cursor: "pointer",
            }}
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={loading || saving}
            style={{
              flex: 1,
              padding: "10px",
              borderRadius: "999px",
              background: "linear-gradient(135deg, #1a56db, #3b82f6)",
              color: "#fff",
              border: "none",
              fontWeight: 700,
              fontSize: "14px",
              cursor: "pointer",
              opacity: loading || saving ? 0.7 : 1,
            }}
          >
            {saving ? "Saving..." : "Save"}
          </button>
        </div>
      </div>

      <div
        style={{
          position: "fixed",
          bottom: "32px",
          left: "50%",
          transform: `translateX(-50%) translateY(${toast ? "0" : "20px"})`,
          background: "#16a34a",
          color: "#fff",
          padding: "11px 24px",
          borderRadius: "999px",
          fontWeight: 600,
          fontSize: "14px",
          boxShadow: "0 4px 20px rgba(22,163,74,0.35)",
          opacity: toast ? 1 : 0,
          pointerEvents: "none",
          transition: "opacity 0.25s, transform 0.25s",
          zIndex: 999,
          whiteSpace: "nowrap",
        }}
      >
        Profile updated successfully
      </div>
    </>
  );
}

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return "..";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0] ?? ""}${parts[1][0] ?? ""}`.toUpperCase();
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div style={{ padding: "18px 22px", borderBottom: "1px solid #e2e8f0" }}>
      <p style={{ fontSize: "11px", fontWeight: 700, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.07em", marginBottom: "14px" }}>
        {title}
      </p>
      <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>{children}</div>
    </div>
  );
}

function Field({ label, type = "text", value, onChange, disabled = false }: { label: string; type?: string; value: string; onChange: (v: string) => void; disabled?: boolean }) {
  return (
    <div>
      <label style={labelStyle}>{label}</label>
      <input
        type={type}
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value)}
        style={{ ...inputStyle, opacity: disabled ? 0.7 : 1 }}
      />
    </div>
  );
}

const labelStyle: CSSProperties = {
  display: "block",
  fontSize: "12px",
  fontWeight: 600,
  color: "#94a3b8",
  marginBottom: "5px",
};

const inputStyle: CSSProperties = {
  width: "100%",
  padding: "9px 12px",
  border: "1.5px solid #e2e8f0",
  borderRadius: "10px",
  fontSize: "14px",
  color: "#0f172a",
  background: "#f8faff",
  outline: "none",
  transition: "border-color 0.15s, box-shadow 0.15s, background 0.15s",
  fontFamily: "inherit",
};
