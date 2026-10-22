package datagraph.dataset;

/**
 * A number to show, defined as a Spark SQL aggregate over the
 * dataset's columns, e.g. "SUM(revenue)". The SQL comes from code,
 * never from a request.
 */
public record Metric(String label, String sql, Format format) {}
