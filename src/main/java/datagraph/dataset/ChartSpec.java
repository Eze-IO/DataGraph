package datagraph.dataset;

/** A chart that shows one metric broken down by a column. */
public record ChartSpec(String title, Metric metric) {}
