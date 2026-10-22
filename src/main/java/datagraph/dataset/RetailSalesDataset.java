package datagraph.dataset;

import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static datagraph.dataset.SampleSettings.*;

/**
 * Customer orders at a fictional office and electronics retailer.
 * Sales grow slowly, peak in November and December, and dip in
 * January and February.
 */
@Component
@Order(1)
public class RetailSalesDataset implements SampleDataset {

    private static final List<String> REGIONS = List.of("North", "South", "East", "West");
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

    private static final Metric REVENUE = new Metric("Revenue", "SUM(revenue)", Format.MONEY);

    private static final DatasetDefinition DEFINITION = new DatasetDefinition(
            "retail", "Customer Sales", "Retail",
            "Orders from customers of an office and electronics store.",
            "Region", REGIONS,
            List.of(REVENUE,
                    new Metric("Orders", "COUNT(*)", Format.COUNT),
                    new Metric("Units sold", "SUM(units)", Format.COUNT),
                    new Metric("Average order", "try_divide(SUM(revenue), COUNT(*))", Format.MONEY)),
            "Revenue by month", List.of(REVENUE),
            new ChartSpec("Revenue by region", REVENUE),
            new ChartSpec("Revenue by category", REVENUE),
            new ChartSpec("Top products", REVENUE));

    @Override
    public DatasetDefinition definition() {
        return DEFINITION;
    }

    @Override
    public StructType measures() {
        return new StructType()
                .add("units", DataTypes.IntegerType, false)
                .add("unit_price", DataTypes.createDecimalType(10, 2), false)
                .add("revenue", DataTypes.createDecimalType(12, 2), false);
    }

    @Override
    public List<Row> generate(SampleSettings settings) {
        Random random = settings.random(DEFINITION.id());
        List<Row> rows = new ArrayList<>();

        for (int m = 0; m < settings.months(); m++) {
            YearMonth month = settings.startMonth().plusMonths(m);
            int orders = (int) Math.round(120 * (1 + 0.025 * m) * seasonality(month.getMonthValue()) * jitter(random, 0.1));

            for (int i = 0; i < orders; i++) {
                Product product = PRODUCTS.get(random.nextInt(PRODUCTS.size()));
                int units = 1 + random.nextInt(product.maxUnits());
                BigDecimal unitPrice = BigDecimal.valueOf(product.price() * jitter(random, 0.1))
                        .setScale(2, RoundingMode.HALF_UP);
                rows.add(RowFactory.create(Date.valueOf(randomDay(random, month)),
                        REGIONS.get(pickWeighted(random, REGION_WEIGHTS)), product.category(), product.name(),
                        units, unitPrice, unitPrice.multiply(BigDecimal.valueOf(units))));
            }
        }
        return rows;
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
}
