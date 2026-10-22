package datagraph.dashboard;

import datagraph.dataset.Format;

import java.math.BigDecimal;
import java.util.List;

/** Everything one dashboard view shows, calculated for one dataset and optional segment. */
public record Dashboard(
        String dataset,
        String segment,
        List<Kpi> kpis,
        Trend trend,
        Breakdown bySegment,
        Breakdown byGroup,
        Breakdown topItems) {

    public record Kpi(String label, Format format, BigDecimal value) {}

    public record Trend(String title, Format format, List<String> labels, List<Series> series) {}

    public record Series(String label, List<BigDecimal> values) {}

    public record Breakdown(String title, Format format, List<LabeledValue> data) {}

    /** One bar or slice: a label (region, model, creator…) and its value. */
    public record LabeledValue(String label, BigDecimal value) {}
}
