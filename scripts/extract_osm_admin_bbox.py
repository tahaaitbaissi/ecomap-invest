#!/usr/bin/env python3
"""
Print YAML/env snippet for app.hexagon.study-area from an OSM administrative place.

Equivalent to GDAL `GetEnvelope()` on the admin polygon Ring (converted from Overpass/OSM):
  xmin, xmax, ymin, ymax  → swLng, neLng, swLat, neLat in WGS84.

This script uses Nominatim (same polygon source as OSM) so we do not need a local GDAL install.
For a Shapefile workflow: ogrinfo -so -geom=YES Layer.shp LayerName → bbox, or GDAL Python/osgeo.ogr GetEnvelope().
"""
from __future__ import annotations

import argparse
import json
import ssl
import sys
import urllib.parse
import urllib.request

NOMINATIM = "https://nominatim.openstreetmap.org/search"


def fetch_bbox_from_nominatim(params: dict[str, str]) -> tuple[float, float, float, float, str, str]:
    """Return (sw_lng, sw_lat, ne_lng, ne_lat, osm_comment, label) from Nominatim boundingbox."""
    q = urllib.parse.urlencode({**params, "format": "jsonv2", "limit": "1"})
    req = urllib.request.Request(
        f"{NOMINATIM}?{q}",
        headers={"User-Agent": "EcoMapInvest-study-bbox-script/1.0 (education)"},
    )
    ctx = ssl.create_default_context()
    with urllib.request.urlopen(req, context=ctx, timeout=30) as r:
        rows = json.load(r)
    if not rows:
        raise SystemExit(f"No results for Nominatim params: {params!r}")
    bb = rows[0].get("boundingbox")
    if not bb or len(bb) != 4:
        raise SystemExit(f"Unexpected response: {rows[0]!r}")
    # [min_latitude, max_latitude, min_longitude, max_longitude]
    sw_lat, ne_lat, sw_lng, ne_lng = (float(bb[0]), float(bb[1]), float(bb[2]), float(bb[3]))
    osm_info = ""
    if "osm_type" in rows[0]:
        osm_info = f"# OSM {rows[0]['osm_type']} {rows[0].get('osm_id')}\n"
    name = rows[0].get("display_name") or rows[0].get("name")
    return sw_lng, sw_lat, ne_lng, ne_lat, osm_info, name


def main() -> None:
    p = argparse.ArgumentParser(
        description="Print study-area bounds for application.yml (OSM admin bbox via Nominatim or OGR-equivalent envelope)."
    )
    p.add_argument(
        "-q",
        "--query",
        help="Free-text Nominatim search (use with care; try --city for admin city boundary).",
    )
    p.add_argument(
        "--city",
        default="Casablanca",
        help="City for structured search (default: Casablanca)",
    )
    p.add_argument(
        "--country",
        default="Morocco",
        help="Country for structured search (default: Morocco)",
    )
    args = p.parse_args()
    if args.query:
        params = {"q": args.query}
    else:
        params = {
            "city": args.city,
            "country": args.country,
            "featuretype": "city",
        }
    sw_lng, sw_lat, ne_lng, ne_lat, osm_info, name = fetch_bbox_from_nominatim(params)
    if osm_info:
        print(osm_info, end="")
    if name:
        print(f'# "{name}"\n')
    print("app.hexagon.study-area (Spring YAML fragment):")
    print(f"  sw-lng: {sw_lng}")
    print(f"  sw-lat: {sw_lat}")
    print(f"  ne-lng: {ne_lng}")
    print(f"  ne-lat: {ne_lat}")
    print()
    print("Docker env overrides:")
    print(f"  HEX_STUDY_SW_LNG={sw_lng}")
    print(f"  HEX_STUDY_SW_LAT={sw_lat}")
    print(f"  HEX_STUDY_NE_LNG={ne_lng}")
    print(f"  HEX_STUDY_NE_LAT={ne_lat}")


if __name__ == "__main__":
    main()
    sys.exit(0)
