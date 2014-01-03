package com.fhx.bitcoin.miner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by George on 1/2/14.
 */
public class Work {
    private static final Logger log = LoggerFactory.getLogger(Work.class);

    public final int[] midstate = new int[8];
    public final int[] data = new int[32];
    public final int[] hash1 = new int[16];
    public final long[] target = new long[8];

    public  String dataText;
    public  String midstateText;
    public  String targetText;
    public  String hash1Text;

    long timestamp;
    long base;

    public Work() {
        this.timestamp = System.nanoTime() / 1000000;
        this.base = 0;
    }

    public Work(String data2, String midstate2, String target2, String hash2) {
        this.dataText = data2;
        this.midstateText = midstate2;
        this.targetText = target2;
        this.hash1Text = hash2;

        this.timestamp = System.nanoTime() / 1000000;
        this.base = 0;
    }

    public boolean update(long delta) {
        boolean getWork = false;

        // base += delta;

        return getWork;
    }

    public void submitNonce(int nonce) {
        data[19] = nonce;
        //networkState.addSendQueue(this);
    }

    public long getTimestamp() {
        return timestamp;
    }
    public long getBase() {
        return base;
    }

    public int[] getData() {
        return data;
    }

    public int getData(int n) {
        return data[n];
    }

    public void setData(int n, int x) {
        data[n] = x;
    }

    public int getMidstate(int n) {
        return midstate[n];
    }

    public int[] getMidstate() {
        return midstate;
    }

    public void setMidstate(int n, int x) {
        midstate[n] = x;
    }

    public long getTarget(int n) {
        return target[n];
    }

    public void setTarget(int n, long x) {
        target[n] = x;
    }
}
