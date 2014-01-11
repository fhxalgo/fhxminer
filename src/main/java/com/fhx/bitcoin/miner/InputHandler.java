package com.fhx.bitcoin.miner;

import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by George on 1/9/14.
 */
public class InputHandler extends LifeCycleAwareEventHandler<BlockEvent> {
    private static final Logger log = LoggerFactory.getLogger(InputHandler.class);

    private final Stopwatch STOPWATCH = SimonUtils.getNumberedStopwatch("input.handler");

    public InputHandler() {
    }

    @Override
    public void onStart() {
        log.info("InputHandler: started");
    }

    @Override
    public void onShutdown() {
        log.info("InputHandler: stopped");
    }

    @Override
    public void onEvent(BlockEvent event, long sequence, boolean endOfBatch) throws Exception {
        Split split = STOPWATCH.start();

        try {
        Work work = event.getWork();
        log.info(String.format("InputHandler: {%s}, seq: {%s}, work: [data:{%s}, target:{%s}] "
                , event, sequence, work.dataText, work.targetText));

        }
        finally {
            split.stop();
        }
    }

}
