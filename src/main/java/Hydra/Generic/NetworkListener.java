package Hydra.Generic;

import Hydra.Network.ConnectionState;

public interface NetworkListener {

    void onConnectionStateChanged(ConnectionState state);

    void update(int sent, int received, int confirmed, int lost);
    void received(int sequence, long time);
    void updateSequences(int client, int server);
    void updateRTT();

    void onCriticalError(String message);

}
