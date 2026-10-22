package datagraph.sales;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregation queries over the orders table. Every method takes an
 * optional region; null means all regions.
 */
@Repository
public class SalesRepository {

    private final JdbcTemplate jdbc;

    public SalesRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Summary summary(String region) {
        Filter f = Filter.of(region);
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(revenue), 0) AS revenue, COUNT(*) AS orders, COALESCE(SUM(units), 0) AS units
                FROM orders""" + f.where(),
                (rs, i) -> {
                    BigDecimal revenue = rs.getBigDecimal("revenue");
                    long orders = rs.getLong("orders");
                    BigDecimal average = orders == 0 ? BigDecimal.ZERO
                            : revenue.divide(BigDecimal.valueOf(orders), 2, java.math.RoundingMode.HALF_UP);
                    return new Summary(revenue, orders, rs.getLong("units"), average);
                },
                f.args());
    }

    public List<LabeledValue> revenueByMonth(String region) {
        Filter f = Filter.of(region);
        return jdbc.query("""
                SELECT YEAR(order_date) AS y, MONTH(order_date) AS m, SUM(revenue) AS revenue
                FROM orders""" + f.where()
                + " GROUP BY YEAR(order_date), MONTH(order_date) ORDER BY y, m",
                (rs, i) -> new LabeledValue(String.format("%d-%02d", rs.getInt("y"), rs.getInt("m")),
                        rs.getBigDecimal("revenue")),
                f.args());
    }

    public List<LabeledValue> revenueByRegion() {
        return jdbc.query("""
                SELECT region, SUM(revenue) AS revenue FROM orders
                GROUP BY region ORDER BY revenue DESC""",
                (rs, i) -> new LabeledValue(rs.getString("region"), rs.getBigDecimal("revenue")));
    }

    public List<LabeledValue> revenueByCategory(String region) {
        Filter f = Filter.of(region);
        return jdbc.query("""
                SELECT category, SUM(revenue) AS revenue FROM orders""" + f.where()
                + " GROUP BY category ORDER BY revenue DESC",
                (rs, i) -> new LabeledValue(rs.getString("category"), rs.getBigDecimal("revenue")),
                f.args());
    }

    public List<LabeledValue> topProducts(String region, int limit) {
        Filter f = Filter.of(region);
        List<Object> args = new ArrayList<>(List.of(f.args()));
        args.add(limit);
        return jdbc.query("""
                SELECT product, SUM(revenue) AS revenue FROM orders""" + f.where()
                + " GROUP BY product ORDER BY revenue DESC LIMIT ?",
                (rs, i) -> new LabeledValue(rs.getString("product"), rs.getBigDecimal("revenue")),
                args.toArray());
    }

    //Optional WHERE clause; the region is always a bound parameter
    private record Filter(String region) {
        static Filter of(String region) {
            return new Filter(region == null || region.isBlank() ? null : region);
        }

        String where() {
            return region == null ? "" : " WHERE region = ?";
        }

        Object[] args() {
            return region == null ? new Object[0] : new Object[]{region};
        }
    }
}
