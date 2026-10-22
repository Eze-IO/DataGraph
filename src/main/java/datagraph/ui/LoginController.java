package datagraph.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.input.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.text.TextAlignment;
import datagraph.core.Database;
import datagraph.core.HelperFunction;
import datagraph.core.Information;
import datagraph.core.UserSettings;



public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button submitLogin;
    @FXML
    private Label statusMessage;
    @FXML
    private VBox frame;
    @FXML
    private Label titleLabel;
    @FXML
    private Button uClearButton;
    @FXML
    private Button pClearButton;
    @FXML
    private ImageView settingsSign;

    public void initialize() {

        try {
            //Set values
            titleLabel.setText(Information.Title());
            settingsSign.setBlendMode(BlendMode.HARD_LIGHT);
            showSettingsImage();

            //Creating the submit button event handler
            submitLogin.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    toggleLogin();
                    attemptLogin(usernameField.getText(), passwordField.getText());
                    toggleLogin();
                }
            });

            //Event to clear username field
            uClearButton.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent t) {
                    usernameField.clear();
                }
            });

            //Event to clear password field
            pClearButton.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent t) {
                    passwordField.clear();
                }
            });

            //Clear username field toggle event
            usernameField.textProperty().addListener((obs, oldText, newText) -> {
                uClearButton.setDisable(true);
                if(!HelperFunction.IsEmptyOrNull(usernameField.getText()))
                    uClearButton.setDisable(false);
                updateStatus(null);
            });

            //Clear password field toggle event
            passwordField.textProperty().addListener((obs, oldText, newText) -> {
                pClearButton.setDisable(true);
                if(!HelperFunction.IsEmptyOrNull(passwordField.getText()))
                    pClearButton.setDisable(false);
                updateStatus(null);
            });

            //Set settings click event
            settingsSign.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    switchToSettingsScreen();
                }
            });

            Tooltip tip = new Tooltip("Click here to configure database settings!");
            tip.setTextAlignment(TextAlignment.CENTER);
            tip.setWrapText(true);
            Tooltip.install(settingsSign, tip);
        } catch (Exception ex) {
            if(!HelperFunction.IsEmptyOrNull(ex.getMessage()))
                System.out.println("Failed to load page: "+ex.getMessage());
            else{
                String m = "This page didn't load correctly!";
                System.out.println(m);
                updateStatus(m);
            }

        }
    }

    private void showSettingsImage() {
        try {
            settingsSign.setImage(new Image("/settings.png"));
            settingsSign.setCache(true);
        } catch (Exception e) {
            settingsSign.setVisible(false);
            return;
        }
    }

    private void attemptLogin(String username, String password) {
        if(HelperFunction.IsEmptyOrNull(username) || HelperFunction.IsEmptyOrNull(password)){
            updateStatus("The username or password field is empty!");
        } else {
            updateStatus("");
            if(HelperFunction.IsInternetAvailable()){
                if(Database.IsUserAuthorized(username, password)){
                    //Switch to view ui
                    System.out.println("Successfully logged into the '"+ Database.GetDatabaseName()+"' database!");
                    switchToViewScreen();
                }
                else {
                    pClearButton.fire(); //Clear password field when it's probably incorrect
                    String m = "Failed to login into database!";
                    updateStatus("Your username or password is incorrect!");
                }
            }
            else {
                String m = "No internet connection is available";
                updateStatus(m);
            }
        }
    }

    private void toggleLogin(){
        if(!usernameField.isDisabled()
                && !passwordField.isDisabled()){
            usernameField.setDisable(true);
            passwordField.setDisable(true);
            submitLogin.setDisable(true);
        }
        else {
            usernameField.setDisable(false);
            passwordField.setDisable(false);
            submitLogin.setDisable(false);
        }
    }

    //Switches to the view screen
    private void switchToViewScreen() {
        try{
            Screens screen = new Screens(frame.getScene());
            screen.addScreen("view", FXMLLoader.load(getClass().getResource("View.fxml")));
            screen.activate("view");
        }
        catch (Exception ex) { return; }
    }

    //Switches to the settings screen
    private void switchToSettingsScreen() {
        try{
            Screens screen = new Screens(frame.getScene());
            screen.addScreen("settings", FXMLLoader.load(getClass().getResource("Settings.fxml")));
            screen.activate("settings");
        }
        catch (Exception ex) { return; }
    }

    //Show message on the login ui
    private void showMessage(String message){
        try{
            System.out.println(message);
            if(UserSettings.GetShowErrors()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(Information.Title());
                alert.setHeaderText(Information.Name());
                alert.setContentText(message);
                alert.show();
            }
        }
        catch(Exception ex) {}
    }

    //Displays status or error message
    private void updateStatus(String message){
        if(!HelperFunction.IsEmptyOrNull(message))
            statusMessage.setText(message);
        else
            statusMessage.setText("");
    }
}
