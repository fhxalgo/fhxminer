package com.fhx.bitcoin.miner;

import com.lmax.disruptor.EventFactory;

/**
 * Created by George on 1/8/14.
 */
public final class BlockEvent extends AbstractDisruptorEvent {
    private Work work;
    private long startTime;
    private long endTime;

    public Work getWork() {
        return work;
    }

    public void setWork(Work work) {
        this.work = work;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public void onReset() {

        // reset on Work
        if (work != null)
            work = null;
    }

    public static final EventFactory<BlockEvent> BLOCK_EVENT_FACTORY = new EventFactory<BlockEvent>() {
        @Override
        public BlockEvent newInstance() {
            return new BlockEvent();
        }
    };
}
