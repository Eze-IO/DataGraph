package datagraph.core;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

public class HelperFunction {
    public static Boolean IsEmptyOrNull(String str){
        if(str == null || str.isEmpty())
            return true;
        return false;
    }

    public static Boolean IsInternetAvailable() {
        try {
            URL url = URI.create("https://www.google.com/").toURL();
            URLConnection connection = url.openConnection();
            connection.connect();
            return true;
        }
        catch(Exception ex) {
            return false;
        }
    }

    public static String GetFileExtension(File file){
        String extension = "";
        int i = file.getPath().lastIndexOf('.');
        if (i > 0) {
            extension = file.getPath().substring(i+1);
        }
        return extension;
    }
}
