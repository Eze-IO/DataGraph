package datagraph.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import datagraph.core.Database;
import datagraph.core.HelperFunction;
import datagraph.core.Information;

/*
    Asks for the server, database and credentials, then tries
    to connect. The dialog stays open until the connection
    succeeds or the user cancels. Replaces the old login and
    settings screens.
*/
public class ConnectDialog {

    //Shows the dialog; returns true when connected
    public static boolean Show(Window owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(Information.Title());
        dialog.setHeaderText("Connect to SQL Server");

        ButtonType connectType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectType, ButtonType.CANCEL);

        TextField hostField = new TextField(Database.GetDomain().equals("Unknown") ? "" : Database.GetDomain());
        TextField portField = new TextField(Database.GetPort());
        TextField databaseField = new TextField(Database.GetDatabaseName().equals("Unknown") ? "" : Database.GetDatabaseName());
        TextField usernameField = new TextField(HelperFunction.IsEmptyOrNull(Database.GetUsername()) ? "" : Database.GetUsername());
        PasswordField passwordField = new PasswordField();
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #c0392b;");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(320);

        //Force the port field to be numeric only
        portField.textProperty().addListener((obs, oldValue, newValue) -> {
            if(!newValue.matches("\\d*"))
                portField.setText(newValue.replaceAll("[^\\d]", ""));
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));
        grid.addRow(0, new Label("Host"), hostField);
        grid.addRow(1, new Label("Port"), portField);
        grid.addRow(2, new Label("Database"), databaseField);
        grid.addRow(3, new Label("Username"), usernameField);
        grid.addRow(4, new Label("Password"), passwordField);
        grid.add(errorLabel, 0, 5, 2, 1);
        dialog.getDialogPane().setContent(grid);

        //Try to connect when 'Connect' is clicked; keep the dialog open on failure
        Node connectButton = dialog.getDialogPane().lookupButton(connectType);
        connectButton.addEventFilter(ActionEvent.ACTION, event -> {
            String host = hostField.getText().trim();
            String port = portField.getText().trim();
            String database = databaseField.getText().trim();
            if(HelperFunction.IsEmptyOrNull(host) || HelperFunction.IsEmptyOrNull(port)
                    || HelperFunction.IsEmptyOrNull(database) || HelperFunction.IsEmptyOrNull(usernameField.getText())
                    || HelperFunction.IsEmptyOrNull(passwordField.getText())) {
                errorLabel.setText("Please fill in every field.");
                event.consume();
                return;
            }
            Database.SetConnectionUrl(host, Integer.parseInt(port), database);
            if(!Database.IsUserAuthorized(usernameField.getText(), passwordField.getText())) {
                errorLabel.setText(HelperFunction.IsEmptyOrNull(Database.GetLastError())
                        ? "Failed to connect to the database!" : Database.GetLastError());
                passwordField.clear();
                event.consume();
            }
        });

        dialog.setOnShown(e -> (HelperFunction.IsEmptyOrNull(hostField.getText()) ? hostField
                : HelperFunction.IsEmptyOrNull(usernameField.getText()) ? usernameField : passwordField).requestFocus());

        return dialog.showAndWait().orElse(ButtonType.CANCEL) == connectType;
    }
}
