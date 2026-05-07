import { useCallback, useEffect, useState } from "react";
import type { ThemePreference } from "./themeTypes";

const KEY = "ecomap_theme";

function resolveSystem(): "light" | "dark" {
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

export function useThemePreference() {
  const [pref, setPref] = useState<ThemePreference>("system");
  const [effective, setEffective] = useState<"light" | "dark">("dark");

  useEffect(() => {
    const stored = (typeof window !== "undefined" ? localStorage.getItem(KEY) : null) as ThemePreference | null;
    const initial = stored === "light" || stored === "dark" || stored === "system" ? stored : "system";
    setPref(initial);
    setEffective(initial === "system" ? resolveSystem() : initial);
  }, []);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const mql = window.matchMedia("(prefers-color-scheme: dark)");
    const onChange = () => {
      setEffective(pref === "system" ? resolveSystem() : (pref as "light" | "dark"));
      if (pref === "system") {
        document.documentElement.dataset.theme = resolveSystem();
      }
    };
    mql.addEventListener?.("change", onChange);
    return () => mql.removeEventListener?.("change", onChange);
  }, [pref]);

  const apply = useCallback((p: ThemePreference) => {
    setPref(p);
    if (typeof window === "undefined") return;
    localStorage.setItem(KEY, p);
    const theme = p === "system" ? resolveSystem() : p;
    document.documentElement.dataset.theme = theme;
    setEffective(theme);
  }, []);

  return { preference: pref, effectiveTheme: effective, setPreference: apply };
}

