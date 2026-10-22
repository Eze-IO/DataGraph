package datagraph.dashboard;

import datagraph.dataset.DatasetDefinition;

import java.util.List;

/** What the navbar and filter need to know about a dataset. */
public record DatasetInfo(String id, String name, String industry, String description,
                          String segmentLabel, List<String> segments) {

    static DatasetInfo of(DatasetDefinition d) {
        return new DatasetInfo(d.id(), d.name(), d.industry(), d.description(), d.segmentLabel(), d.segments());
    }
}
