# DataGraph

A web dashboard that turns sales data into charts. It's a Spring Boot app that generates made-up sample sales for a fictional company, serves them as JSON, and draws them in the browser with [Chart.js](https://www.chartjs.org/).

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

Then open <http://localhost:8080>.

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

Every time the app starts, it generates about 3,800 orders into an in-memory [H2](https://www.h2database.com/) database. The data covers 24 months (October 2024 to September 2026) across four regions, four categories and twelve products. Sales grow over time, peak in November and December, and dip in January and February.

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
pom.xml                                Maven build (Spring Boot 4, Java 21)
src/main/java/datagraph/
  DataGraphApplication.java            App entry point
  sample/SampleDataGenerator.java      Generates the sample orders at startup
  sales/SalesRepository.java           SQL queries behind each chart
  sales/SalesController.java           JSON API
src/main/resources/
  application.properties               Database and sample-data settings
  schema.sql                           The orders table
  static/                              Dashboard page (index.html, app.js, styles.css)
src/test/java/                         API tests
```
