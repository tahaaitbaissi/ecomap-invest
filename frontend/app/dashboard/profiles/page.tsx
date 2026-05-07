"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Header from "@/components/profile/Header";
import ProfileRuleEditor from "@/components/profile/ProfileRuleEditor";
import Card from "@/components/ui/Card";
import Input from "@/components/ui/Input";
import PageShell from "@/components/ui/PageShell";
import { Button } from "@/components/ui/Button";
import { getToken } from "@/lib/auth";
import {
  archiveDynamicProfile,
  duplicateDynamicProfile,
  fetchProfileTags,
  fetchMyProfilesAsList,
  generateDynamicProfile,
  updateDynamicProfile,
  type DynamicProfileResponse,
  type ProfileTagOption,
  type TagWeightDto,
} from "@/services/api/profileService";
import { useStore } from "@/store/useStore";

interface RuleValidation {
  errors: Record<number, string | undefined>;
  emptyError?: string;
}

function validateRules(rules: TagWeightDto[], catalog: ProfileTagOption[]): RuleValidation {
  const supported = new Set(catalog.map((option) => option.tag));
  const seen = new Map<string, number>();
  const errors: Record<number, string | undefined> = {};

  if (rules.length === 0) {
    return { errors, emptyError: "Add at least one rule." };
  }

  rules.forEach((rule, index) => {
    if (!rule.tag || !supported.has(rule.tag)) {
      errors[index] = "Choose a supported tag from the catalog.";
      return;
    }
    if (!Number.isFinite(rule.weight) || rule.weight < 0.1 || rule.weight > 1.5) {
      errors[index] = "Weight must be between 0.1 and 1.5.";
      return;
    }
    if (seen.has(rule.tag)) {
      errors[index] = "Duplicate tag in this section. Remove one or adjust the existing rule.";
      const firstIndex = seen.get(rule.tag);
      if (firstIndex !== undefined && !errors[firstIndex]) {
        errors[firstIndex] = "Duplicate tag in this section.";
      }
      return;
    }
    seen.set(rule.tag, index);
  });

  return { errors };
}

function hasRuleErrors(validation: RuleValidation) {
  return Boolean(validation.emptyError || Object.values(validation.errors).some(Boolean));
}

function canonicalizeRules(rules: TagWeightDto[], catalog: ProfileTagOption[]) {
  if (catalog.length === 0) return rules;
  const aliases = new Map<string, string>();
  catalog.forEach((option) => {
    aliases.set(option.tag.toLowerCase(), option.tag);
    option.aliases.forEach((alias) => aliases.set(alias.toLowerCase(), option.tag));
  });

  const merged = new Map<string, TagWeightDto>();
  rules.forEach((rule) => {
    const canonicalTag = aliases.get(rule.tag.trim().toLowerCase());
    if (!canonicalTag || !Number.isFinite(rule.weight)) return;
    const existing = merged.get(canonicalTag);
    if (!existing || rule.weight > existing.weight) {
      merged.set(canonicalTag, { tag: canonicalTag, weight: rule.weight });
    }
  });
  return Array.from(merged.values());
}

