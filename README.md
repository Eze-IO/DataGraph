# DataGraph

A desktop SQL Server query client built with JavaFX. Connect to a database, run SQL (typed or opened from a `.sql` file), view the results in a table, and export them to CSV.

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

## Using DataGraph

1. Start the app. It opens on the query screen and shows the **Connect** dialog.
2. Enter the host, port, database, username and password, then click **Connect**. If the connection fails, the error is shown in the dialog so you can fix it and try again. The title bar shows which database you're connected to.
3. Type a SQL statement on the left, or use **File > Open SQL file**, then click **RUN**. Results appear in the table on the right.
4. Use **File > Export as CSV file** to save the results.
5. **File > Connect…** switches to another database, and **File > Disconnect** forgets the current credentials. Passwords are never saved to disk.

**File > Show error pop-ups** turns query error pop-ups on or off (on by default).

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

These values are the defaults shown in the Connect dialog. Username and password are always entered in the dialog.

## Project layout

```
pom.xml                         Maven build
src/main/java/datagraph         Java sources (Main, core/, ui/)
src/main/resources              FXML screens, images, default datagraph.properties
```

## Opening in an IDE

Open the folder in IntelliJ IDEA, Eclipse or VS Code and import it as a Maven project. The IDE picks up the dependencies from `pom.xml`.
