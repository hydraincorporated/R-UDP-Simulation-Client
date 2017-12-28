package Hydra.Network;

import Hydra.Logging.Logger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class Launch extends Application {

    private ApplicationManager applicationManager;

    @Override
    public void start(Stage primaryStage) throws Exception{

        FXMLLoader loader = new FXMLLoader(new File("resources/FXClient.fxml").toURI().toURL());
        Parent root = loader.load();
        Controller controller = loader.getController();

        primaryStage.setTitle("R-UDP Client");
        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.show();

        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(600);

        primaryStage.setMaxWidth(800);
        primaryStage.setMaxHeight(1000);

        applicationManager = new ApplicationManager(primaryStage, controller);
    }

    public static void main(String[] args) {
        Logger.initialize();
        launch(args);
    }
}
