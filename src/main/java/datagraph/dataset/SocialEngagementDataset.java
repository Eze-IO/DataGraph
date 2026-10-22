package datagraph.dataset;

import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static datagraph.dataset.SampleSettings.*;

/**
 * Posts by creators on fictional social media apps, with views,
 * likes and favorites. Audiences grow every month; videos get the
 * most views and polls the most likes per view.
 */
@Component
@Order(3)
public class SocialEngagementDataset implements SampleDataset {

    private static final List<String> PLATFORMS = List.of("Snapgram", "Clipster", "Chatter", "Threadly");
    private static final double[] PLATFORM_WEIGHTS = {0.32, 0.30, 0.22, 0.16};
    private static final Map<String, Double> PLATFORM_REACH = Map.of(
            "Snapgram", 1.2, "Clipster", 1.6, "Chatter", 0.7, "Threadly", 0.5);

    private record ContentType(String name, double weight, double typicalViews, double likeRate) {}

    private static final List<ContentType> CONTENT_TYPES = List.of(
            new ContentType("Video", 0.35, 18_000, 0.045),
            new ContentType("Photo", 0.35, 9_000, 0.060),
            new ContentType("Article", 0.18, 4_000, 0.030),
            new ContentType("Poll", 0.12, 6_000, 0.085)
    );
    private static final double[] CONTENT_WEIGHTS = CONTENT_TYPES.stream().mapToDouble(ContentType::weight).toArray();

    private static final List<String> CREATORS = List.of(
            "@chef.maya", "@trailrunner", "@pixelpete", "@greenthumb", "@techtess",
            "@vinylvic", "@fitwithfin", "@bookworm.bea", "@urbansketch", "@dadjokes.dan");

    private static final Metric VIEWS = new Metric("Views", "SUM(views)", Format.COUNT);
    private static final Metric LIKES = new Metric("Likes", "SUM(likes)", Format.COUNT);
    private static final Metric FAVORITES = new Metric("Favorites", "SUM(favorites)", Format.COUNT);

    private static final DatasetDefinition DEFINITION = new DatasetDefinition(
            "social", "Likes & favorites", "Social media",
            "Views, likes and favorites on creators' posts.",
            "Platform", PLATFORMS,
            List.of(VIEWS, LIKES, FAVORITES,
                    new Metric("Like rate", "try_divide(SUM(likes), SUM(views))", Format.PERCENT)),
            "Likes and favorites by month", List.of(LIKES, FAVORITES),
            new ChartSpec("Views by platform", VIEWS),
            new ChartSpec("Likes by content type", LIKES),
            new ChartSpec("Top creators", LIKES));

    @Override
    public DatasetDefinition definition() {
        return DEFINITION;
    }

    @Override
    public StructType measures() {
        return new StructType()
                .add("views", DataTypes.LongType, false)
                .add("likes", DataTypes.LongType, false)
                .add("favorites", DataTypes.LongType, false);
    }

    @Override
    public List<Row> generate(SampleSettings settings) {
        Random random = settings.random(DEFINITION.id());
        // Some creators are more popular than others
        double[] creatorPull = CREATORS.stream().mapToDouble(c -> 0.5 + random.nextDouble() * 1.5).toArray();
        List<Row> rows = new ArrayList<>();

        for (int m = 0; m < settings.months(); m++) {
            YearMonth month = settings.startMonth().plusMonths(m);
            double growth = Math.pow(1.04, m);
            int posts = (int) Math.round(260 * jitter(random, 0.1));

            for (int i = 0; i < posts; i++) {
                String platform = PLATFORMS.get(pickWeighted(random, PLATFORM_WEIGHTS));
                ContentType type = CONTENT_TYPES.get(pickWeighted(random, CONTENT_WEIGHTS));
                int creator = random.nextInt(CREATORS.size());
                // Views vary a lot from post to post; a few posts go viral
                double spread = Math.exp(random.nextGaussian() * 0.6) * (random.nextDouble() < 0.02 ? 8 : 1);
                long views = Math.round(type.typicalViews() * PLATFORM_REACH.get(platform) * creatorPull[creator] * growth * spread);
                long likes = Math.round(views * type.likeRate() * jitter(random, 0.3));
                long favorites = Math.round(likes * (0.12 + random.nextDouble() * 0.18));
                rows.add(RowFactory.create(Date.valueOf(randomDay(random, month)), platform, type.name(),
                        CREATORS.get(creator), views, likes, favorites));
            }
        }
        return rows;
    }
}
