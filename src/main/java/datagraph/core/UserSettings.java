package datagraph.core;

public class UserSettings {
    private static Boolean showErrors = false;
    public static void SetShowErrors(Boolean value){
        showErrors = value;
    }
    public static Boolean GetShowErrors(){
        return showErrors;
    }
}
