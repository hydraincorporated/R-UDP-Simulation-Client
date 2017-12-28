package Hydra.Network;

import Hydra.Generic.NetworkListener;
import Hydra.Utils.GUITextUtils;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static Hydra.Network.ConnectionState.DISCONNECTED;

public class ApplicationManager implements NetworkListener{

    private Stage stage;
    private Scene scene;

    private Controller widgets;

    private final UDPClient udpClient;

    private final EventHandler<ActionEvent> connectHandler, disconnectHandler;
    private final EventHandler<ActionEvent> startHandler, stopHandler;

    private XYChart.Series<Integer, Integer> lostPacketsSeries;
    private LineChart.Series<Integer, Integer> sequenceSeries;

    // simulation
    private final ArrayList<Long> rtts = new ArrayList<>();

    private long nextLostSeriesUpdate;
    private int currentSecond;
    private int previousLost;
    private long nextSequenceUpdate;
    private long startTime;
    private long nextRTTUpdate;

    private static final int lostSeriestUpdateDelay = 1000;
    private static final int sequenceUpdateDelay = 800;
    private static final int RTTUpdateDelay = 500;

    private boolean reliable, ordered;
    private int pps;
    private float extraLoss;
    private String message;
    private long timePassed;
    private int lostThisSecond;
    private long timeNow;
    private long avgRTT;

