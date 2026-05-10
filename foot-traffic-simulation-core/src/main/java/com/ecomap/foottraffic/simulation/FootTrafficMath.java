package com.ecomap.foottraffic.simulation;

import java.nio.charset.StandardCharsets;
import java.util.SplittableRandom;

/** Deterministic pseudo-random helpers for foot-traffic jitter. */
public final class FootTrafficMath {

    private FootTrafficMath() {}

    public static long murmurHash64(String h3Index, long salt) {
        byte[] data = (h3Index + ":" + salt).getBytes(StandardCharsets.UTF_8);
        return fmurMurHash64(data, 0L);
    }

    private static long fmurMurHash64(byte[] data, long seed) {
        final long m = 0xc6a4a7935bd1e995L;
        final int r = 47;
        long h = seed ^ (data.length * m);
        int len = data.length;
        int i = 0;
        while (len >= 8) {
            long k =
                    ((long) data[i] & 0xff)
                            | (((long) data[i + 1] & 0xff) << 8)
                            | (((long) data[i + 2] & 0xff) << 16)
                            | (((long) data[i + 3] & 0xff) << 24)
                            | (((long) data[i + 4] & 0xff) << 32)
                            | (((long) data[i + 5] & 0xff) << 40)
                            | (((long) data[i + 6] & 0xff) << 48)
                            | (((long) data[i + 7] & 0xff) << 56);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h ^= k;
            h *= m;
            i += 8;
            len -= 8;
        }
        switch (len) {
            case 7:
                h ^= ((long) data[i + 6] & 0xff) << 48;
                // fall through
            case 6:
                h ^= ((long) data[i + 5] & 0xff) << 40;
                // fall through
            case 5:
                h ^= ((long) data[i + 4] & 0xff) << 32;
                // fall through
            case 4:
                h ^= ((long) data[i + 3] & 0xff) << 24;
                // fall through
            case 3:
                h ^= ((long) data[i + 2] & 0xff) << 16;
                // fall through
            case 2:
                h ^= ((long) data[i + 1] & 0xff) << 8;
                // fall through
            case 1:
                h ^= (long) data[i] & 0xff;
                h *= m;
            default:
                break;
        }
        h ^= h >>> r;
        h *= m;
        h ^= h >>> r;
        return h;
    }

    public static double gaussian(long seed) {
        SplittableRandom rnd = new SplittableRandom(seed);
        double u1 = Math.nextUp(rnd.nextDouble());
        double u2 = rnd.nextDouble();
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }

    public static double jitterMultiplier(long noiseSeed, double noiseSigma) {
        double z = gaussian(noiseSeed);
        double j = 1.0 + noiseSigma * z;
        return Math.min(1.2, Math.max(0.8, j));
    }
}
