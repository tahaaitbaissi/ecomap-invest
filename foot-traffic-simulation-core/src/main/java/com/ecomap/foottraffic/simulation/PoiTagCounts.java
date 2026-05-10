package com.ecomap.foottraffic.simulation;

/** Aggregated POI counts inside one H3 cell for foot-traffic archetype classification. */
public record PoiTagCounts(
        long transitPoiCount,
        long railwayStationCount,
        long officeCount,
        long bankCount,
        long hotelCount,
        long shopCount,
        long restaurantCount,
        long cafeCount,
        long barCount,
        long schoolCount,
        long universityCount,
        long kindergartenCount,
        long parkCount,
        long natureReserveCount,
        long residentialCount,
        long competitorApprox,
        /** enterprise=, industrial=, craft=, landuse=industrial/commercial (workplace proxy). */
        long workplaceMiscCount,
        long total) {

    /** Office / bank / hotel / workshop-style tags (excludes pure retail food). */
    public long workplaceFootprintCount() {
        return officeCount + bankCount + hotelCount + workplaceMiscCount;
    }

    public long campusEducationCount() {
        return schoolCount + universityCount + kindergartenCount;
    }

    public long retailFoodCount() {
        return shopCount + restaurantCount + cafeCount + barCount;
    }

    public long cbdOfficeBankHotel() {
        return officeCount + bankCount + hotelCount;
    }

    public long parkNature() {
        return parkCount + natureReserveCount;
    }

    public static PoiTagCounts empty() {
        return new PoiTagCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
