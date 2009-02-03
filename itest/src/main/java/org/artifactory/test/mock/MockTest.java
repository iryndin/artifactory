package org.artifactory.test.mock;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Noam
 * Date: Sep 11, 2008
 * Time: 3:24:40 PM
 */
@XStreamAlias("test")
public class MockTest {
    public String name;
    public List<MockPathTest> paths = new ArrayList<MockPathTest>();

    public MockTest() {
    }

    public MockTest(String name) {
        this.name = name;
    }
}
