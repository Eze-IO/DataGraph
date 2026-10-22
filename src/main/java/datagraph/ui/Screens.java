package datagraph.ui;

import javafx.application.Application;
import java.util.*;
import javafx.scene.layout.*;
import javafx.scene.*;
import javafx.fxml.*;
import javafx.stage.*;
import javafx.geometry.*;

public class Screens {
    private HashMap<String, Pane> screenMap = new HashMap<>();
    private Scene main;

    public Screens(Scene main) {
        this.main = main;
    }

    protected void addScreen(String name, Pane pane){
        if(!screenMap.containsKey(name))
            screenMap.put(name, pane);
    }

    protected void removeScreen(String name){
        if(screenMap.containsKey(name))
            screenMap.remove(name);
    }

    protected void activate(String name){
        main.setRoot(screenMap.get(name));
        main.getWindow().sizeToScene();
    }

    public static void CenterWindow(Stage stage) {
        try{
            stage.centerOnScreen();
            Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX((primScreenBounds.getWidth() - stage.getWidth()) / 2);
            stage.setY((primScreenBounds.getHeight() - stage.getHeight()) / 2);
        }
        catch (Exception ex) { return; }
    }
}
