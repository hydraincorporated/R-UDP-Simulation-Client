package Hydra.Packets;

import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;

public class PacketContainer{

    private final CopyOnWriteArrayList<Packet> packets;

    public PacketContainer(){
        packets = new CopyOnWriteArrayList<>();
    }

    public void add(Packet e){
        synchronized (PacketContainer.this){
            packets.add(e);
        }
    }

    public LinkedList<Packet> getElements(){
        synchronized (PacketContainer.this) {
            return new LinkedList<>(packets);
        }
    }

    public void cutBelow(Packet param){
        synchronized (PacketContainer.this){
            packets.removeIf(pckt -> pckt.getSequenceNumber() < param.getSequenceNumber());
        }
    }

    public void cutBelow(int param){
        synchronized (PacketContainer.this){
            packets.removeIf(pckt -> pckt.getSequenceNumber() < param);
        }
    }

    public void remove(Packet param){
        synchronized (PacketContainer.this){
            packets.removeIf(pckt -> pckt.getSequenceNumber() == param.getSequenceNumber());
        }
    }

    public void remove(int param){
        synchronized (PacketContainer.this){
            packets.removeIf(pckt -> pckt.getSequenceNumber() == param);
        }
    }

    public int getSize(){
        synchronized (PacketContainer.this) {
            return packets.size();
        }
    }
}
