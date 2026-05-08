package com.example.backend.foottraffic.simulation;

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
        long total) {

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
        return new PoiTagCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
