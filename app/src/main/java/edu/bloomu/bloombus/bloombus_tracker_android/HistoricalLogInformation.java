package edu.bloomu.bloombus.bloombus_tracker_android;

import java.util.UUID;

public class HistoricalLogInformation {
    public long arrivalTime;
    public String prevStop;
    public String nextStop;
    public String loopKey;
    public UUID sessionID;

    public HistoricalLogInformation(
        long arrivalTime, String prevStop, String nextStop, String loopKey, UUID sessionID
    ) {
        this.arrivalTime = arrivalTime;
        this.prevStop = prevStop;
        this.nextStop = nextStop;
        this.loopKey = loopKey;
        this.sessionID = sessionID;
    }
}
