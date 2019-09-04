package edu.bloomu.bloombus.bloombus_tracker_android;

import java.util.LinkedList;
import java.util.List;

public class HistoricalLogInformation {
    public List<HistoricalPoint> histPoints;
    public long arrivalTime;
    public String prevStop;
    public String nextStop;
    public String loopKey;
    
    public HistoricalLogInformation() {
        histPoints = new LinkedList<>();
    }
}
