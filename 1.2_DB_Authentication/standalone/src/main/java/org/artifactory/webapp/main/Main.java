package org.artifactory.webapp.main;

import org.apache.log4j.Logger;
import org.mortbay.jetty.Server;
import org.mortbay.xml.XmlConfiguration;

import java.net.URL;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class Main {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(Main.class);

    private static final Logger log = Logger.getLogger(Main.class);

    /**
     * Main function, starts the jetty server.
     *
     * @param args
     */
    public static void main(String[] args) {
        Server server = null;
        try {
            URL configUrl = new URL("file:etc/jetty.xml");
            XmlConfiguration xmlConfiguration = new XmlConfiguration(configUrl);
            server = new Server();
            xmlConfiguration.configure(server);
            server.start();
        } catch (Exception e) {
            log.fatal("Could not start the Jetty server: " + e);
            if (server != null) {
                try {
                    server.stop();
                } catch (Exception e1) {
                    log.fatal("Unable to stop the jetty server: " + e1);
                }
            }
        }
    }
}
