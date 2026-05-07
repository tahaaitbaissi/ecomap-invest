import type { LatLngBounds } from "leaflet";
import L from "leaflet";

/** Same corners as backend app.hexagon.study-area (OSM Casablanca city admin bbox). */
export const STUDY_SW_LAT = 33.493342;
export const STUDY_SW_LNG = -7.751238;
export const STUDY_NE_LAT = 33.6409103;
export const STUDY_NE_LNG = -7.4574165;

/** Slight leaflet pan slack outside the pilot envelope only (not backend seed). */
const MAP_PAD_LAT = 0.012;
const MAP_PAD_LNG = 0.012;

export const CASA_CENTER: [number, number] = [33.5731, -7.5898];
export const INITIAL_ZOOM = 13;
export const MIN_ZOOM = 12;
export const MAX_ZOOM = 17;

export const MAX_BOUNDS: LatLngBounds = L.latLngBounds(
  [STUDY_SW_LAT - MAP_PAD_LAT, STUDY_SW_LNG - MAP_PAD_LNG],
  [STUDY_NE_LAT + MAP_PAD_LAT, STUDY_NE_LNG + MAP_PAD_LNG],
);

export function clampPointToPilot(lat: number, lng: number): [number, number] {
  const sw = MAX_BOUNDS.getSouthWest();
  const ne = MAX_BOUNDS.getNorthEast();
  return [
    Math.min(Math.max(lat, sw.lat), ne.lat),
    Math.min(Math.max(lng, sw.lng), ne.lng),
  ];
}

/** Intersect viewport bbox with pilot maxBounds before calling APIs. */
export function clampViewportBbox(bbox: {
  northEast: { lat: number; lng: number };
  southWest: { lat: number; lng: number };
}): {
  northEast: { lat: number; lng: number };
  southWest: { lat: number; lng: number };
} {
  const sw = MAX_BOUNDS.getSouthWest();
  const ne = MAX_BOUNDS.getNorthEast();
  const iSwLat = Math.max(bbox.southWest.lat, sw.lat);
  const iSwLng = Math.max(bbox.southWest.lng, sw.lng);
  const iNeLat = Math.min(bbox.northEast.lat, ne.lat);
  const iNeLng = Math.min(bbox.northEast.lng, ne.lng);
  if (iSwLat > iNeLat || iSwLng > iNeLng) {
    return {
      southWest: { lat: sw.lat, lng: sw.lng },
      northEast: { lat: ne.lat, lng: ne.lng },
    };
  }
  return {
    southWest: { lat: iSwLat, lng: iSwLng },
    northEast: { lat: iNeLat, lng: iNeLng },
  };
}
