package com.fhx.bitcoin.miner;

import com.google.common.util.concurrent.AbstractService;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.RingBuffer;
import org.javasimon.Split;
import org.javasimon.Stopwatch;

import javax.naming.InsufficientResourcesException;

/**
 * Created by George on 1/8/14.
 */
public abstract class AbstractDisruptorService<T extends BlockEvent> extends AbstractService {
    public final Stopwatch STOPWATCH = SimonUtils.getNumberedStopwatch("abstract.disruptor.time.e2e");

    private RingBuffer<T> ringBuffer;

    public AbstractDisruptorService() {
    }

    protected void doStart() {
        try {
            ringBuffer = startDisruptor();
            notifyStarted();
        } catch (Exception e) {
            notifyFailed(e);
        }

    }

    protected void doStop() {
        stopDisruptor();
        notifyStopped();
    }

    public void publishEvent(EventTranslator<T> translator) {
        Split split = STOPWATCH.start();
        long next = ringBuffer.next();
        {
            T blockEvent = ringBuffer.get(next);
            blockEvent.setStopWatch(split);
            translator.translateTo(blockEvent, next);
        }
        ringBuffer.publish(next);
    }

    public void tryPublishEvent(EventTranslator<T> translator) throws InsufficientResourcesException {
        Split split = STOPWATCH.start();
        long next = ringBuffer.next();
        {
            T blockEvent = ringBuffer.get(next);
            blockEvent.setStopWatch(split);
            translator.translateTo(blockEvent, next);
        }
        ringBuffer.publish(next);
    }

    protected abstract RingBuffer<T> startDisruptor() throws Exception;

    protected abstract void stopDisruptor();

}
