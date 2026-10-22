package datagraph.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import datagraph.core.Database;
import datagraph.core.HelperFunction;
import datagraph.core.UserSettings;

public class SettingsController {

    @FXML
    private TextField domainField;
    @FXML
    private TextField portField;
    @FXML
    private TextField databaseNameField;
    @FXML
    private VBox frame;
    @FXML
    private Button submit;
    @FXML
    private Button cancelButton;
    @FXML
    private ToggleButton showErrorsButton;

    public void initialize() {

        //Get and set values
        portField.setText(Database.GetPort());
        domainField.setText(Database.GetDomain());
        databaseNameField.setText(Database.GetDatabaseName());

        //Force the port field to be numeric only
        portField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                                String newValue) {
                if (!newValue.matches("\\d*")) {
                    portField.setText(newValue.replaceAll("[^\\d]", ""));
                }
            }
        });

        //Creating the submit button event handler
        submit.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                if(!HelperFunction.IsEmptyOrNull(domainField.getText()) && !HelperFunction.IsEmptyOrNull(portField.getText())
                        && !HelperFunction.IsEmptyOrNull(databaseNameField.getText())) {
                    Database.SetConnectionUrl(domainField.getText(), Integer.parseInt(portField.getText()), databaseNameField.getText());
                    System.out.println("Settings saved successfully!");
                }
                else {
                    System.out.println("Failed to save settings, a setting value was empty!");
                }
                switchToLoginScreen();
            }
        });

        //Creating the cancel button event handler
        /*Boolean lastToggle = UserSettings.GetShowErrors();
        cancelButton.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                if(lastToggle != UserSettings.GetShowErrors())
                    UserSettings.SetShowErrors(!UserSettings.GetShowErrors());
            }
        });*/

        //Set default value for 'show/hide errors' toggle button
        showErrorsButton.setSelected(UserSettings.GetShowErrors());
        if(UserSettings.GetShowErrors())
            showErrorsButton.setText("HIDE ERRORS");
        else
            showErrorsButton.setText("SHOW ERRORS");

        //Set click event for 'show/hide errors' toggle button
        showErrorsButton.setOnAction((ActionEvent e) -> {
            UserSettings.SetShowErrors(showErrorsButton.isSelected());
            if(UserSettings.GetShowErrors())
                showErrorsButton.setText("HIDE ERRORS");
            else
                showErrorsButton.setText("SHOW ERRORS");
        });
    }

    //Switches to the login screen
    private void switchToLoginScreen() {
        try{
            Screens screen = new Screens(frame.getScene());
            screen.addScreen("login", FXMLLoader.load(getClass().getResource("Login.fxml")));
            screen.activate("login");
        }
        catch (Exception ex) { return; }
    }
}
