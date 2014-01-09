package com.fhx.bitcoin.miner;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by George on 1/9/14.
 */
public class MinerModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(MinerModule.class);

    private static void startSingletonServices(Service... services) {
        ServiceManager serviceManager = new ServiceManager(ImmutableList.copyOf(services));
        serviceManager.startAsync();
        serviceManager.awaitHealthy();
    }

    public MinerModule() {
    }

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    public DisruptorService provideDisruptorService(
            @Named("hashThreads") int hashThreads,
            @Named("hashCount") int hashCount,
            BitcoinRPCService rpcService)
    {
        DisruptorService disruptorService = new DisruptorService(hashThreads, hashCount, rpcService);
        startSingletonServices(disruptorService);
        return disruptorService;
    }

    @Provides
    @Singleton
    public BitcoinRPCService provideBitcoinRPCService(
        @Named("user") String user,
        @Named("password") String password,
        @Named("url") String url)
    {
        BitcoinRPCService rpcService = new BitcoinRPCService(user, password, url);
        startSingletonServices(rpcService);
        return rpcService;
    }

}