export default function CommercialProfilesPage() {
  const router = useRouter();
  const profiles = useStore((s) => s.commercialProfiles);
  const setProfiles = useStore((s) => s.setCommercialProfiles);
  const upsertProfile = useStore((s) => s.upsertCommercialProfile);
  const removeProfile = useStore((s) => s.removeCommercialProfile);
  const profileId = useStore((s) => s.profileId);
  const setProfileId = useStore((s) => s.setProfileId);

  const [selectedId, setSelectedId] = useState<string | null>(profileId);
  const [query, setQuery] = useState("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [catalog, setCatalog] = useState<ProfileTagOption[]>([]);
  const [name, setName] = useState("");
  const [drivers, setDrivers] = useState<TagWeightDto[]>([]);
  const [competitors, setCompetitors] = useState<TagWeightDto[]>([]);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
    }
  }, [router]);

  useEffect(() => {
    setLoading(true);
    void Promise.all([fetchMyProfilesAsList(100), fetchProfileTags()])
      .then(([list, tagOptions]) => {
        setProfiles(list);
        setCatalog(tagOptions);
        setSelectedId((cur) => cur ?? list[0]?.id ?? null);
      })
      .catch(() => setError("Failed to load profiles or supported rule tags."))
      .finally(() => setLoading(false));
  }, [setProfiles]);

  const selected = useMemo(
    () => profiles.find((p) => p.id === selectedId) ?? profiles[0] ?? null,
    [profiles, selectedId],
  );

  useEffect(() => {
    if (!selected) {
      setName("");
      setDrivers([]);
      setCompetitors([]);
      return;
    }
    setName(selected.name || selected.userQuery);
    setDrivers(canonicalizeRules(selected.drivers, catalog));
    setCompetitors(canonicalizeRules(selected.competitors, catalog));
  }, [selected, catalog]);

  const driverValidation = useMemo(() => validateRules(drivers, catalog), [drivers, catalog]);
  const competitorValidation = useMemo(() => validateRules(competitors, catalog), [competitors, catalog]);

  const filtered = profiles.filter((profile) => {
    const term = search.trim().toLowerCase();
    if (!term) return true;
    return `${profile.name} ${profile.userQuery}`.toLowerCase().includes(term);
  });

  const generate = async () => {
    if (!query.trim()) return;
    setSaving(true);
    setError(null);
    try {
      const created = await generateDynamicProfile(query.trim());
      upsertProfile(created);
      setSelectedId(created.id);
      setQuery("");
    } catch {
      setError("Profile generation failed. Check Ollama/backend logs and try again.");
    } finally {
      setSaving(false);
    }
  };

  const save = async () => {
    if (!selected) return;
    const trimmedName = name.trim();
    if (!trimmedName) {
      setError("Profile name is required.");
      return;
    }
    if (hasRuleErrors(driverValidation) || hasRuleErrors(competitorValidation)) {
      setError("Fix the highlighted profile rules before saving.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const updated = await updateDynamicProfile(selected.id, {
        name: trimmedName,
        drivers,
        competitors,
      });
      upsertProfile(updated);
      setSelectedId(updated.id);
    } catch {
      setError("Could not save this profile. Check the selected tags and try again.");
    } finally {
      setSaving(false);
    }
  };

  const duplicate = async () => {
    if (!selected) return;
    setSaving(true);
    setError(null);
    try {
      const copy = await duplicateDynamicProfile(selected.id);
      upsertProfile(copy);
      setSelectedId(copy.id);
    } catch {
      setError("Could not duplicate this profile.");
    } finally {
      setSaving(false);
    }
  };

  const archive = async () => {
    if (!selected) return;
    if (!window.confirm(`Archive "${selected.name}"?`)) return;
    setSaving(true);
    setError(null);
    try {
      await archiveDynamicProfile(selected.id);
      removeProfile(selected.id);
      const next = profiles.find((p) => p.id !== selected.id) ?? null;
      setSelectedId(next?.id ?? null);
    } catch {
      setError("Could not archive this profile.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="min-h-screen bg-[color:var(--color-bg-page)]">
      <Header />
      <main className="py-6">
        <PageShell className="flex w-full flex-col gap-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[color:var(--color-text-muted)]">Dashboard</p>
            <h1 className="text-2xl font-extrabold text-[color:var(--color-text-primary)]">Commercial Profiles</h1>
            <p className="text-sm text-[color:var(--color-text-secondary)]">Manage the scoring profiles used by heatmaps and simulations.</p>
          </div>
          <Link href="/dashboard" className="ds-btn ds-btn-secondary">
            Back to map
          </Link>
        </div>

        {error && (
          <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</div>
        )}

        <section className="grid min-h-[620px] gap-5 lg:grid-cols-[360px_1fr]">
          <aside className="rounded-2xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
            <div className="mb-4 rounded-2xl border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.03)] p-4">
              <h2 className="mb-2 text-sm font-extrabold text-[color:var(--color-text-primary)]">Generate profile</h2>
              <textarea
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                rows={4}
                placeholder="Describe a business concept, e.g. pharmacy near clinics and hospitals"
                className="w-full resize-none rounded-xl border border-[color:var(--color-border)] bg-transparent p-3 text-sm text-[color:var(--color-text-primary)] outline-none placeholder:text-[color:var(--color-text-muted)] focus:ring-2 focus:ring-[color:rgba(47,107,255,0.35)]"
              />
              <button
                type="button"
                disabled={saving || !query.trim()}
                onClick={() => void generate()}
                className="ds-btn ds-btn-primary mt-3 w-full disabled:opacity-60"
              >
                {saving ? "Working..." : "Generate new profile"}
              </button>
            </div>

            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search profiles"
              className="mb-3"
            />

            <div className="space-y-2">
              {loading && <p className="text-sm text-[color:var(--color-text-secondary)]">Loading profiles...</p>}
              {!loading && filtered.length === 0 && (
                <p className="rounded-xl border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.03)] p-4 text-sm text-[color:var(--color-text-secondary)]">
                  No profiles yet. Generate your first scoring profile above.
                </p>
              )}
              {filtered.map((profile) => {
                const active = profile.id === profileId;
                const selectedRow = profile.id === selected?.id;
                return (
                  <button
                    key={profile.id}
                    type="button"
                    onClick={() => setSelectedId(profile.id)}
                    className={[
                      "w-full rounded-xl border p-3 text-left transition",
                      "border-[color:var(--color-border)]",
                      selectedRow
                        ? "bg-[color:rgba(47,107,255,0.12)]"
                        : "bg-[color:rgba(234,240,255,0.01)] hover:bg-[color:rgba(234,240,255,0.04)]",
                    ].join(" ")}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <span className="line-clamp-1 text-sm font-extrabold text-[color:var(--color-text-primary)]">{profile.name}</span>
                      {active && (
                        <span className="rounded-full border border-emerald-500/30 bg-emerald-500/10 px-2 py-0.5 text-[10px] font-extrabold text-emerald-300">
                          Active
                        </span>
                      )}
                    </div>
                    <p className="mt-1 line-clamp-2 text-xs text-[color:var(--color-text-secondary)]">{profile.userQuery}</p>
                    <p className="mt-2 text-[11px] text-[color:var(--color-text-muted)]">{new Date(profile.generatedAt).toLocaleString()}</p>
                  </button>
                );
              })}
            </div>
          </aside>

          <section className="rounded-2xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-5">
            {!selected ? (
              <div className="flex h-full items-center justify-center rounded-2xl border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.03)] p-8 text-center text-[color:var(--color-text-secondary)]">
                Select or generate a profile to edit its scoring rules.
              </div>
            ) : (
              <div className="flex h-full flex-col gap-5">
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[color:var(--color-border)] pb-4">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[color:var(--color-text-muted)]">Profile editor</p>
                    <h2 className="text-xl font-extrabold text-[color:var(--color-text-primary)]">{selected.name}</h2>
                    <p className="text-sm text-[color:var(--color-text-secondary)]">{selected.userQuery}</p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button type="button" onClick={() => setProfileId(selected.id)} className="ds-btn ds-btn-primary">
                      Use on map
                    </button>
                    <button type="button" onClick={() => void duplicate()} className="ds-btn ds-btn-secondary">
                      Duplicate
                    </button>
                    <button type="button" onClick={() => void archive()} className="ds-btn ds-btn-secondary border-red-500/30 text-red-300 hover:bg-red-500/10">
                      Archive
                    </button>
                  </div>
                </div>

                <label className="flex flex-col gap-2 text-sm font-semibold text-[color:var(--color-text-secondary)]">
                  Display name
                  <Input
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="font-normal"
                  />
                </label>

                <div className="rounded-2xl border border-[color:rgba(47,107,255,0.25)] bg-[color:rgba(47,107,255,0.10)] p-4 text-sm text-[color:var(--color-text-primary)]">
                  <p className="font-extrabold">How rules work</p>
                  <p className="mt-1">
                    Drivers are nearby anchors that increase demand. Competitors are similar or substitute businesses that reduce opportunity.
                    Weight controls influence: 0.1 weak, 1.0 normal, 1.5 strong.
                  </p>
                </div>

                <div className="grid gap-4 xl:grid-cols-2">
                  <ProfileRuleEditor
                    title="Drivers"
                    description="Things that increase demand near a hex."
                    rules={drivers}
                    onChange={setDrivers}
                    options={catalog}
                    errors={driverValidation.errors}
                    emptyError={driverValidation.emptyError}
                  />
                  <ProfileRuleEditor
                    title="Competitors"
                    description="Similar or substitute businesses that reduce opportunity."
                    rules={competitors}
                    onChange={setCompetitors}
                    options={catalog}
                    errors={competitorValidation.errors}
                    emptyError={competitorValidation.emptyError}
                  />
                </div>

                <div className="mt-auto flex justify-end">
                  <button
                    type="button"
                    disabled={saving}
                    onClick={() => void save()}
                    className="ds-btn ds-btn-primary px-5 py-2.5 text-sm disabled:opacity-60"
                  >
                    {saving ? "Saving..." : "Save changes"}
                  </button>
                </div>
              </div>
            )}
          </section>
        </section>
        </PageShell>
      </main>
    </div>
  );
}
