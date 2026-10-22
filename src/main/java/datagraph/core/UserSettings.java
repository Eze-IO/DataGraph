package datagraph.core;

public class UserSettings {
    private static Boolean showErrors = true;
    public static void SetShowErrors(Boolean value){
        showErrors = value;
    }
    public static Boolean GetShowErrors(){
        return showErrors;
    }
}
