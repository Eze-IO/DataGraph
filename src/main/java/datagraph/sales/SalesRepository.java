package datagraph.sales;

import datagraph.sample.SampleDataGenerator;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Chart data, calculated by Spark SQL over the sample orders.
 * The orders are loaded once into a cached Spark DataFrame and
 * registered as the "orders" view. Every method takes an optional
 * region; null means all regions.
 */
@Repository
public class SalesRepository {

    private static final Logger log = LoggerFactory.getLogger(SalesRepository.class);

    private final SparkSession spark;

    public SalesRepository(SparkSession spark, SampleDataGenerator generator) {
        this.spark = spark;
        Dataset<Row> orders = spark.createDataFrame(generator.generateOrders(), SampleDataGenerator.ORDERS_SCHEMA);
        orders.cache().createOrReplaceTempView("orders");
        log.info("Loaded {} sample orders into Spark", orders.count());
        warmUp();
    }

    //Spark compiles each query the first time it runs; do that now so the first page load is fast
    private void warmUp() {
        for (String region : new String[]{null, SampleDataGenerator.REGIONS.get(0)}) {
            summary(region);
            revenueByMonth(region);
            revenueByCategory(region);
            topProducts(region, 5);
        }
        revenueByRegion();
    }

    public Summary summary(String region) {
        Row row = query("""
                SELECT COALESCE(SUM(revenue), 0) AS revenue, COUNT(*) AS orders, COALESCE(SUM(units), 0) AS units
                FROM orders""", region, "").first();
        BigDecimal revenue = row.getDecimal(0);
        long orders = row.getLong(1);
        BigDecimal average = orders == 0 ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
        return new Summary(revenue, orders, row.getLong(2), average);
    }

    public List<LabeledValue> revenueByMonth(String region) {
        return toLabeledValues(query("""
                SELECT date_format(order_date, 'yyyy-MM') AS month, SUM(revenue) AS revenue
                FROM orders""", region, " GROUP BY month ORDER BY month"));
    }

    public List<LabeledValue> revenueByRegion() {
        return toLabeledValues(query("""
                SELECT region, SUM(revenue) AS revenue FROM orders""", null, " GROUP BY region ORDER BY revenue DESC"));
    }

    public List<LabeledValue> revenueByCategory(String region) {
        return toLabeledValues(query("""
                SELECT category, SUM(revenue) AS revenue FROM orders""", region, " GROUP BY category ORDER BY revenue DESC"));
    }

    public List<LabeledValue> topProducts(String region, int limit) {
        return toLabeledValues(query("""
                SELECT product, SUM(revenue) AS revenue FROM orders""", region, " GROUP BY product ORDER BY revenue DESC")
                .limit(limit));
    }

    //Adds an optional region filter; the region is always a named parameter, never pasted into the SQL
    private Dataset<Row> query(String select, String region, String rest) {
        if (region == null || region.isBlank())
            return spark.sql(select + rest);
        return spark.sql(select + " WHERE region = :region" + rest, Map.<String, Object>of("region", region));
    }

    private static List<LabeledValue> toLabeledValues(Dataset<Row> result) {
        return result.collectAsList().stream()
                .map(row -> new LabeledValue(row.getString(0), row.getDecimal(1)))
                .toList();
    }
}
