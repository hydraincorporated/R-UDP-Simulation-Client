package Hydra.Packets;


public class Packet {

    private int sequenceNumber;
    private long launchTime;
    private String message;
    private float dropChance;
    private int _serverSequence;

    public Packet(){}

    public Packet(int sequenceNumber, long launchTime, String message, float dropChance){
        this.sequenceNumber = sequenceNumber;
        this.launchTime = launchTime;
        this.message = message;
        this.dropChance = dropChance;
    }

    public float getDropChance(){
        return dropChance;
    }

    public int getSequenceNumber(){
        return sequenceNumber;
    }

    public long getLaunchTime(){
        return launchTime;
    }

    public String getMessage(){
        return message;
    }

    public void setSequenceNumber(int sequenceNumber){
        this.sequenceNumber = sequenceNumber;
    }

    public void setLaunchTime(long launchTime){
        this.launchTime = launchTime;
    }

    public void setMessage(String message){
        this.message = message;
    }

    public void setServerSequence(int sequence){
        this._serverSequence = sequence;
    }

    public int getServerSequence(){
        return this._serverSequence;
    }
}
