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
 * Sales at a fictional chain of car dealerships. Sales are strongest
 * in spring and at the year-end push, and electric models gain
 * share every month.
 */
@Component
@Order(2)
public class CarSalesDataset implements SampleDataset {

    private static final List<String> REGIONS = List.of("North", "South", "East", "West");
    private static final double[] REGION_WEIGHTS = {0.22, 0.34, 0.24, 0.20};

    private record Model(String bodyType, String name, double price) {}

    private static final List<Model> MODELS = List.of(
            new Model("SUV", "Summit", 42_500),
            new Model("SUV", "Trailhead", 36_900),
            new Model("Sedan", "Metro", 27_400),
            new Model("Sedan", "Avenue", 33_800),
            new Model("Truck", "Ridgeline Pro", 51_200),
            new Model("Truck", "Haul", 39_900),
            new Model("Electric", "Volta", 46_700),
            new Model("Electric", "Spark Mini", 31_500)
    );

    private static final Metric CARS_SOLD = new Metric("Cars sold", "SUM(units)", Format.COUNT);
    private static final Metric REVENUE = new Metric("Revenue", "SUM(revenue)", Format.MONEY);

    private static final DatasetDefinition DEFINITION = new DatasetDefinition(
            "cars", "Car sales", "Automotive",
            "New-car sales across a chain of dealerships.",
            "Region", REGIONS,
            List.of(CARS_SOLD, REVENUE,
                    new Metric("Average price", "try_divide(SUM(revenue), SUM(units))", Format.MONEY),
                    new Metric("Electric share", "try_divide(SUM(CASE WHEN group_name = 'Electric' THEN units ELSE 0 END), SUM(units))", Format.PERCENT)),
            "Cars sold by month", List.of(CARS_SOLD),
            new ChartSpec("Revenue by region", REVENUE),
            new ChartSpec("Cars sold by body type", CARS_SOLD),
            new ChartSpec("Top models", CARS_SOLD));

    @Override
    public DatasetDefinition definition() {
        return DEFINITION;
    }

    @Override
    public StructType measures() {
        return new StructType()
                .add("units", DataTypes.IntegerType, false)
                .add("revenue", DataTypes.createDecimalType(14, 2), false);
    }

    @Override
    public List<Row> generate(SampleSettings settings) {
        Random random = settings.random(DEFINITION.id());
        List<Row> rows = new ArrayList<>();

        for (int m = 0; m < settings.months(); m++) {
            YearMonth month = settings.startMonth().plusMonths(m);
            int sales = (int) Math.round(220 * (1 + 0.01 * m) * seasonality(month.getMonthValue()) * jitter(random, 0.08));
            // Electric models start at about 10% of sales and grow to about 25%
            double electricShare = 0.10 + 0.15 * m / Math.max(1, settings.months() - 1);

            for (int i = 0; i < sales; i++) {
                Model model = pickModel(random, electricShare);
                // Most sales are one car; some are small fleet orders
                int units = random.nextDouble() < 0.92 ? 1 : 2 + random.nextInt(4);
                BigDecimal price = BigDecimal.valueOf(model.price() * jitter(random, 0.06)).setScale(2, RoundingMode.HALF_UP);
                rows.add(RowFactory.create(Date.valueOf(randomDay(random, month)),
                        REGIONS.get(pickWeighted(random, REGION_WEIGHTS)), model.bodyType(), model.name(),
                        units, price.multiply(BigDecimal.valueOf(units))));
            }
        }
        return rows;
    }

    private static Model pickModel(Random random, double electricShare) {
        boolean electric = random.nextDouble() < electricShare;
        List<Model> pool = MODELS.stream()
                .filter(model -> model.bodyType().equals("Electric") == electric)
                .toList();
        return pool.get(random.nextInt(pool.size()));
    }

    //Spring buying season and the year-end sales push; slow winter
    private static double seasonality(int monthOfYear) {
        return switch (monthOfYear) {
            case 3, 4, 5 -> 1.20;
            case 12 -> 1.25;
            case 1, 2 -> 0.75;
            default -> 1.0;
        };
    }
}
