package datagraph.dataset;

import java.util.List;

/**
 * Describes one dataset and its dashboard. Every dataset's rows have
 * the same four descriptive columns (event_date, segment, group_name,
 * item) plus its own measure columns that the metrics refer to.
 *
 * @param id           used in URLs, e.g. "cars"
 * @param segmentLabel what the filter and the segment chart are called, e.g. "Region"
 * @param segments     the possible segment values, in display order
 * @param kpis         the totals shown in tiles at the top
 * @param trendTitle   title of the by-month line chart
 * @param trend        one or more metrics drawn as lines by month (same format)
 * @param bySegment    bar chart by segment (always shows every segment)
 * @param byGroup      doughnut chart by group_name
 * @param topItems     horizontal bar chart of the top items
 */
public record DatasetDefinition(
        String id,
        String name,
        String industry,
        String description,
        String segmentLabel,
        List<String> segments,
        List<Metric> kpis,
        String trendTitle,
        List<Metric> trend,
        ChartSpec bySegment,
        ChartSpec byGroup,
        ChartSpec topItems) {}
