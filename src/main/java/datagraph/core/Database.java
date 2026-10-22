package datagraph.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;


public class Database {

    private static String username;
    private static String password;
    private static String lastUsername;
    private static String lastError;

    //Safely checks if username and password are correct for the database
    public static Boolean IsUserAuthorized(String username, String password){
        Connection c = null;
        try{
            Database.username = username;
            Database.password = password;
            Database.lastUsername = username;
            c = GetConnection(Database.username, Database.password);
            if(c != null)
                return true;
            return false;
        }
        catch(Exception e) {
            //System.out.println(e);
            return false;
        }
        finally {
            if(c != null)
                try{
                    c.close();
                }catch (Exception ex) {}
        }
    }

    private static int _port;
    private static String _domain;
    private static String _databaseName;
    private static String _options;
    public static void SetConnectionUrl(String domain, int port, String databaseName) {
        _domain = domain;
        _port = port;
        _databaseName = databaseName;
    }

    /*
        Loads connection settings from the bundled datagraph.properties,
        then from a datagraph.properties file in the working directory,
        then from -Ddb.* system properties (later ones win).
    */
    public static void LoadConnectionSettings() {
        Properties props = new Properties();
        try (InputStream in = Database.class.getResourceAsStream("/datagraph.properties")) {
            if(in != null)
                props.load(in);
        } catch (IOException e) {
            System.out.println("Failed to read bundled settings: "+e.getMessage());
        }
        File local = new File("datagraph.properties");
        if(local.isFile()) {
            try (InputStream in = new FileInputStream(local)) {
                props.load(in);
            } catch (IOException e) {
                System.out.println("Failed to read "+local.getAbsolutePath()+": "+e.getMessage());
            }
        }
        for(String key : new String[]{"db.host", "db.port", "db.name", "db.options"}) {
            String value = System.getProperty(key);
            if(value != null)
                props.setProperty(key, value);
        }

        int port = 1433;
        try {
            port = Integer.parseInt(props.getProperty("db.port", "1433").trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid db.port, using 1433");
        }
        SetConnectionUrl(props.getProperty("db.host"), port, props.getProperty("db.name"));
        _options = props.getProperty("db.options");
    }

    //Formats the database connection string url
    private static String getUrl() {
        if(!HelperFunction.IsEmptyOrNull(_domain) && !HelperFunction.IsEmptyOrNull(_databaseName)) {
            String url = String.format("jdbc:sqlserver://%s:%d;databaseName=%s", _domain, _port, _databaseName);
            if(!HelperFunction.IsEmptyOrNull(_options))
                url += ";"+_options;
            return url;
        }
        return null;
    }

    //Returns domain name
    public static String GetDomain() {
        if(HelperFunction.IsEmptyOrNull(_domain))
            return ("Unknown");
        return _domain;
    }

    //Returns port number as string
    public static String GetPort() {
        return (""+_port+"");
    }

    //Returns database name
    public static String GetDatabaseName() {
        if(HelperFunction.IsEmptyOrNull(_databaseName))
            return ("Unknown");
        return _databaseName;
    }

    /*
        An overload to get the
        database connection if
        the 'IsUserAuthorized'
        method returns true :)
    */
    public static Connection GetConnection() {
        if(!HelperFunction.IsEmptyOrNull(username) && !HelperFunction.IsEmptyOrNull(password)){
            return GetConnection(username, password);
        }
        return null;
    }

    //Gets database connection; can return null :(
    public static Connection GetConnection(String username, String password) {
        try{
            String driver ="com.microsoft.sqlserver.jdbc.SQLServerDriver";
            String url = getUrl();
            if(HelperFunction.IsEmptyOrNull(url))
                System.out.println("Connection information is not set!");
            Class.forName(driver);

            Connection conn = DriverManager.getConnection(url, username, password);
            //conn.setNetworkTimeout(null, 120);
            System.out.println("Connection successfully created!");
            lastError = null;
            return conn; //Successfully connected to the database
        }
        catch(Exception e)
        {
            //Error on connecting to the database; callers show GetLastError()
            ClearCredentials();
            lastError = e.getMessage();
            System.out.println("Failed to create connection: "+e);
            return null;
        }
    }

    //True after a successful connect, until disconnect or a failed connection
    public static boolean IsConnected() {
        return !HelperFunction.IsEmptyOrNull(username) && !HelperFunction.IsEmptyOrNull(password);
    }

    //Username of the last connection attempt (used to prefill the connect dialog)
    public static String GetUsername() {
        return lastUsername;
    }

    //Message from the last failed connection, or null
    public static String GetLastError() {
        return lastError;
    }

    public static void ClearCredentials() {
        username = password = null;
    }
    public static void ClearDatabaseInfo() { _domain = _databaseName = null; _port = 0;}
}

