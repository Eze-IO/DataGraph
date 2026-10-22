package datagraph.core;

import java.io.File;

public class HelperFunction {
    public static Boolean IsEmptyOrNull(String str){
        if(str == null || str.isEmpty())
            return true;
        return false;
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