    ApplicationManager(Stage stage, Controller widgets)
    {
        this.stage = stage;
        this.scene = stage.getScene();

        // close other threads on exit
        this.stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if(ApplicationManager.this.udpClient != null) {
                    ApplicationManager.this.udpClient.stopSimulation();
                    ApplicationManager.this.udpClient.stop();
                }
            }
        });

        this.widgets = widgets;

        this.udpClient = new UDPClient(ApplicationManager.this);
        new Thread(udpClient).start();

        connectHandler = event -> {
            widgets.serverAddressTF.setDisable(true);
            widgets.connectButton.setDisable(true);
            udpClient.connect(widgets.serverAddressTF.getText());
        };

        disconnectHandler = event -> {
            widgets.serverAddressTF.setDisable(false);
            widgets.connectButton.setDisable(false);
            udpClient.disconnect();
            udpClient.stopSimulation();
            onConnectionStateChanged(DISCONNECTED);
            clearSimulation();
        };

        widgets.connectButton.setOnAction(connectHandler);

        startHandler= event -> {
            startSimulation();
            widgets.startButton.setText("Stop");
            widgets.messageTextfield.setDisable(true);
            widgets.optionsPane.setDisable(true);
        };

        stopHandler = event -> {
            stopSimulation();
            widgets.startButton.setText("Start");
            widgets.messageTextfield.setDisable(false);
            widgets.optionsPane.setDisable(false);
        };

        widgets.startButton.setOnAction(startHandler);

        widgets.lossCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue)
            {
                widgets.loseChance.setDisable(false);
                widgets.loseChance.setOpacity(1f);
            }
            else
            {
                widgets.loseChance.setDisable(true);
                widgets.loseChance.setOpacity(0.5f);
            }
        });

        widgets.reliableCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue)
            {
                widgets.orderedCheckbox.setDisable(false);
            }
            else
            {
                widgets.orderedCheckbox.setDisable(true);
            }

        });

        widgets.lostPerSecondChart.setCreateSymbols(false);

        widgets.packetsPerSecond.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            udpClient.setPacketsPerSec(newValue.intValue());
        });

        widgets.loseChance.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            udpClient.setLoseChance(newValue.floatValue());
        });
    }

    @Override
    public void onConnectionStateChanged(ConnectionState state) {

        switch (state){
            case CONNECTED:
                clearSimulation();
                widgets.serverAddressTF.setDisable(true);
                widgets.connectButton.setText("Disconnect");
                widgets.connectButton.setDisable(false);
                GUITextUtils.animateTextAndColor(widgets.connectionInfoLabel,"connected", "37ad68");

                disablePanes(false);
                widgets.connectButton.setOnAction(disconnectHandler);
                break;

            case DISCONNECTED:
            case TIMED_OUT:
                widgets.serverAddressTF.setDisable(false);
                widgets.connectButton.setText("Connect");
                widgets.connectButton.setDisable(false);
                GUITextUtils.animateTextAndColor(widgets.connectionInfoLabel,"disconnected", "132d2a");
                widgets.connectButton.setOnAction(connectHandler);

                stopSimulation();
                widgets.startButton.setText("Start");
                widgets.messageTextfield.setDisable(false);
                widgets.optionsPane.setDisable(false);

                disablePanes(true);
                break;
        }
    }

    private void disablePanes(boolean disable){
        widgets.inputPane.setDisable(disable);
        widgets.optionsPane.setDisable(disable);
        widgets.slidersPane.setDisable(disable);
        widgets.statsPane.setDisable(disable);
    }

    private void startSimulation(){
        clearSimulation();

        // getting input
        reliable = widgets.reliableCheckbox.isSelected();
        ordered = widgets.orderedCheckbox.isSelected();

        if(!reliable) ordered = false;

        pps = (int) widgets.packetsPerSecond.getValue();
        extraLoss = widgets.lossCheckbox.isSelected() ? (float) widgets.loseChance.getValue() : 0f;

        message = widgets.messageTextfield.getText();
        if(message == null || message.equals("")) {
            message = "Packet Message";
            widgets.messageTextfield.setText("Packet Message");
        }

        udpClient.startSimulation(reliable,
                ordered,
                pps,
                extraLoss,
                message);

        widgets.startButton.setOnAction(stopHandler);
    }

    private void stopSimulation(){
        udpClient.stopSimulation();
        widgets.startButton.setOnAction(startHandler);

        if(widgets.lossCheckbox.isSelected()) {
            widgets.loseChance.setDisable(false);
            widgets.loseChance.setOpacity(1f);
        } else {
            widgets.loseChance.setDisable(true);
            widgets.loseChance.setOpacity(0.5f);
        }
    }

    @Override
    public void updateSequences(int client, int server) {

        timeNow = System.currentTimeMillis();

        if(timeNow> nextSequenceUpdate){
            nextSequenceUpdate += sequenceUpdateDelay;

            int time = (int) (timeNow - startTime);
            int difference = Math.abs(client - server);

            // sequence difference
            if(sequenceSeries.getData().size() > 10)
                sequenceSeries.getData().remove(0);

            sequenceSeries.getData()
                    .add(new XYChart.Data<>(time,  difference));
        }
    }

    @Override
    public void update(int sent, int bufferSize, int confirmed, int lost) {

        timeNow = System.currentTimeMillis();

        widgets.sentLabel.setText(String.valueOf(sent));
        widgets.bufferLabel.setText(String.valueOf(bufferSize));
        widgets.confirmedLabel.setText(String.valueOf(confirmed));
        widgets.lostLabel.setText(String.valueOf(lost));

        timePassed = timeNow - startTime;
        widgets.timeLabel.setText(String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(timePassed),
                TimeUnit.MILLISECONDS.toSeconds(timePassed) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timePassed)))
        );

        if(timeNow > nextLostSeriesUpdate){
            nextLostSeriesUpdate += lostSeriestUpdateDelay;

            lostThisSecond = Math.abs(lost - previousLost);
            lostPacketsSeries.getData().add(new XYChart.Data<>(currentSecond++, lostThisSecond));
            this.previousLost = lost;
        }

    }

    @Override
    public void received(int sequence, long time) {
        if(rtts.size() > 10){rtts.remove(0);}
        rtts.add(time);
    }

    @Override
    public void updateRTT(){
        if(timeNow > nextRTTUpdate && rtts.size() > 0){
            nextRTTUpdate += RTTUpdateDelay;
            avgRTT = rtts.stream().mapToLong(p -> p).sum() / rtts.size();
            widgets.pingLabel.setText(String.valueOf(avgRTT + " ms"));
        }
    }

    @Override
    public void onCriticalError(String message) {
        disconnectHandler.handle(null);
        GUITextUtils.animateTextAndColor(widgets.connectionInfoLabel, message, "70050e");
    }

    private void clearSimulation(){
        widgets.sentLabel.setText("0");
        widgets.bufferLabel.setText("0");
        widgets.confirmedLabel.setText("0");
        widgets.pingLabel.setText("0");
        widgets.lostLabel.setText("0");
        widgets.timeLabel.setText("0");

        rtts.clear();
        timeNow = System.currentTimeMillis();
        startTime = timeNow;
        nextLostSeriesUpdate = timeNow;
        nextLostSeriesUpdate = timeNow;
        nextSequenceUpdate = timeNow;

        currentSecond = previousLost = 0;
        widgets.lostPerSecondChart.getData().clear();
        lostPacketsSeries = new XYChart.Series<>();
        widgets.lostPerSecondChart.getData().add(lostPacketsSeries);

        widgets.sequenceChart.getData().clear();
        sequenceSeries = new XYChart.Series<>();
        sequenceSeries.setName("Difference");
        widgets.sequenceChart.getData().add(sequenceSeries);

    }
}
