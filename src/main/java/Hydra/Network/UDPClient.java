package Hydra.Network;

import Hydra.Generic.NetworkListener;
import Hydra.Logging.Logger;
import Hydra.Packets.Packet;
import Hydra.Packets.PacketContainer;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import javafx.application.Platform;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static Hydra.Logging.Logger.error;

class UDPClient extends Client {

    private final int TCP_PORT = 5555;
    private final int UDP_PORT = 5556;
    private final int TIMEOUT = 3000;

    private final long DROP_RELIABLE_PCKT_TIME = 5_000;

    private static final int WRITE_BUFFER_SIZE = 30_000;
    private static final int OBJECT_BUFFER_SIZE = 6_000;

    private NetworkListener conStateListener;
    private Listener initialListener;
    private PacketSender packetSender = null;
    private Thread senderThread = null;

    UDPClient(NetworkListener conStateListener)
    {
        super(WRITE_BUFFER_SIZE, OBJECT_BUFFER_SIZE);
        this.conStateListener = conStateListener;

        PacketRegistry.register(this);

        initialListener = new Listener(){
            @Override
            public void received(Connection connection, Object object) {
                super.received(connection, object);

                if(object instanceof PacketRegistry.InitialPacket) {
                    removeListener(this);
                    if (senderThread != null) {
                        senderThread.start();
                    }
                }
            }
        };
    }

    void connect(String serverAddress)
    {
        new Thread(() -> {

            try { connect(TIMEOUT,serverAddress,TCP_PORT, UDP_PORT); }
            catch (IOException ex) {
                error("Connection error", ex);
            }

            Platform.runLater(() -> {
                if(isConnected())
                    conStateListener.onConnectionStateChanged(ConnectionState.CONNECTED);
                else conStateListener.onConnectionStateChanged(ConnectionState.TIMED_OUT);
            });

        }).start();
    }

    void stopSimulation()
    {
        if (packetSender != null) {
            removeListener(packetSender);
            packetSender.stop();
            packetSender = null;
        }
    }

    void disconnect()
    {
        close();
        if(packetSender != null)
            removeListener(packetSender);
    }

    void startSimulation(boolean reliable, boolean ordered, int packetsPerSec, float loseChance, String msg)
    {
        stopSimulation();
        packetSender = new PacketSender(reliable,ordered,packetsPerSec, loseChance,msg);
        addListener(packetSender);
        senderThread = new Thread(packetSender);

        addListener(initialListener);
        sendTCP(new PacketRegistry.InitialPacket());
    }

    private class PacketSender extends Listener implements Runnable
    {
        private volatile boolean running = true;

        private final boolean reliable, ordered;
        private volatile int packetsPerSec;
        private volatile float loseChance;
        private final String msg;
        private Random random;

        private int sent, confirmed;
        private int lost = 0;

        private int _sequenceNumber = 0;
        private AtomicInteger lastACK = new AtomicInteger(0);
        private PacketContainer history = new PacketContainer();

        private float randomValue;
        private long currentTime;

        PacketSender(boolean reliable, boolean ordered, int packetsPerSec, float loseChance, String msg){
            this.reliable = reliable;
            this.ordered = ordered;

            setPacketsPerSec(packetsPerSec);
            setLoseChance(loseChance);

            this.msg = msg;
            this.random = new Random();
            this.sent = this.confirmed;
        }

