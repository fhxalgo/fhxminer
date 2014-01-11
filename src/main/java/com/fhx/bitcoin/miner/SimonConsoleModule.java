package com.fhx.bitcoin.miner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import org.javasimon.SimonManager;
import org.javasimon.callback.CompositeCallback;
import org.javasimon.console.SimonConsoleServlet;
import org.javasimon.jmx.JmxRegisterCallback;
import org.javasimon.jmx.SimonManagerMXBean;
import org.javasimon.jmx.SimonManagerMXBeanImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Created by George on 1/9/14.
 */
public class SimonConsoleModule extends ServletModule {
    private static final Logger log = LoggerFactory.getLogger(SimonConsoleModule.class);

    static {
        registerSimon();
    }

    private static void registerSimon() {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        CompositeCallback callback = SimonManager.callback();
        if (callback != null) {
            callback.addCallback(new JmxRegisterCallback(mBeanServer, "Simon"));
        }

        try {
            ObjectName name = new ObjectName("Simon:type=SimonManager");
            if (mBeanServer.isRegistered(name)) {
                mBeanServer.unregisterMBean(name);
            }

            SimonManagerMXBean simon = new SimonManagerMXBeanImpl(SimonManager.manager());
            mBeanServer.registerMBean(simon, name);
            log.info("SimonMXBean registered under name : " + name);
        } catch (Exception e) {
            log.info("SimonMXBean registration failed!", e);
        }
    }

    @Override
    protected void configureServlets() {
        serve("/simon/*").with(SimonConsoleServlet.class, ImmutableMap.of("url-prefix", "/simon"));
    }

    @Provides
    @Singleton
    SimonConsoleServlet providesSimonConsoleServlet() {
        return new SimonConsoleServlet();
    }
}
