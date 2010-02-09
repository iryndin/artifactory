package org.artifactory.test.mock;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.httpclient.HttpStatus;

/**
 * User: Noam
 * Date: Sep 11, 2008
 * Time: 3:19:39 PM
 */
@XStreamAlias("testPath")
public class MockPathTest {
    public String path;
    /**
     * If the value is not HttpStatus.SC_OK do sendError(code,reason)
     */
    public int returnCode = HttpStatus.SC_OK;
    ;
    public String contentType;
    public String reason;
    /**
     * If set call response.setContentLength() with it
     */
    public int contentLength;
    /**
     * Can be a remote HTTP content need to be copied in local /tmp folder and save the FIle obj in a local Map
     */
    public String urlContent;
    /**
     * If set after the amount of bytes send on the response output stream, close it.
     */
    public int breakPipeAfter;
    /**
     * The amount of seconds before sending the first response header
     */
    public int timeToTakeForHeader;
    /**
     * The amount of seconds to take sending the response data
     */
    public int timeToTakeForData;

    /**
     * Simple constructor for path
     *
     * @param path
     */
    public MockPathTest(String path) {
        this.path = path;
    }

    /**
     * Constructor for simulating a pipe break
     *
     * @param path
     * @param contentType
     * @param urlContent
     * @param timeToTakeForHeader
     * @param timeToTakeForData
     * @param breakPipeAfter
     */
    public MockPathTest(String path, String contentType, String urlContent, int timeToTakeForHeader, int timeToTakeForData, int breakPipeAfter) {
        this.path = path;
        this.contentType = contentType;
        this.urlContent = urlContent;
        this.timeToTakeForHeader = timeToTakeForHeader;
        this.timeToTakeForData = timeToTakeForData;
        this.breakPipeAfter = breakPipeAfter;
    }

    /**
     * Constructor for returning a 200 with urlContent and pospone the transfer
     *
     * @param path
     * @param contentType
     * @param urlContent
     * @param timeToTakeForHeader
     * @param timeToTakeForData
     */
    public MockPathTest(String path, String contentType, String urlContent, int timeToTakeForHeader, int timeToTakeForData) {
        this.path = path;
        this.contentType = contentType;
        this.urlContent = urlContent;
        this.timeToTakeForHeader = timeToTakeForHeader;
        this.timeToTakeForData = timeToTakeForData;
    }

    /**
     * Constructor for returning a 200 with urlContent
     *
     * @param path
     * @param contentType
     * @param urlContent
     */
    public MockPathTest(String path, String contentType, String urlContent) {
        this.path = path;
        this.contentType = contentType;
        this.urlContent = urlContent;
    }

    /**
     * Constructor for returning an error
     *
     * @param path
     * @param returnCode
     * @param reason
     */
    public MockPathTest(String path, int returnCode, String reason) {
        this.path = path;
        this.returnCode = returnCode;
        this.reason = reason;
    }
}
