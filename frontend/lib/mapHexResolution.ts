/**
 * Coarser H3 resolution when zoomed out (fewer hex polygons). Simulation must stay at res 9
 * because sim score overrides are keyed by child h3Index.
 */
export function h3ResolutionForZoom(zoom: number, simulationMode: boolean): number {
  if (simulationMode) return 9;
  if (zoom <= 11) return 7;
  if (zoom <= 13) return 8;
  return 9;
}