        @Override
        public void run()
        {
            try {
                while (running)
                {
                    try {
                        Thread.sleep(1_000 / packetsPerSec);
                    } catch (InterruptedException e) {
                        Platform.runLater(() -> conStateListener.onCriticalError("CPU error. Please connect again"));
                        error("CPU Error", e);
                    }

                    currentTime = System.currentTimeMillis();
                    randomValue = random.nextFloat();

                    final Packet pckt = new Packet(_sequenceNumber++, currentTime, msg, loseChance);

                    if (reliable) {

                        if (ordered) // complete implementation
                        {
                            history.add(pckt);

                            if (randomValue > loseChance)
                                sendUDP(history);

                            ++sent;
                            Platform.runLater(() -> conStateListener.update(sent, history.getSize(), confirmed, sent - confirmed));
                        }
                        else
                        {
                            history.add(pckt);

                            if (randomValue > loseChance)
                                sendUDP(pckt);

                            ++sent;
                            Platform.runLater(() -> conStateListener.update(sent, history.getSize(), confirmed, sent - confirmed));

                            // send the entire unconfirmed history
                            for (Packet p : history.getElements())
                            {
                                if (randomValue > loseChance) {
                                    sendUDP(p);
                                }

                                sent++;
                                Platform.runLater(() -> conStateListener.update(sent, history.getSize(), confirmed, sent - confirmed));
                            }
                        }
                    }
                    else // NOT RELIABLE && NOT ORDERED
                    {
                        if (randomValue > loseChance) {
                            sendUDP(pckt);
                            history.add(pckt);
                        }else ++lost;

                        ++sent;
                        Platform.runLater(() -> conStateListener.update(sent, 0, confirmed, lost));
                    }

                    Platform.runLater(() -> {
                        conStateListener.updateSequences(_sequenceNumber, lastACK.get());
                        conStateListener.updateRTT();
                    });
                }

            }catch (Exception ex){
                Platform.runLater(() -> conStateListener.onCriticalError("Buffer overflow. Please use lower Packet Loss"));
                Logger.error("Buffer overflow", ex);
            }
        }

        @Override
        public void received(Connection connection, Object object)
        {
            super.received(connection, object);

            currentTime = System.currentTimeMillis();

            if(object instanceof Packet)
            {
                Platform.runLater(() -> conStateListener.updateRTT());

                final Packet pckt = (Packet) object;

                if(reliable){

                    if(ordered)
                        history.cutBelow(pckt.getSequenceNumber());
                    else
                        history.remove(pckt.getSequenceNumber());

                    if(pckt.getSequenceNumber() > lastACK.get())
                        lastACK.set(pckt.getSequenceNumber());

                    ++confirmed;

                    Platform.runLater(() -> {
                        conStateListener.update(sent,history.getSize(),confirmed,sent-confirmed);
                        conStateListener.received(pckt.getSequenceNumber(), currentTime - pckt.getLaunchTime());
                    });
                }
                else // NOT RELIABLE && NOT ORDERED
                {
                    lastACK.set(pckt.getSequenceNumber());
                    ++confirmed;

                    history.remove(pckt.getSequenceNumber());

                    history.getElements().stream()
                            .filter(p -> p.getLaunchTime() < currentTime - DROP_RELIABLE_PCKT_TIME)
                            .forEach(p -> {
                                ++lost;
                                history.remove(p.getSequenceNumber());
                            });

                    Platform.runLater(() -> {
                        conStateListener.update(sent, 0, confirmed, lost);
                        conStateListener.received(pckt.getSequenceNumber(), currentTime - pckt.getLaunchTime());
                    });
                }
            }
        }

        @Override
        public void disconnected(Connection connection) {
            super.disconnected(connection);
            Platform.runLater(() -> conStateListener.onConnectionStateChanged(ConnectionState.DISCONNECTED));
        }

        void setPacketsPerSec(int pps){
            if(pps > 0)
                this.packetsPerSec = pps;
            else this.packetsPerSec = 1;
        }

        void setLoseChance(float chance){
            if(chance < 0)
                this.loseChance = 0;
            if(loseChance >= 1)
                loseChance = 0.7f;
            else this.loseChance = chance;
        }

        void stop(){ running = false; }
    }

    void setPacketsPerSec(int pps){
        if(packetSender != null){
            packetSender.setPacketsPerSec(pps);
        }
    }

    void setLoseChance(float chance){
        if(packetSender != null){
            packetSender.setLoseChance(chance);
        }
    }
}
