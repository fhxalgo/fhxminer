package com.fhx.bitcoin.miner;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Slf4jLog;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import javax.servlet.DispatcherType;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Set;

import static java.util.EnumSet.allOf;

/**
 * Created by George on 1/9/14.
 */
public class JettyServletModule extends ServletModule {
    private static final Logger log = LoggerFactory.getLogger(JettyServletModule.class);

    @Override
    protected void configureServlets() {
        bind(Server.class).toProvider(JettyServerProvider.class).asEagerSingleton();
    }

    static class JettyServerProvider implements Provider<Server> {

        @Inject
        Injector injector;

        @Inject (optional = true)
        Set<WarEntry> warEntries;

        @Inject (optional = true)
        SecurityHandler securityHandler;

        @Inject (optional = true)
        @Named("jetty.port")
        private int port = 8080;
        private Server server;

        @Override
        public Server get() {
            System.setProperty("org.eclipse.jetty.util.log.class", Slf4jLog.class.getName());
            Log.setLog(new Slf4jLog(("jetty")));

            log.info("......starting Server at port: " + port);
            server = new Server(port);

            ContextHandlerCollection handlerCollection = new ContextHandlerCollection();

            ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            servletContextHandler.setSessionHandler(new SessionHandler());

            {
                servletContextHandler.addEventListener(new GuiceServletContextListener() {
                    @Override
                    protected Injector getInjector() {
                        return injector;
                    }
                });
                servletContextHandler.addFilter(GuiceFilter.class, "/*", allOf(DispatcherType.class));
            }

            ServletHolder servletHolder = new ServletHolder(DefaultServlet.class);
            servletHolder.setAsyncSupported(true);
            servletContextHandler.addServlet(servletHolder, "/");
            handlerCollection.addHandler(servletContextHandler);

            {
                if (warEntries != null) {
                    for (WarEntry warEntry : warEntries) {
                        WebAppContext webAppContext = new WebAppContext();
                        webAppContext.setWar(warEntry.getWarPath());
                        webAppContext.setContextPath(warEntry.getContextPath());
                        webAppContext.setParentLoaderPriority(true);

                        //webAppContext.setExtractWAR(false);
                        //webAppContext.setCopyWebInf(true);

                        webAppContext.setSessionHandler(new SessionHandler());
                        if (warEntry.getParameters() != null) {
                            for (Map.Entry<String, String> entry : warEntry.getParameters().entrySet()) {
                                webAppContext.setInitParameter(entry.getKey(), entry.getValue());

                            }
                        }

                        if (securityHandler != null) {
                            webAppContext.setSecurityHandler(securityHandler);
                        }

                        handlerCollection.addHandler(webAppContext);
                    }
                }
            }

            {
                WebAppContext webAppContext = new WebAppContext();
                webAppContext.setContextPath("/files");
                webAppContext.setResourceBase(".");
                handlerCollection.addHandler(webAppContext);
            }
            server.setHandler(handlerCollection);

            MBeanContainer mBeanContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
            server.getContainer().addEventListener(mBeanContainer);
            server.addBean(mBeanContainer);

            try {
                server.start();
            } catch (Exception e) {
                throw new IllegalStateException("Error starting Jetty Server", e);
            }

            return server;
        }
    }

    class WarEntry {
        private final String warPath;
        private final String contextPath;
        private final Map<String, String> parameters;

        public WarEntry(String contextPath, String warPath, Map<String, String> parameters) {
            this.contextPath = contextPath;
            this.warPath = warPath;
            this.parameters = parameters;
        }

        String getWarPath() {
            return warPath;
        }

        String getContextPath () {
            return contextPath;
        }

        Map<String, String> getParameters() {
            return parameters;
        }
    }
}
