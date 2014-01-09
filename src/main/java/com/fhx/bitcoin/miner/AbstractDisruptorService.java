package com.fhx.bitcoin.miner;

import com.google.common.util.concurrent.AbstractService;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.RingBuffer;

import javax.naming.InsufficientResourcesException;

/**
 * Created by George on 1/8/14.
 */
public abstract class AbstractDisruptorService<T extends BlockEvent> extends AbstractService {
    private RingBuffer<T> ringBuffer;

    public AbstractDisruptorService() {
    }

    protected void doStart() {
    }

    protected void doStop() {

    }

    public void publishEvent(EventTranslator<T> translator) {
        long next = ringBuffer.next();
        {
            T blockEvent = ringBuffer.get(next);
            translator.translateTo(blockEvent, next);
        }
        ringBuffer.publish(next);
    }

    public void tryPublishEvent(EventTranslator<T> translator) throws InsufficientResourcesException {
        long next = ringBuffer.next();
        {
            T blockEvent = ringBuffer.get(next);
            translator.translateTo(blockEvent, next);
        }
        ringBuffer.publish(next);
    }

    protected abstract RingBuffer<T> startDisruptor() throws Exception;

    protected abstract void stopDisruptor();

}
