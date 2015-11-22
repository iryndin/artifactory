package org.artifactory.ui.rest.model.artifacts.deploy;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Unit tests for {@link UploadArtifactInfo}.
 *
 * @author Yossi Shaul
 */
@Test
public class UploadArtifactInfoTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void failSetFileNameWithRelativePath() {
        new UploadArtifactInfo().setFileName("../hack");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void failGetFileNameWithRelativePath() throws Exception {
        UploadArtifactInfo i = new UploadArtifactInfo();
        ReflectionTestUtils.setField(i, "fileName", "../hack");
        i.getFileName(); // should throw IllegalArgumentException
    }

    public void validSetGetFileName() {
        UploadArtifactInfo i = new UploadArtifactInfo();
        i.setFileName("ok");
        assertEquals(i.getFileName(), "ok");
    }
}