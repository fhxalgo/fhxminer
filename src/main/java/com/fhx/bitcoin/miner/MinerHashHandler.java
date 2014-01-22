package com.fhx.bitcoin.miner;

import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by George on 1/9/14.
 */
public class MinerHashHandler extends LifeCycleAwareEventHandler<BlockEvent> {
    private static final Logger log = LoggerFactory.getLogger(MinerHashHandler.class);
    private final Stopwatch STOPWATCH = SimonUtils.getNumberedStopwatch("input.handler");

    private int id;
    private int count;

    private ScanHash sh = new ScanHash();

    public MinerHashHandler(int id, int count) {
        this.id = id;
        this.count = count;
    }

    @Override
    public void onStart() {
        log.info("MinerHashHandler {} started ", id);
    }

    @Override
    public void onShutdown() {
        log.info("MinerHashHandler {} stopped ", id);
    }

    @Override
    public void onEvent(BlockEvent event, long sequence, boolean endOfBatch) throws Exception {
        Split split = STOPWATCH.start();

        try {
            log.info(String.format("ID: {%d}, event: {%s}, seq: {%d}, work: {%s}", id, event, sequence, event.getWork()));

            Work work = event.getWork();
            if (work != null) {
                genHash(work);
            }
            else {
                log.error("Invalid work: {}", work);
            }
        }
        finally {
            split.stop();
        }
    }

    public void genHash(Work work) {
        long ST = System.currentTimeMillis();
        boolean found = sh.scan(work, id*count+1, count);
        long hashes = sh.getCount();
        work.updateHashCount(hashes);
        long ET = System.currentTimeMillis();
        double took = (ET - ST)/1000.0;

        if (found) {
            log.info("$$$$ found: {} " + work.data);
        }

        log.info("HASH {} done in {} seconds, rate [{} Kh/s], TOTAL: {} X" , new Object[]{ hashes, took, (hashes/took)/1000.0
                , work.getHashCount() });
    }

}
