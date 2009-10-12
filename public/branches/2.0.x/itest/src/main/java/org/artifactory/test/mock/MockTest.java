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

    /**
     * Specifies the test name
     */
    private String name;
    /**
     * Optional - Specifies a root-path. all requests will be diverted to that path
     */
    private String rootPath;
    /**
     * Contains all the MockPathTests which belong to this test
     */
    private List<MockPathTest> paths = new ArrayList<MockPathTest>();

    /**
     * Default constructor
     */
    public MockTest() {
    }

    /**
     * Constructor with test name specification
     * 
     * @param name Test name
     */
    public MockTest(String name) {
        checkName(name);
        this.name = name;
    }

    /**
     * Constructor with test name and rootPath specification
     *
     * @param name Test name
     * @param rootPath A root path to use in the local fs
     */
    public MockTest(String name, String rootPath) {
        checkName(name);
        this.name = name;
        this.rootPath = rootPath;
    }

    /**
     * Returns the test name
     *
     * @return Test name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the test name
     *
     * @param name Test name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the root path
     *
     * @return A root path to use in the local fs
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * Sets the root path
     *
     * @param rootPath A root path to use in the local fs
     */
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * Returns the array list of mock path tests
     *
     * @return ArrayList of MockPathTests
     */
    public List<MockPathTest> getPaths() {
        return paths;
    }

    /**
     * Adds a mock path test object to the collection
     *
     * @param mockPath A MockPathTest object
     */
    public void addPath(MockPathTest mockPath) {
        mockPath.parentTest = this;
        paths.add(mockPath);
    }

    /**
     * Checks a given name to make sure they don't start with a "/"
     *
     * @param name Test name
     */
    private void checkName(String name) {
        if (name.indexOf("/") > 0) {
            throw new IllegalArgumentException("Slashes are not allowed in the test name");
        }
    }
}
