import L from "leaflet";
import iconRetinaUrl from "leaflet/dist/images/marker-icon-2x.png";
import iconUrl from "leaflet/dist/images/marker-icon.png";
import shadowUrl from "leaflet/dist/images/marker-shadow.png";

/**
 * Webpack and Turbopack resolve image imports to either a string URL or a module with `.src`.
 * Using `.src` alone breaks the other bundler and yields undefined → "iconUrl not set".
 */
function imageUrl(m: string | { src: string }): string {
  if (typeof m === "string") {
    return m;
  }
  if (m && typeof m === "object" && "src" in m) {
    return m.src;
  }
  return "";
}

delete (L.Icon.Default.prototype as { _getIconUrl?: unknown })._getIconUrl;

L.Icon.Default.mergeOptions({
  iconRetinaUrl: imageUrl(iconRetinaUrl as string | { src: string }),
  iconUrl: imageUrl(iconUrl as string | { src: string }),
  shadowUrl: imageUrl(shadowUrl as string | { src: string }),
});
