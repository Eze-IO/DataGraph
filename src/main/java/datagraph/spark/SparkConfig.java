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
                                     @Value("${datagraph.spark.shuffle-partitions:4}") int shufflePartitions) {
        return SparkSession.builder()
                .appName("DataGraph")
                .master(master)
                // The data is small; the default of 200 partitions would only add overhead
                .config("spark.sql.shuffle.partitions", shufflePartitions)
                .config("spark.ui.enabled", false)
                .getOrCreate();
    }
}
