package com.fhx.bitcoin.miner;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

/**
 * Created by George on 1/8/14.
 */
public abstract class LifeCycleAwareEventHandler<T> implements EventHandler<BlockEvent>, LifecycleAware {

    public static final int MAX_TRY = 10;
    ThreadGroup rootThreadGroup = null;

//    @Override
//    public abstract void onEvent(T event, long sequence, boolean endOfBatch) throws Exception;

    @Override
    public void onStart() {
        int tryCount = 0;
        while (tryCount < MAX_TRY) {
            String name = getThreadName() + "-" + tryCount;
            Thread thread = getThread(name);

            if (thread == null) {
                Thread.currentThread().setName(name);
                break;
            }
            tryCount++;
        }
    }

    protected String getThreadName() {
        return this.getClass().getName();
    }

    Thread[] getAllThreads() {
        final ThreadGroup root = getRootThreadGroup();
        final ThreadMXBean thbean = ManagementFactory.getThreadMXBean();

        int nAlloc = thbean.getDaemonThreadCount();
        int n = 0;
        Thread[] threads;

        do {
            nAlloc *= 2;
            threads = new Thread[nAlloc];
            n = root.enumerate(threads, true);
        } while (n == nAlloc);

        return Arrays.copyOf(threads, n);
    }

    ThreadGroup getRootThreadGroup() {
        if (rootThreadGroup != null) {
            return rootThreadGroup;
        }

        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        ThreadGroup ptg;

        while ((ptg = tg.getParent()) != null) {
            tg = ptg;
        }

        return tg;
    }

    Thread getThread(final String name) {
        if (name == null) {
            throw new NullPointerException("Null name");
        }

        final Thread[] threads = getAllThreads();

        for (Thread thread : threads) {
            if (thread.getName().equals(name)) {
                return thread;
            }
        }

        return null;
    }

    @Override
    public void onShutdown() {

    }

}
