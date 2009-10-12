package org.artifactory.test.internal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.request.DownloadService;
import org.artifactory.jcr.schedule.WorkingCopyCommitter;
import org.artifactory.schedule.TaskCallback;
import org.artifactory.schedule.TaskService;
import org.artifactory.spring.ArtifactoryApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Tests the behavior of Artifactory when a repository import is done and in the same time clients
 * asks for one or more files which are currently imported.
 * <p/>
 * This test requires a valid export directory to use to initiate an import before the test begins.
 */
public class WcCommiterTest extends ArtifactoryTestBase {
    private final static Logger log = LoggerFactory.getLogger(WcCommiterTest.class);

    @BeforeClass
    public void setupWcDir() throws IOException, InterruptedException, URISyntaxException {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger(WcCommiterTest.class).setLevel(Level.DEBUG);
        lc.getLogger(TaskCallback.class).setLevel(Level.DEBUG);
        lc.getLogger(ArtifactoryApplicationContext.class).setLevel(Level.DEBUG);

        importToRepoFromExportPath("/export/WcTest", "libs-releases-local", true);
        TaskService taskService = context.getTaskService();
        taskService.stopTasks(WorkingCopyCommitter.class, true);
    }

    @Test(invocationCount = 20, threadPoolSize = 10)
    public void concurrentWcComitter() throws Exception {
        log.info("### Thread started for concurrentWcComitter");
        DownloadService downloadService = context.beanForType(DownloadService.class);
        ArtifactoryRequestStub request =
                new ArtifactoryRequestStub("biz/aQute/bndlib/0.0.227/bndlib-0.0.227.pom");
        ArtifactoryResponseStub response = new ArtifactoryResponseStub();
        //Download it
        log.info("Requesting " + request.getPath() + " ... ");
        downloadService.process(request, response);
        //Assert
        assertTrue(response.isSuccessful(),
                "Request failed with failure status: " + response.getStatus() + " reason=" +
                        response.getReason());
        assertEquals(response.getContentLength(), 950);//size of bndlib-0.0.227.pom
        assertTrue(response.getLastModified() > 0);
        assertEquals(response.getContentType(), ContentType.mavenPom.getMimeType());
    }

    @Override
    String getConfigName() {
        return "no-remote-repo";
    }
}