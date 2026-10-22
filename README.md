# DataGraph

A desktop SQL Server query client built with JavaFX. Log in to a database, run SQL (typed or opened from a `.sql` file), view the results in a table, and export them to CSV.

DataGraph began in 2018 as *Zeal.Application*, a class project by Zeal Productivity Solutions for CIS3365. In 2026 it was renamed and modernized to build with Maven on Java 21.

## Requirements

- **Java 21 or newer** (JDK). Check with `java -version`.
- **Maven 3.9+**. Check with `mvn -v`.

You don't need to install JavaFX, the SQL Server driver or opencsv separately: Maven downloads them.

## Build and run

Run straight from source:

```bash
mvn javafx:run
```

Build a single runnable jar (includes all dependencies):

```bash
mvn package
java -jar target/datagraph-1.0.0-all.jar
```

The bundled jar contains the JavaFX files for the operating system it was built on. Build it on each OS you want to run it on (macOS, Windows, Linux).

## Database settings

The default connection is set in [`src/main/resources/datagraph.properties`](src/main/resources/datagraph.properties):

| Key          | Meaning                             | Default                                 |
|--------------|-------------------------------------|-----------------------------------------|
| `db.host`    | SQL Server host name                | `localhost`                             |
| `db.port`    | Port                                | `1433`                                  |
| `db.name`    | Database name                       | `master`                                |
| `db.options` | Extra JDBC connection options       | `encrypt=true;trustServerCertificate=true` |

To use a different server without rebuilding, either:

- put a `datagraph.properties` file with your values in the folder you run the app from (it's git-ignored), or
- pass them on the command line, e.g. `java -Ddb.host=localhost -Ddb.name=MyDb -jar target/datagraph-1.0.0-all.jar`.

You can also change the host, port and database name at runtime from the gear icon on the login screen.

## Project layout

```
pom.xml                         Maven build
src/main/java/datagraph         Java sources (Main, core/, ui/)
src/main/resources              FXML screens, images, default datagraph.properties
instructions.txt / .html        Original end-user instructions
.misc/                          Screenshots
```

## Opening in an IDE

Open the folder in IntelliJ IDEA, Eclipse or VS Code and import it as a Maven project. The IDE picks up the dependencies from `pom.xml`.
