package datagraph.ui;

import com.opencsv.CSVWriter;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import datagraph.core.*;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;


public class ViewController {

    @FXML
    private VBox frame;
    @FXML
    private Label titleLabel;
    @FXML
    private MenuItem quitButton;
    @FXML
    private MenuItem exportOption;
    @FXML
    private MenuItem openFileOption;
    @FXML
    private MenuItem aboutButton;
    @FXML
    private Menu fileMenu;
    @FXML
    private MenuItem logoutOption;
    @FXML
    private SeparatorMenuItem firstMenuSeparator;
    @FXML
    private SeparatorMenuItem secondMenuSeparator;
    @FXML
    private TextArea txtInput;
    @FXML
    private Button clearInput;
    @FXML
    private Button clearOutput;
    @FXML
    private GridPane gridOutput;
    @FXML
    private Button runButton;
    @FXML
    private TableView tableView;


    public void initialize() {

        try{
            //Set values
            titleLabel.setText(Information.Title());

            //Try to center window
            //Screens.CenterWindow((Stage)frame.getScene().getWindow());

            //Add 'quit' menu item click event
            quitButton.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent t) {
                    Platform.exit();
                }
            });

            //Add 'open file' menu item click event
            openFileOption.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent t) {
                    getFileText();
                }
            });

            //Add 'about' menu item click event
            aboutButton.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent t) {
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("About");
                    alert.setHeaderText(Information.Name());
                    alert.setContentText("A database query client application.");
                    alert.show();
                }
            });

            //Set button click event for 'Clear' button on input
            clearInput.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    txtInput.clear();
                }
            });

            //Set button click event for 'Clear' button on output
            clearOutput.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    try{
                        tableView.getItems().clear();
                        tableView.getColumns().clear();
                    }
                    catch (Exception ex) {}
                }
            });

            //Set menu items availability when its about to show
            fileMenu.setOnShowing(event -> {
                exportOption.setDisable(true);
                openFileOption.setDisable(false);

                /*
                    Make 'export' options available when a
                    table result is returned back to the user.
                 */
                if(tableView.getColumns().toArray().length>0){
                    exportOption.setDisable(false);
                }
            });

            //Set 'export' option click event
            exportOption.setOnAction(event -> {
                extractCSVFile();
            });

            //Set button click event for 'Run' button
            runButton.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    try{
                        runButton.setDisable(true);
                        //Check if there is an actual statement
                        if(!HelperFunction.IsEmptyOrNull(txtInput.getText())) {
                            formatResults(Database.GetConnection());
                            lastResult = Database.GetConnection().createStatement().executeQuery(txtInput.getText());
                        }
                        else
                        {
                            String m = "The statement input box is empty!";
                            showMessage(m);
                        }
                    }
                    catch(Exception ex) {}
                    finally { runButton.setDisable(false); }
                }
            });

            //Set user logout option event
            logoutOption.setOnAction(event -> {
                Database.ClearCredentials();
                switchToLoginScreen();
                System.out.println("User has logged out of the '"+ Database.GetDatabaseName()+"' database!");
            });

        } catch (Exception ex) {
            if(!HelperFunction.IsEmptyOrNull(ex.getMessage()))
                System.out.println("Failed to load page: "+ex.getMessage());
            else {
                System.out.println("This page didn't load correctly");
            }
        }
    }

    //Get last result set
    private ResultSet lastResult;

    //Get text from any .sql or .txt file
    private void getFileText() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open a SQL file");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL File", "*.sql", "*.txt"));
        File file = chooser.showOpenDialog(frame.getScene().getWindow());
        if(file != null) {
            try{
                String txt = Files.readString(file.toPath());
                if(!HelperFunction.IsEmptyOrNull(txt))
                    txtInput.setText(txt);
            }
            catch(Exception er) {
                showMessage("Failed to open file: "+er.getMessage());
            }
        }
    }

    //Extract table result as .csv file
    private void extractCSVFile() {
        Alert question = new Alert(AlertType.CONFIRMATION,
                "Would you like to include the columns in your .csv file?", ButtonType.YES, ButtonType.NO);
        question.setTitle(Information.Title());
        question.setHeaderText(null);
        boolean includeColumns = question.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save file to");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV File", "*.csv"));
        File file = chooser.showSaveDialog(frame.getScene().getWindow());
        if(file != null) {
            try{
                if(HelperFunction.IsEmptyOrNull(HelperFunction.GetFileExtension(file)))
                    file = new File(file.getPath()+".csv");
                CSVWriter csvWriter = new CSVWriter(new FileWriter(file));
                if(lastResult!=null){
                    csvWriter.writeAll(lastResult, includeColumns, false);
                    csvWriter.close();
                    System.out.println("Created CSV file '"+ file.getPath()+"'");
                }
                else {
                    System.out.println("Failed to extract results to file, results were empty!");
                }
            }
            catch(Exception er) {
                System.out.println("Failed to extract results to file: "+er.getMessage());
            }
        }
    }

    /*
        This function handles the output
        for the sql query results in the grid.
    */
    private ObservableList<ObservableList> data;
    private void formatResults(Connection c){
        try {
            data = FXCollections.observableArrayList();
            if(!HelperFunction.IsEmptyOrNull(txtInput.getText())){
                ResultSet rs = c.createStatement().executeQuery(txtInput.getText());
                /**********************************
                 * TABLE COLUMN ADDED DYNAMICALLY *
                 **********************************/
                tableView.setTableMenuButtonVisible(true);
                tableView.getItems().clear();
                tableView.getColumns().clear();
                int i;
                for(i=0 ; i<rs.getMetaData().getColumnCount(); i++){
                    //We are using non property style for making dynamic table
                    final int j = i;
                    TableColumn col = new TableColumn(rs.getMetaData().getColumnName(i+1));
                    col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList,String>,ObservableValue<String>>(){
                        public ObservableValue<String> call(TableColumn.CellDataFeatures<ObservableList, String> param) {
                            return new SimpleStringProperty(param.getValue().get(j).toString());
                        }
                    });
                    tableView.getColumns().addAll(col);
                }
                System.out.println(i+" column(s) in this table result.");
                /********************************
                 * Data added to ObservableList *
                 ********************************/
                int r = 0;
                while(rs.next()){
                    //Iterate Row
                    ObservableList<String> row = FXCollections.observableArrayList();
                    for(int v=1 ; v<=rs.getMetaData().getColumnCount(); v++){
                        //Iterate Column
                        row.add(rs.getString(v));
                    }
                    data.add(row);
                    r+=1;
                }
                System.out.println(r+" row(s) return as a result.");
                //FINALLY ADDED TO TableView
                tableView.setItems(data);
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
            showMessage("Error on query execution: "+ex.getMessage());
        }
    }

    //Show message on the view ui
    private void showMessage(String message){
        try{
            System.out.println(message);
            if(UserSettings.GetShowErrors()) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle(Information.Title());
                alert.setHeaderText(Information.Name());
                alert.setContentText(message);
                alert.show();
            }
        }
        catch(Exception ex) {}
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
