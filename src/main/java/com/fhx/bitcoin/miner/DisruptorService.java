package com.fhx.bitcoin.miner;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by George on 1/9/14.
 */
public class DisruptorService extends AbstractDisruptorService<BlockEvent> {
    private static final Logger log = LoggerFactory.getLogger(DisruptorService.class);

    private Disruptor<BlockEvent> disruptor;
    private int hashThreads;
    private int hashCount;

    private ExecutorService executorService;
    private final BitcoinRPCService rpcService;

    public DisruptorService(int hashThreads, int hashCount, BitcoinRPCService rpcService) {
        this.hashThreads = hashThreads;
        this.hashCount = hashCount;
        this.rpcService = rpcService;
    }

    @Override
    protected void doStart() {
        log.info("Starting DisruptorService...");
        try {
            startDisruptor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("DisruptorService started");
    }

    @Override
    protected void doStop() {
        log.info("Stopping DisruptorService...");
        stopDisruptor();
        log.info("DisruptorService stopped");
    }

    @Override
    protected RingBuffer<BlockEvent> startDisruptor() throws Exception {
        executorService = Executors.newCachedThreadPool();
        //WaitStrategy waitStrategy = new SleepingWaitStrategy();

        disruptor = new Disruptor<BlockEvent>(
                BlockEvent.BLOCK_EVENT_FACTORY,
                8192,
                executorService
        );

        disruptor.handleEventsWith(new InputHandler());
        for (int i=0; i<hashThreads; i++) {
            disruptor.handleEventsWith(new MinerHashHandler(i, hashCount));
        }

        this.rpcService.setDisruptor(disruptor);

        return disruptor.start();
    }

    @Override
    protected void stopDisruptor() {
        disruptor.halt();
        rpcService.doStop();
        executorService.shutdown();
    }
}
