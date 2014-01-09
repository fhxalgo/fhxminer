package com.fhx.bitcoin.miner;

import org.javasimon.Split;

/**
 * Created by George on 1/8/14.
 */
public abstract class AbstractDisruptorEvent {
    public static final int TARGET_ALL = -1;
    public static final int TARGET_NONE = -2;

    private int targetInstance = TARGET_ALL;
    private Split stopWatch;

    public void setTargetInstance(int targetInstance) {
        this.targetInstance = targetInstance;
    }

    public int getTargetInstance() {
        return targetInstance;
    }

    public void setStopWatch(Split stopWatch) {
        this.stopWatch = stopWatch;
    }

    public Split getStopWatch() {
        return stopWatch;
    }

    abstract public void onReset();

    public void reset() {
        onReset();
        targetInstance = TARGET_ALL;

        if (stopWatch != null) {
            stopWatch.stop();
            stopWatch = null;
        }
    }
}
