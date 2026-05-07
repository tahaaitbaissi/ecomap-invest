"use client";

import { Moon, Sun, Monitor } from "lucide-react";
import { useThemePreference } from "./useThemePreference";

export default function ThemeToggle() {
  const { preference, setPreference } = useThemePreference();
  return (
    <div className="inline-flex items-center rounded-full border border-[color:var(--color-border)] bg-transparent p-1">
      <button
        type="button"
        className={[
          "inline-flex items-center gap-2 rounded-full px-3 py-2 text-xs font-semibold transition",
          preference === "system"
            ? "bg-[color:rgba(234,240,255,0.06)] text-[color:var(--color-text-primary)]"
            : "text-[color:var(--color-text-secondary)] hover:bg-[color:rgba(234,240,255,0.04)] hover:text-[color:var(--color-text-primary)]",
        ].join(" ")}
        onClick={() => setPreference("system")}
        aria-label="Use system theme"
      >
        <Monitor className="h-4 w-4" />
        <span className="hidden sm:inline">System</span>
      </button>
      <button
        type="button"
        className={[
          "inline-flex items-center gap-2 rounded-full px-3 py-2 text-xs font-semibold transition",
          preference === "light"
            ? "bg-[color:rgba(234,240,255,0.06)] text-[color:var(--color-text-primary)]"
            : "text-[color:var(--color-text-secondary)] hover:bg-[color:rgba(234,240,255,0.04)] hover:text-[color:var(--color-text-primary)]",
        ].join(" ")}
        onClick={() => setPreference("light")}
        aria-label="Use light theme"
      >
        <Sun className="h-4 w-4" />
        <span className="hidden sm:inline">Light</span>
      </button>
      <button
        type="button"
        className={[
          "inline-flex items-center gap-2 rounded-full px-3 py-2 text-xs font-semibold transition",
          preference === "dark"
            ? "bg-[color:rgba(234,240,255,0.06)] text-[color:var(--color-text-primary)]"
            : "text-[color:var(--color-text-secondary)] hover:bg-[color:rgba(234,240,255,0.04)] hover:text-[color:var(--color-text-primary)]",
        ].join(" ")}
        onClick={() => setPreference("dark")}
        aria-label="Use dark theme"
      >
        <Moon className="h-4 w-4" />
        <span className="hidden sm:inline">Dark</span>
      </button>
    </div>
  );
}

