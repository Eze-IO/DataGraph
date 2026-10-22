package datagraph.dataset;

import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.List;

/**
 * A made-up dataset: what its dashboard shows, its columns, and how
 * to generate its rows. Each implementation is a Spring bean; they
 * appear in the navbar in @Order order.
 */
public interface SampleDataset {

    DatasetDefinition definition();

    /** Measure columns; the four standard columns are added in front. */
    StructType measures();

    List<Row> generate(SampleSettings settings);

    default StructType schema() {
        StructType schema = new StructType()
                .add("event_date", DataTypes.DateType, false)
                .add("segment", DataTypes.StringType, false)
                .add("group_name", DataTypes.StringType, false)
                .add("item", DataTypes.StringType, false);
        for (var field : measures().fields())
            schema = schema.add(field);
        return schema;
    }
}
