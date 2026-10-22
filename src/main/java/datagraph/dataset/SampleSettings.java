package datagraph.dataset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Random;

/**
 * Shared settings and helpers for generating sample data. A fixed
 * seed makes every dataset come out the same on every start.
 */
@Component
public class SampleSettings {

    private final long seed;
    private final YearMonth startMonth;
    private final int months;

    public SampleSettings(@Value("${datagraph.sample.seed:42}") long seed,
                          @Value("${datagraph.sample.start-month:2024-10}") String startMonth,
                          @Value("${datagraph.sample.months:24}") int months) {
        this.seed = seed;
        this.startMonth = YearMonth.parse(startMonth);
        this.months = months;
    }

    /** A random generator for one dataset; different datasets get different streams. */
    public Random random(String datasetId) {
        return new Random(seed * 31 + datasetId.hashCode());
    }

    public YearMonth startMonth() {
        return startMonth;
    }

    public int months() {
        return months;
    }

    public static LocalDate randomDay(Random random, YearMonth month) {
        return month.atDay(1 + random.nextInt(month.lengthOfMonth()));
    }

    /** Picks an index with the given probabilities (which should add up to 1). */
    public static int pickWeighted(Random random, double[] weights) {
        double r = random.nextDouble();
        for (int i = 0; i < weights.length; i++) {
            r -= weights[i];
            if (r <= 0)
                return i;
        }
        return weights.length - 1;
    }

    /** A value around 1 that varies by up to the given fraction either way. */
    public static double jitter(Random random, double fraction) {
        return 1 - fraction + random.nextDouble() * 2 * fraction;
    }
}
