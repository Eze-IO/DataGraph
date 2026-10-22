package datagraph.sales;

import java.math.BigDecimal;

/** One point on a chart: a label (month, region, product…) and its value. */
public record LabeledValue(String label, BigDecimal value) {}
