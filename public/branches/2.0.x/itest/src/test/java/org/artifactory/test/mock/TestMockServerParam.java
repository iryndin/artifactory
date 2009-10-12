package org.artifactory.test.mock;

import com.thoughtworks.xstream.XStream;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;

/**
 * User: jfrog
 * Date: 25/11/2008
 * Time: 13:33:01
 */
public class TestMockServerParam {
    @Test
    public void test() throws Exception {
        MockTest repo1 = new MockTest("repo1");
        repo1.setRootPath("C:\\Documents and Settings\\isracard\\.m2\\repository");
        XStream xStream = new XStream();
        xStream.processAnnotations(MockTest.class);
        File file = new File("repo1.xml");
        FileWriter writer = new FileWriter(file);
        xStream.toXML(repo1, writer);
        writer.close();
        StartDummyRepo.main(new String[]{file.getAbsolutePath()});

        // http://localhost:8090/repo1/ant/ant/1.5.4/ant-1.5.4.jar

    }
}
