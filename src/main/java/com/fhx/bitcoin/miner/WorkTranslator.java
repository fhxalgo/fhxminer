package com.fhx.bitcoin.miner;

import com.lmax.disruptor.EventTranslator;

/**
 * Created by George on 1/8/14.
 */
public class WorkTranslator implements EventTranslator<BlockEvent> {

    private Work work;

    public void setWork(Work work) {
        this.work = work;
    }

    @Override
    public BlockEvent translateTo(BlockEvent blockEvent, long l) {
        if (work != null) {
            blockEvent.setWork(work);
            work = null;
        }

        return blockEvent;
    }
}
