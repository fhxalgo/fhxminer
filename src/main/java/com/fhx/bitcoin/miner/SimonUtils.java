package com.fhx.bitcoin.miner;

import org.javasimon.Counter;
import org.javasimon.Simon;
import org.javasimon.SimonManager;
import org.javasimon.Stopwatch;

/**
 * Created by George on 1/9/14.
 */
public class SimonUtils {
    public static Stopwatch getNumberedStopwatch(String name) {
        int tryCount = 0;
        do {
            String fullname = name + "." + tryCount;
            Simon simon = SimonManager.getSimon(fullname);
            if (simon == null) {
                return SimonManager.getStopwatch(fullname);
            }
        } while (tryCount++ < Integer.MAX_VALUE);

        return SimonManager.getStopwatch(name);
    }

    public static Counter getNumberedCounter(String name) {
        int tryCount = 0;
        do {
            String fullname = name + "." + tryCount;
            Simon simon = SimonManager.getSimon(fullname);
            if (simon == null) {
                return SimonManager.getCounter(fullname);
            }
        } while (tryCount++ < Integer.MAX_VALUE);

        return SimonManager.getCounter(name);
    }
}
