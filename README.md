# DataGraph

A web dashboard that turns sales data into charts. It's a Spring Boot app that generates made-up sample sales for a fictional company, calculates the chart data with [Apache Spark](https://spark.apache.org/), serves it as JSON, and draws it in the browser with [Chart.js](https://www.chartjs.org/).

The dashboard shows:

- totals for revenue, orders, units sold and average order value
- revenue by month (line chart)
- revenue by region (bar chart)
- revenue by product category (doughnut chart)
- top products by revenue (horizontal bar chart)

A **Region** filter at the top updates every chart. It follows your system's light or dark mode and works on phones.

## Requirements

- **Java 21 or newer** (JDK). Check with `java -version`.
- **Maven 3.9+**. Check with `mvn -v`.

## Run it

```bash
mvn spring-boot:run
```

Then open <http://localhost:8080> once the log says `Started DataGraphApplication`. Startup takes 20–30 seconds because Spark starts inside the app and prepares its queries.

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

## How Spark is used

Spark runs inside the app in local mode, using all CPU cores; no cluster or separate install is needed.

1. At startup, the sample orders are loaded into a Spark DataFrame, cached in memory and registered as the `orders` view.
2. Each API call runs a Spark SQL query on that view (for example `SELECT region, SUM(revenue) … GROUP BY region`). The region filter is passed as a named parameter, never pasted into the SQL.
3. Every query is run once at startup, so Spark has already compiled it before the first visitor arrives.

To run the queries on a real Spark cluster instead, set `datagraph.spark.master` in [`application.properties`](src/main/resources/application.properties), for example `spark://host:7077`.

Spark needs access to some JDK internals on Java 17+. The build sets the required `--add-opens` flags for `mvn spring-boot:run` and `mvn test`, and writes them into the jar's manifest so `java -jar` works without extra flags. Because of Spark, the jar is about 300 MB.

## Sample data

Every time the app starts, it generates about 3,800 orders in memory. The data covers 24 months (October 2024 to September 2026) across four regions, four categories and twelve products. Sales grow over time, peak in November and December, and dip in January and February.

A fixed random seed means you get the same numbers on every run. You can change the seed, start month, number of months and orders per month in [`application.properties`](src/main/resources/application.properties).

## API

The dashboard reads from these JSON endpoints. Each one except `/api/regions` and `/api/revenue/by-region` accepts an optional `?region=North` (or South, East, West).

| Endpoint                   | Returns                                               |
|----------------------------|-------------------------------------------------------|
| `GET /api/regions`         | The list of regions                                   |
| `GET /api/summary`         | Revenue, orders, units and average order value        |
| `GET /api/revenue/monthly` | Revenue per month                                     |
| `GET /api/revenue/by-region` | Revenue per region (always all regions)             |
| `GET /api/revenue/by-category` | Revenue per product category                      |
| `GET /api/top-products`    | Top products by revenue (`?limit=5`, 1–50)            |

## Project layout

```
pom.xml                                Maven build (Spring Boot 4, Spark 4, Java 21)
src/main/java/datagraph/
  DataGraphApplication.java            App entry point
  spark/SparkConfig.java               Starts the Spark session
  sample/SampleDataGenerator.java      Generates the sample orders
  sales/SalesRepository.java           Loads orders into Spark; Spark SQL behind each chart
  sales/SalesController.java           JSON API
src/main/resources/
  application.properties               Spark and sample-data settings
  static/                              Dashboard page (index.html, app.js, styles.css)
src/test/java/                         API tests
```
