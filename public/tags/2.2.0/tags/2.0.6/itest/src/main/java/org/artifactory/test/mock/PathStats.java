package org.artifactory.test.mock;

/**
 * Represent a single request to the mock server.
 * Stores needed information for the statistics collection
 *
 * @author Noam Tenne
 */
public class PathStats {
    /**
     * The return code for the request
     */
    private int returnCode;
    /**
     * The http method of the request 
     */
    private String methodName;

    /**
     * Default constructor
     */
    public PathStats() {}

    /**
     * Constructor with return code and method name setters
     *
     * @param returnCode
     * @param methodName
     */
    public PathStats(int returnCode, String methodName) {
        this.returnCode = returnCode;
        this.methodName = methodName;
    }

    /**
     * Returns the return code
     *
     * @return Return code
     */
    public int getReturnCode() {
        return returnCode;
    }

    /**
     * Returns the HTTP method name
     *
     * @return HTTP method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Sets the return code
     *
     * @param int returnCode
     */
    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    /**
     * Sets the method name
     *
     * @param String methodName
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
