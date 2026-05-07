"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import Card from "@/components/ui/Card";
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
    <div className="px-3">
      <div className="flex flex-col gap-2">
        <Card className="p-3">
          <div className="mb-2 text-[11px] font-extrabold text-[color:var(--color-text-muted)]">Active profile</div>
        {profiles.length > 0 ? (
          <select
            value={profileId ?? ""}
            onChange={(e) => setProfileId(e.target.value || null)}
            className="w-full rounded-[12px] border border-[color:var(--color-border)] bg-transparent px-3 py-2 text-[13px] font-semibold text-[color:var(--color-text-primary)] outline-none focus:ring-2 focus:ring-[color:rgba(47,107,255,0.35)]"
          >
            {profiles.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name || p.userQuery}
              </option>
            ))}
          </select>
        ) : (
          <p className="text-[12px] leading-relaxed text-[color:var(--color-text-secondary)]">
            {loading ? "Loading profiles..." : "No scoring profile yet."}
          </p>
        )}
        {selected && (
          <p className="mt-2 text-[11px] leading-relaxed text-[color:var(--color-text-muted)]">
            {selected.drivers.length} drivers · {selected.competitors.length} competitors
          </p>
        )}
        </Card>
        <Link href="/dashboard/profiles" className="ds-btn ds-btn-primary w-full">
          Manage profiles
        </Link>
      </div>
    </div>
  );
}
