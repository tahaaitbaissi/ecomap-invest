"use client";

import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { getMyProfile, updateMyProfile } from "@/lib/api";
import Input from "@/components/ui/Input";

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
        className={[
          "fixed inset-0 z-[var(--app-shell-z-popover)] bg-black/50 backdrop-blur-sm transition-opacity",
          open ? "opacity-100 pointer-events-auto" : "opacity-0 pointer-events-none",
        ].join(" ")}
      />

      <div
        className={[
          "fixed right-6 top-[80px] z-[calc(var(--app-shell-z-popover)+1)] w-[400px] max-w-[calc(100vw-48px)] overflow-auto",
          "max-h-[calc(100vh-100px)] rounded-[20px] border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)]",
          "shadow-[0_20px_60px_rgba(0,0,0,0.25)] transition",
          open ? "opacity-100 translate-y-0 scale-100 pointer-events-auto" : "opacity-0 -translate-y-3 scale-[0.97] pointer-events-none",
        ].join(" ")}
      >
        <div
          className="relative rounded-t-[20px] border-b border-[color:rgba(234,240,255,0.08)] bg-[color:rgba(47,107,255,0.15)] px-[22px] pb-5 pt-6"
        >
          <button
            onClick={onClose}
            className="absolute right-3 top-3 inline-flex h-8 w-8 items-center justify-center rounded-full border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.04)] text-[color:var(--color-text-primary)] hover:bg-[color:rgba(234,240,255,0.06)]"
          >
            ×
          </button>

          <div className="flex items-center gap-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-full border border-[color:rgba(234,240,255,0.22)] bg-[color:rgba(234,240,255,0.04)] text-[22px] font-extrabold text-[color:var(--color-text-primary)]">
              {initials || "?"}
            </div>
            <div>
              <p className="text-[17px] font-extrabold leading-tight text-[color:var(--color-text-primary)]">{form.companyName || "-"}</p>
              <p className="mt-1 text-[13px] text-[color:var(--color-text-secondary)]">{form.email || "-"}</p>
            </div>
          </div>
        </div>

        <Section title="Profile">
          {error && <p className="text-[12px] text-red-300">{error}</p>}
          <Field label="Company name" value={form.companyName} onChange={(v) => set("companyName", v)} disabled={loading || saving} />
          <Field label="Email" type="email" value={form.email} onChange={() => {}} disabled />
        </Section>

        <div className="flex gap-3 px-[22px] pb-[22px] pt-4">
          <button
            onClick={onClose}
            className="ds-btn ds-btn-secondary"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={loading || saving}
            className="ds-btn ds-btn-primary flex-1 disabled:opacity-60"
          >
            {saving ? "Saving..." : "Save"}
          </button>
        </div>
      </div>

      <div
        className={[
          "fixed bottom-8 left-1/2 z-[999] -translate-x-1/2 rounded-full border border-emerald-500/30 bg-emerald-500/12",
          "px-6 py-3 text-[14px] font-semibold text-emerald-200 shadow-[0_8px_30px_rgba(16,185,129,0.22)] transition",
          toast ? "opacity-100 translate-y-0" : "pointer-events-none translate-y-5 opacity-0",
        ].join(" ")}
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
    <div className="border-b border-[color:var(--color-border)] px-[22px] py-[18px]">
      <p className="mb-3 text-[11px] font-extrabold uppercase tracking-[0.07em] text-[color:var(--color-text-muted)]">
        {title}
      </p>
      <div className="flex flex-col gap-3">{children}</div>
    </div>
  );
}

function Field({ label, type = "text", value, onChange, disabled = false }: { label: string; type?: string; value: string; onChange: (v: string) => void; disabled?: boolean }) {
  return (
    <div>
      <label className="mb-1 block text-[12px] font-semibold text-[color:var(--color-text-secondary)]">{label}</label>
      <Input
        type={type}
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value)}
        className={disabled ? "opacity-70" : ""}
      />
    </div>
  );
}
