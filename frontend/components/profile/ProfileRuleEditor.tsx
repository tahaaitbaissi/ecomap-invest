"use client";

import { useMemo, useState } from "react";
import type { ProfileTagOption, TagWeightDto } from "@/services/api/profileService";

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
    <section className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="mb-3">
        <h3 className="text-sm font-bold text-slate-800">{title}</h3>
        <p className="text-xs text-slate-500">{description}</p>
      </div>

      <input
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        placeholder="Search supported tags"
        className="mb-3 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-blue-500"
      />

      <div className="space-y-3">
        {rules.map((rule, index) => {
          const selected = byTag.get(rule.tag);
          return (
            <div key={`${rule.tag}-${index}`} className="rounded-xl border border-slate-200 bg-white p-3">
              <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_120px_auto]">
                <label className="flex flex-col gap-1 text-xs font-semibold text-slate-600">
                  Rule tag
                  <select
                    value={rule.tag}
                    onChange={(e) => updateRule(index, { tag: e.target.value })}
                    className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-normal text-slate-800 outline-none focus:border-blue-500"
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

                <label className="flex flex-col gap-1 text-xs font-semibold text-slate-600">
                  Weight
                  <input
                    type="number"
                    min={0.1}
                    max={1.5}
                    step={0.1}
                    value={rule.weight}
                    onChange={(e) => updateRule(index, { weight: Number(e.target.value) })}
                    className="rounded-xl border border-slate-200 px-3 py-2 text-sm font-normal outline-none focus:border-blue-500"
                  />
                </label>

                <button
                  type="button"
                  onClick={() => removeRule(index)}
                  className="self-end rounded-xl border border-red-200 px-3 py-2 text-sm font-semibold text-red-600 hover:bg-red-50"
                >
                  Remove
                </button>
              </div>

              <div className="mt-2 flex flex-wrap items-center gap-2 text-xs">
                {selected && <span className="text-slate-500">{selected.description}</span>}
                <span className="rounded-full bg-slate-100 px-2 py-0.5 font-mono text-[11px] text-slate-500">
                  {rule.tag || "No tag selected"}
                </span>
              </div>

              {errors[index] && <p className="mt-2 text-xs font-semibold text-red-600">{errors[index]}</p>}
            </div>
          );
        })}
      </div>

      {emptyError && <p className="mt-3 text-xs font-semibold text-red-600">{emptyError}</p>}

      <button
        type="button"
        disabled={!fallbackTag}
        onClick={() => onChange([...rules, { tag: fallbackTag, weight: 1 }])}
        className="mt-3 rounded-xl border border-blue-200 bg-white px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50 disabled:border-slate-200 disabled:text-slate-400"
      >
        Add {title.toLowerCase().replace(/s$/, "")}
      </button>
    </section>
  );
}
