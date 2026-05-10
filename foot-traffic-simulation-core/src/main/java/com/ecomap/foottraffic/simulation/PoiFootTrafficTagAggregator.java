package com.ecomap.foottraffic.simulation;

import java.util.List;

/** Maps raw (type_tag, count) rows from PostGIS into {@link PoiTagCounts}. */
public final class PoiFootTrafficTagAggregator {

    private PoiFootTrafficTagAggregator() {}

    public static PoiTagCounts aggregate(List<Object[]> rows) {
        long transit = 0;
        long railwayStation = 0;
        long office = 0;
        long bank = 0;
        long hotel = 0;
        long shop = 0;
        long restaurant = 0;
        long cafe = 0;
        long bar = 0;
        long school = 0;
        long university = 0;
        long kindergarten = 0;
        long park = 0;
        long nature = 0;
        long residential = 0;
        long competitorApprox = 0;
        long workplaceMisc = 0;
        long total = 0;

        if (rows == null) {
            return PoiTagCounts.empty();
        }

        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }
            String tag = row[0] != null ? row[0].toString() : "";
            long cnt = toLong(row[1]);
            total += cnt;
            if (cnt <= 0 || tag.isEmpty()) {
                continue;
            }

            if (tag.startsWith("highway=bus_stop")
                    || tag.startsWith("public_transport=")
                    || tag.startsWith("amenity=bus_station")
                    || tag.startsWith("amenity=subway_entrance")) {
                transit += cnt;
            }
            if (tag.startsWith("railway=station")
                    || tag.startsWith("railway=halt")
                    || tag.startsWith("railway=subway_entrance")) {
                railwayStation += cnt;
                transit += cnt;
            }
            if (tag.startsWith("category=office")
                    || tag.equals("amenity=office")
                    || tag.startsWith("office=")) {
                office += cnt;
            }
            if (tag.startsWith("enterprise=")
                    || tag.startsWith("industrial=")
                    || tag.startsWith("craft=")
                    || tag.startsWith("landuse=industrial")
                    || tag.startsWith("landuse=commercial")) {
                workplaceMisc += cnt;
            }
            if (tag.startsWith("amenity=bank")) {
                bank += cnt;
            }
            if (tag.startsWith("tourism=hotel") || tag.startsWith("amenity=hotel")) {
                hotel += cnt;
            }
            if (tag.startsWith("shop=")) {
                shop += cnt;
            }
            if (tag.startsWith("shop=supermarket")) {
                competitorApprox += cnt;
            }
            if (tag.startsWith("amenity=restaurant")) {
                restaurant += cnt;
                competitorApprox += cnt;
            }
            if (tag.startsWith("amenity=cafe") || tag.startsWith("amenity=fast_food")) {
                cafe += cnt;
            }
            if (tag.startsWith("amenity=bar") || tag.startsWith("amenity=pub")) {
                bar += cnt;
            }
            if (tag.startsWith("amenity=school")) {
                school += cnt;
            }
            if (tag.startsWith("amenity=university") || tag.startsWith("amenity=college")) {
                university += cnt;
            }
            if (tag.startsWith("amenity=kindergarten")) {
                kindergarten += cnt;
            }
            if (tag.startsWith("leisure=park")) {
                park += cnt;
            }
            if (tag.startsWith("leisure=nature_reserve") || tag.startsWith("boundary=national_park")) {
                nature += cnt;
            }
            if (tag.startsWith("landuse=residential") || tag.startsWith("place=suburb")) {
                residential += cnt;
            }
        }

        return new PoiTagCounts(
                transit,
                railwayStation,
                office,
                bank,
                hotel,
                shop,
                restaurant,
                cafe,
                bar,
                school,
                university,
                kindergarten,
                park,
                nature,
                residential,
                competitorApprox,
                workplaceMisc,
                total);
    }

    private static long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
