package Hydra.Network;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    // interaction
    @FXML
    public Button connectButton;

    @FXML
    public TextField serverAddressTF;

    @FXML
    public Label connectionInfoLabel;

    @FXML
    public LineChart<Integer, Integer> lostPerSecondChart;

    @FXML
    public AreaChart<Integer, Integer> sequenceChart;

    // statistics
    @FXML
    public Label sentLabel;

    @FXML
    public Label bufferLabel;

    @FXML
    public Label confirmedLabel;

    @FXML
    public Label pingLabel;

    @FXML
    public Label lostLabel;

    @FXML
    public Label timeLabel;

    @FXML
    public Slider packetsPerSecond;

    @FXML
    public Slider loseChance;

    // gui
    @FXML
    public GridPane optionsPane;

    @FXML
    public GridPane slidersPane;

    @FXML
    public GridPane inputPane;

    @FXML
    public GridPane statsPane;

    @FXML
    public TextField messageTextfield;

    @FXML
    public Button startButton;

    @FXML
    public CheckBox reliableCheckbox;

    @FXML
    public CheckBox orderedCheckbox;

    @FXML
    public CheckBox lossCheckbox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {}
}
