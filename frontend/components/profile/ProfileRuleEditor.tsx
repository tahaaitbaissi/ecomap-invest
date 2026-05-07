"use client";

import { useMemo, useState } from "react";
import type { ProfileTagOption, TagWeightDto } from "@/services/api/profileService";
import Input from "@/components/ui/Input";

type RuleErrorMap = Record<number, string | undefined>;

interface ProfileRuleEditorProps {
  title: string;
  description: string;
  rules: TagWeightDto[];
  onChange: (rules: TagWeightDto[]) => void;
  options: ProfileTagOption[];
  errors?: RuleErrorMap;
  emptyError?: string;
}

function groupedOptions(options: ProfileTagOption[]) {
  return options.reduce<Record<string, ProfileTagOption[]>>((acc, option) => {
    const group = option.group || "Other";
    acc[group] = acc[group] ?? [];
    acc[group].push(option);
    return acc;
  }, {});
}

export default function ProfileRuleEditor({
  title,
  description,
  rules,
  onChange,
  options,
  errors = {},
  emptyError,
}: ProfileRuleEditorProps) {
  const [filter, setFilter] = useState("");
  const filteredOptions = useMemo(() => {
    const term = filter.trim().toLowerCase();
    const selectedTags = new Set(rules.map((rule) => rule.tag));
    if (!term) return options;
    return options.filter((option) =>
      selectedTags.has(option.tag) ||
      `${option.label} ${option.tag} ${option.group} ${option.description}`.toLowerCase().includes(term),
    );
  }, [filter, options, rules]);
  const byTag = new Map(options.map((option) => [option.tag, option]));
  const grouped = groupedOptions(filteredOptions);
  const fallbackTag = options[0]?.tag ?? "";

  const updateRule = (index: number, patch: Partial<TagWeightDto>) => {
    onChange(rules.map((rule, i) => (i === index ? { ...rule, ...patch } : rule)));
  };

  const removeRule = (index: number) => {
    onChange(rules.filter((_, i) => i !== index));
  };

  return (
    <section className="rounded-2xl border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.03)] p-4">
      <div className="mb-3">
        <h3 className="text-sm font-extrabold text-[color:var(--color-text-primary)]">{title}</h3>
        <p className="text-xs text-[color:var(--color-text-secondary)]">{description}</p>
      </div>

      <Input
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        placeholder="Search supported tags"
        className="mb-3"
      />

      <div className="space-y-3">
        {rules.map((rule, index) => {
          const selected = byTag.get(rule.tag);
          return (
            <div key={`${rule.tag}-${index}`} className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-3">
              <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_120px_auto]">
                <label className="flex flex-col gap-1 text-xs font-semibold text-[color:var(--color-text-secondary)]">
                  Rule tag
                  <select
                    value={rule.tag}
                    onChange={(e) => updateRule(index, { tag: e.target.value })}
                    className="rounded-xl border border-[color:var(--color-border)] bg-transparent px-3 py-2 text-sm font-normal text-[color:var(--color-text-primary)] outline-none focus:ring-2 focus:ring-[color:rgba(47,107,255,0.35)]"
                  >
                    {!selected && rule.tag && (
                      <option value={rule.tag} disabled>
                        Unsupported ({rule.tag})
                      </option>
                    )}
                    {Object.entries(grouped).map(([group, items]) => (
                      <optgroup key={group} label={group}>
                        {items.map((option) => (
                          <option key={option.tag} value={option.tag}>
                            {option.label} ({option.tag})
                          </option>
                        ))}
                      </optgroup>
                    ))}
                  </select>
                </label>

                <label className="flex flex-col gap-1 text-xs font-semibold text-[color:var(--color-text-secondary)]">
                  Weight
                  <input
                    type="number"
                    min={0.1}
                    max={1.5}
                    step={0.1}
                    value={rule.weight}
                    onChange={(e) => updateRule(index, { weight: Number(e.target.value) })}
                    className="rounded-xl border border-[color:var(--color-border)] bg-transparent px-3 py-2 text-sm font-normal text-[color:var(--color-text-primary)] outline-none focus:ring-2 focus:ring-[color:rgba(47,107,255,0.35)]"
                  />
                </label>

                <button
                  type="button"
                  onClick={() => removeRule(index)}
                  className="self-end rounded-xl border border-red-500/30 px-3 py-2 text-sm font-semibold text-red-300 hover:bg-red-500/10"
                >
                  Remove
                </button>
              </div>

              <div className="mt-2 flex flex-wrap items-center gap-2 text-xs">
                {selected && <span className="text-[color:var(--color-text-secondary)]">{selected.description}</span>}
                <span className="rounded-full border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.03)] px-2 py-0.5 font-mono text-[11px] text-[color:var(--color-text-muted)]">
                  {rule.tag || "No tag selected"}
                </span>
              </div>

              {errors[index] && <p className="mt-2 text-xs font-semibold text-red-300">{errors[index]}</p>}
            </div>
          );
        })}
      </div>

      {emptyError && <p className="mt-3 text-xs font-semibold text-red-300">{emptyError}</p>}

      <button
        type="button"
        disabled={!fallbackTag}
        onClick={() => onChange([...rules, { tag: fallbackTag, weight: 1 }])}
        className="ds-btn ds-btn-secondary mt-3"
      >
        Add {title.toLowerCase().replace(/s$/, "")}
      </button>
    </section>
  );
}
