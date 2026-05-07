export type PoiCategory = "restaurants" | "retail" | "offices" | "entertain";

function norm(s: string): string {
  return s.toLowerCase().trim();
}

/**
 * Best-effort mapping from backend `typeTag` (often OSM-like strings) to a UI category.
 * If a tag is unknown, return null so the UI can choose a safe default (usually: keep visible).
 */
export function poiCategoryFromTypeTag(typeTag?: string | null): PoiCategory | null {
  if (!typeTag) return null;
  const t = norm(typeTag);

  // Common OSM-ish patterns: "amenity=restaurant", "shop=supermarket", "office=company"
  if (
    t.includes("restaurant") ||
    t.includes("fast_food") ||
    t.includes("cafe") ||
    t.includes("coffee") ||
    t.includes("bakery") ||
    t.includes("ice_cream")
  ) {
    return "restaurants";
  }

  if (
    t.includes("shop=") ||
    t.includes("retail") ||
    t.includes("supermarket") ||
    t.includes("mall") ||
    t.includes("convenience") ||
    t.includes("pharmacy") ||
    t.includes("clothes") ||
    t.includes("beauty") ||
    t.includes("hardware") ||
    t.includes("electronics") ||
    t.includes("furniture")
  ) {
    return "retail";
  }

  if (
    t.includes("office") ||
    t.includes("coworking") ||
    t.includes("company") ||
    t.includes("bank") ||
    t.includes("bureau") ||
    t.includes("consulting") ||
    t.includes("insurance")
  ) {
    return "offices";
  }

  if (
    t.includes("cinema") ||
    t.includes("theatre") ||
    t.includes("nightclub") ||
    t.includes("bar") ||
    t.includes("pub") ||
    t.includes("casino") ||
    t.includes("entertain")
  ) {
    return "entertain";
  }

  return null;
}

