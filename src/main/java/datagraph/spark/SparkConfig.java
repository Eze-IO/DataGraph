package datagraph.spark;

import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Runs Spark inside the app (no cluster needed). The master URL can
 * point at a real cluster later, e.g. spark://host:7077.
 */
@Configuration
public class SparkConfig {

    @Bean(destroyMethod = "stop")
    public SparkSession sparkSession(@Value("${datagraph.spark.master:local[*]}") String master,
                                     @Value("${datagraph.spark.shuffle-partitions:2}") int shufflePartitions) {
        return SparkSession.builder()
                .appName("DataGraph")
                .master(master)
                // The sample data is small (a few thousand rows per dataset), so Spark's defaults
                // for big data mostly add coordination overhead: 200 shuffle partitions, and
                // adaptive planning that re-plans every stage
                .config("spark.sql.shuffle.partitions", shufflePartitions)
                .config("spark.sql.adaptive.enabled", false)
                // Spark compiles code for every query stage; the default cache of 100 compiled
                // classes is too small for all the dashboards' queries, causing recompiles
                .config("spark.sql.codegen.cache.maxEntries", 2000)
                .config("spark.ui.enabled", false)
                .getOrCreate();
    }
}
