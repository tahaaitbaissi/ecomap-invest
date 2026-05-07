import Script from "next/script";

/**
 * Runs before hydration to avoid theme flash.
 * Stores preference in localStorage under key: "ecomap_theme" ("system" | "light" | "dark").
 */
export default function ThemeScript() {
  const code = `
(function () {
  try {
    var key = "ecomap_theme";
    var pref = localStorage.getItem(key) || "system";
    var root = document.documentElement;

    function apply(theme) {
      if (theme === "light") root.dataset.theme = "light";
      else if (theme === "dark") root.dataset.theme = "dark";
      else root.dataset.theme = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }

    apply(pref);
  } catch (e) {}
})();`;
  return <Script id="ecomap-theme" strategy="beforeInteractive" dangerouslySetInnerHTML={{ __html: code }} />;
}

