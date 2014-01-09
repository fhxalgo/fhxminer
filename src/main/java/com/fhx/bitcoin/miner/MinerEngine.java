package com.fhx.bitcoin.miner;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;


/**
 * Created by George on 1/9/14.
 */
public class MinerEngine {

    public static void main(String[] args) {
        String path = MinerEngine.class.getClassLoader().getResource("config.properties").getFile();

        System.out.println("setting config.properties: " + path);
        System.getProperties().setProperty("config.properties", path);

        Injector injector = Guice.createInjector(new ConfigModule(), new MinerModule());
        ServiceManager serviceManager = new ServiceManager(ImmutableList.of(injector.getInstance(DisruptorService.class)));
        serviceManager.startAsync();
        serviceManager.awaitHealthy();
    }
}
