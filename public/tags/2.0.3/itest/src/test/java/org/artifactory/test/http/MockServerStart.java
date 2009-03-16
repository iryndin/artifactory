package org.artifactory.test.http;

import org.artifactory.test.mock.MockServer;
import org.testng.annotations.Test;

/**
 * TODO: documentation
 *
 * @author Noam Tenne
 */
public class MockServerStart {

    @Test
    public void runMockTestServer() throws InterruptedException {
        System.out.println("Starting localhost server on 8090...");
        MockServer.start("localhost");
        Object forever = new Object();
        synchronized (forever) {
            forever.wait();
        }
    }
}
