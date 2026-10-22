package datagraph;

import datagraph.core.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import datagraph.ui.Screens;
import datagraph.ui.ViewController;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

        try{

            //Set connection information to connect to a database
            Database.LoadConnectionSettings();

            //Create application window, starting on the query screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ui/View.fxml"));
            Parent root = loader.load();
            ViewController controller = loader.getController();

            //Set window attributes
            primaryStage.setTitle(Information.Name());
            primaryStage.setScene(new Scene(root, primaryStage.getWidth(),
                    primaryStage.getHeight()));
            primaryStage.getIcons().add(new Image("/database.png"));
            //primaryStage.setAlwaysOnTop(true);

            //Set window size(s)
            primaryStage.setMinWidth(primaryStage.getScene().getWidth());
            primaryStage.setMinHeight(primaryStage.getScene().getHeight());
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();
            primaryStage.sizeToScene();

            //Show window
            primaryStage.show();
            Screens.CenterWindow(primaryStage);

            //Offer to connect right away
            Platform.runLater(controller::connect);
        }
        catch(Exception er) {
            System.out.println("Error on program launch: "+er.getMessage());
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
