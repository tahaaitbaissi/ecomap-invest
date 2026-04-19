export interface GeocodingResult {
  displayName: string;
  lat: number;
  lng: number;
}

export async function searchAddress(query: string): Promise<GeocodingResult[]> {
  const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=5&countrycodes=ma`;
  const res = await fetch(url, { headers: { "Accept-Language": "fr" } });
  if (!res.ok) return [];
  const data = await res.json();
  return data.map((item: { display_name: string; lat: string; lon: string }) => ({
    displayName: item.display_name,
    lat: parseFloat(item.lat),
    lng: parseFloat(item.lon),
  }));
}
