package datagraph.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fills the orders table with made-up sales for a fictional company.
 * A fixed seed makes the numbers the same on every start. Sales grow
 * slowly over time, peak in November and December, and dip in
 * January and February so the charts have a realistic shape.
 */
@Component
public class SampleDataGenerator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataGenerator.class);

    public static final List<String> REGIONS = List.of("North", "South", "East", "West");
    private static final double[] REGION_WEIGHTS = {0.30, 0.20, 0.32, 0.18};

    private record Product(String category, String name, double price, int maxUnits) {}

    private static final List<Product> PRODUCTS = List.of(
            new Product("Electronics", "Laptop", 1199.00, 3),
            new Product("Electronics", "Monitor", 279.00, 4),
            new Product("Electronics", "Headphones", 149.00, 5),
            new Product("Furniture", "Standing Desk", 549.00, 2),
            new Product("Furniture", "Office Chair", 329.00, 4),
            new Product("Furniture", "Bookshelf", 159.00, 3),
            new Product("Office Supplies", "Paper (case)", 45.00, 20),
            new Product("Office Supplies", "Printer Ink", 64.00, 10),
            new Product("Office Supplies", "Notebook Pack", 18.00, 25),
            new Product("Software", "Design Suite", 599.00, 5),
            new Product("Software", "Antivirus", 79.00, 15),
            new Product("Software", "Cloud Storage", 120.00, 10)
    );

    private final JdbcTemplate jdbc;
    private final long seed;
    private final YearMonth startMonth;
    private final int months;
    private final int ordersPerMonth;

    public SampleDataGenerator(JdbcTemplate jdbc,
                               @Value("${datagraph.sample.seed:42}") long seed,
                               @Value("${datagraph.sample.start-month:2024-10}") String startMonth,
                               @Value("${datagraph.sample.months:24}") int months,
                               @Value("${datagraph.sample.orders-per-month:120}") int ordersPerMonth) {
        this.jdbc = jdbc;
        this.seed = seed;
        this.startMonth = YearMonth.parse(startMonth);
        this.months = months;
        this.ordersPerMonth = ordersPerMonth;
    }

    @Override
    public void run(ApplicationArguments args) {
        Random random = new Random(seed);
        List<Object[]> rows = new ArrayList<>();

        for (int m = 0; m < months; m++) {
            YearMonth month = startMonth.plusMonths(m);
            double growth = 1 + 0.025 * m;
            int orders = (int) Math.round(ordersPerMonth * growth * seasonality(month.getMonthValue())
                    * (0.9 + random.nextDouble() * 0.2));

            for (int i = 0; i < orders; i++) {
                LocalDate date = month.atDay(1 + random.nextInt(month.lengthOfMonth()));
                String region = REGIONS.get(pickWeighted(random, REGION_WEIGHTS));
                Product product = PRODUCTS.get(random.nextInt(PRODUCTS.size()));
                int units = 1 + random.nextInt(product.maxUnits());
                BigDecimal unitPrice = BigDecimal.valueOf(product.price() * (0.9 + random.nextDouble() * 0.2))
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal revenue = unitPrice.multiply(BigDecimal.valueOf(units));

                rows.add(new Object[]{Date.valueOf(date), region, product.category(), product.name(),
                        units, unitPrice, revenue});
            }
        }

        jdbc.batchUpdate("""
                INSERT INTO orders (order_date, region, category, product, units, unit_price, revenue)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, rows);
        log.info("Generated {} sample orders from {} to {}", rows.size(), startMonth, startMonth.plusMonths(months - 1));
    }

    //Holiday peak at the end of the year, slow start to the year
    private static double seasonality(int monthOfYear) {
        return switch (monthOfYear) {
            case 11 -> 1.30;
            case 12 -> 1.45;
            case 1, 2 -> 0.80;
            case 6, 7 -> 0.95;
            default -> 1.0;
        };
    }

    private static int pickWeighted(Random random, double[] weights) {
        double r = random.nextDouble();
        for (int i = 0; i < weights.length; i++) {
            r -= weights[i];
            if (r <= 0)
                return i;
        }
        return weights.length - 1;
    }
}
