# DataGraph

A web dashboard that turns data from different industries into charts. It's a Spring Boot app that generates made-up sample data, calculates the charts with [Apache Spark](https://spark.apache.org/), and draws them in the browser with [Chart.js](https://www.chartjs.org/).

A navbar at the top switches between three datasets:

| Tab                   | Industry     | Shows                                                                 | Filter   |
|-----------------------|--------------|-----------------------------------------------------------------------|----------|
| **Customer Sales**    | Retail       | Revenue, orders, units, average order; revenue by month, region, category; top products | Region   |
| **Car Sales**         | Automotive   | Cars sold, revenue, average price, electric share; cars sold by month and body type; revenue by region; top models | Region   |
| **Likes & Favorites** | Social Media | Views, likes, favorites, like rate; likes and favorites by month; views by platform; likes by content type; top creators | Platform |

Each dashboard has four totals at the top and four charts: a line chart by month, a bar chart by region or platform, a doughnut chart by category, and a bar chart of the top items. The filter updates every chart; in the region/platform chart, the selected one is highlighted. Each tab has its own link (for example `/#cars`). The page follows your system's light or dark mode and works on phones.

## Requirements

- **Java 21 or newer** (JDK). Check with `java -version`.
- **Maven 3.9+**. Check with `mvn -v`.

## Run it

```bash
mvn spring-boot:run
```

Then open <http://localhost:8080> once the log says `Started DataGraphApplication`. Startup takes about 25 seconds because Spark starts inside the app and calculates the dashboards.

To build a runnable jar instead:

```bash
mvn package
java -jar target/datagraph-2.0.0.jar
```

To use a different port, add `--server.port=9090` after the jar name, or run `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090`.

## Run the tests

```bash
mvn test
```

## Sample data

Every time the app starts, it makes up the data in memory. A fixed random seed means you get the same numbers on every run. All three datasets cover 24 months (October 2024 to September 2026):

- **Customer Sales**: about 3,800 orders at an office and electronics store, across 4 regions, 4 categories and 12 products. Sales grow over time, peak in November and December, and dip in January and February.
- **Car Sales**: about 6,000 sales at a chain of dealerships, across 4 regions, 4 body types and 8 models. Sales are strongest in spring and December, and electric models grow from about 10% to 25% of sales.
- **Likes & Favorites**: about 6,100 posts by 10 creators on 4 fictional social apps, as videos, photos, articles and polls. Audiences grow about 4% a month, and a few posts go viral.

You can change the seed, start month and number of months in [`application.properties`](src/main/resources/application.properties).

## How Spark is used

Spark runs inside the app in local mode; no cluster or separate install is needed.

1. At startup, each dataset is loaded into a Spark DataFrame, cached in memory and registered as a view named after the dataset (`retail`, `cars`, `social`).
2. Each dashboard is calculated with Spark SQL queries built from the dataset's definition (for example `SELECT segment, SUM(revenue) … GROUP BY segment`). The filter value is passed as a named parameter, never pasted into the SQL.
3. The data doesn't change after startup, so each dashboard is calculated once and reused. The three unfiltered dashboards are calculated during startup; the 12 filtered ones are calculated in the background right after. Every request is then answered in a few milliseconds.

Spark is tuned for this small amount of data: two partitions per dataset, two shuffle partitions, adaptive query planning off, and a bigger cache for Spark's compiled code. To run the queries on a real Spark cluster instead, set `datagraph.spark.master` in [`application.properties`](src/main/resources/application.properties), for example `spark://host:7077`.

Spark needs access to some JDK internals on Java 17+. The build sets the required `--add-opens` flags for `mvn spring-boot:run` and `mvn test`, and writes them into the jar's manifest so `java -jar` works without extra flags. Because of Spark, the jar is about 300 MB.

## API

| Endpoint                                 | Returns                                                        |
|------------------------------------------|----------------------------------------------------------------|
| `GET /api/datasets`                      | The datasets in navbar order, with their filter name and values |
| `GET /api/datasets/{id}/dashboard`       | Totals and all four charts for one dataset                      |
| `GET /api/datasets/{id}/dashboard?segment=West` | The same, filtered to one region or platform             |

An unknown dataset returns 404; an unknown filter value returns 400.

## Adding a dataset

Each dataset is one class in [`src/main/java/datagraph/dataset`](src/main/java/datagraph/dataset) that implements `SampleDataset`:

1. `definition()`: its name, industry, filter, totals and charts. Each number is a Spark SQL aggregate, such as `SUM(views)`.
2. `measures()`: its number columns. Every dataset also has `event_date`, `segment` (region/platform), `group_name` (category) and `item` (product/model/creator).
3. `generate()`: makes up its rows.

Annotate it with `@Component` and `@Order(n)` to set its place in the navbar. The API, Spark queries and page pick it up automatically.

## Project layout

```
pom.xml                                   Maven build (Spring Boot 4, Spark 4, Java 21)
src/main/java/datagraph/
  DataGraphApplication.java               App entry point
  spark/SparkConfig.java                  Starts and tunes the Spark session
  dataset/                                The datasets and their definitions
    RetailSalesDataset.java               Customer Sales
    CarSalesDataset.java                  Car Sales
    SocialEngagementDataset.java          Likes & Favorites
  dashboard/DashboardRepository.java      Loads data into Spark; Spark SQL behind each dashboard
  dashboard/DashboardController.java      JSON API
src/main/resources/
  application.properties                  Spark and sample-data settings
  static/                                 Dashboard page (index.html, app.js, styles.css)
src/test/java/                            API and Spark tests
```
