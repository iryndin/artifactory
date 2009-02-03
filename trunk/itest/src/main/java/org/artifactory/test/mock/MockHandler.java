package org.artifactory.test.mock;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.handler.DefaultHandler;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Noam Date: Sep 11, 2008 Time: 3:07:54 PM
 */
public class MockHandler extends DefaultHandler {

    private static final Map<String, MockTest> allTests = new HashMap<String, MockTest>();
    private static final Map<String, TestStats> statsMap = new HashMap<String, TestStats>();
    private static final String CONFIG_PREFIX = "/config";
    private static final String EXIST_PREFIX = "/existence";
    private static final String STATS_PREFIX = "/stats";
    private XStream xStream;

    public MockHandler() {
        super();
    }

    /*
     * @see org.mortbay.jetty.Handler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    @Override
    public void handle(String target, HttpServletRequest request, HttpServletResponse response,
            int dispatch) throws IOException, ServletException {

        String pathInfo = request.getPathInfo();

        if (pathInfo.startsWith(EXIST_PREFIX)) {
            //If there is an existence check being performed
            handleExist(response);
        } else if (pathInfo.startsWith(CONFIG_PREFIX)) {
            handleConfig(request, response, pathInfo);
        } else if (pathInfo.startsWith(STATS_PREFIX)) {
            handleStats(request, response, pathInfo);
        } else {// Path request
            initializeTest(request, response, pathInfo);
        }
    }

    private void handleExist(HttpServletResponse response) throws IOException {
        //Return confirmation
        response.setContentType("text/plain; charset=utf-8");
        response.setStatus(HttpStatus.SC_OK);
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(("Mock Test Server is running.").getBytes("utf-8"));
        outputStream.flush();
    }

    private void handleConfig(HttpServletRequest request, HttpServletResponse response,
            String pathInfo) throws IOException {
        //If the path start with the configuration prefix means the user wants to configure a test
        if (!pathInfo.endsWith(CONFIG_PREFIX)) {
            //If the path does not end with the configuration prefix (when it does, the user has
            // either not specified a test, or named his test 'config')

            //Get test name from the url
            String urlTestName = pathInfo.substring(
                    (pathInfo.lastIndexOf(CONFIG_PREFIX) + CONFIG_PREFIX.length()) + 1);

            //If the config request is of PUT type
            if ("PUT".equalsIgnoreCase(request.getMethod())) {
                //Creat mock test and xStream instance to read the test
                MockTest test;
                XStream xStream = getXStream();
                test = (MockTest) xStream.fromXML(request.getReader());
                addTest(response, urlTestName, test);
            }
            //If the config request is of GET type
            else if ("GET".equalsIgnoreCase(request.getMethod())) {
                //Search for selected test
                MockTest foundTest = allTests.get(urlTestName);
                getTest(response, foundTest);
            }
            //If the config request is of POST type
            else if ("POST".equalsIgnoreCase(request.getMethod())) {
                //Get sent path test
                MockPathTest mockPathTest =
                        (MockPathTest) getXStream().fromXML(request.getReader());
                //Look for the path test to replace
                replaceMockPathTest(urlTestName, mockPathTest);
            }
        } else {
            //If there is no test name specified, or if the test is named 'config'
            sendError(response, HttpStatus.SC_NOT_FOUND,
                    "Cannot configure a test named 'config'.");
        }
    }

    private void handleStats(HttpServletRequest request, HttpServletResponse response,
            String pathInfo)
            throws IOException {
        if ("GET".equalsIgnoreCase(request.getMethod())) {

            //Get test name from the url
            String urlTestName = pathInfo.substring(
                    (pathInfo.lastIndexOf(STATS_PREFIX) + STATS_PREFIX.length()) + 1);

            TestStats foundStats = statsMap.get(urlTestName);
            if (foundStats != null) {
                //Stream the selected test in XML
                String selectedTest = getXStream().toXML(foundStats);
                response.setContentType("application/xml");
                response.setStatus(HttpStatus.SC_OK);
                ServletOutputStream outputStream = response.getOutputStream();
                outputStream.write(selectedTest.getBytes("utf-8"));
                outputStream.flush();
            } else {
                //If the test is not found
                sendError(response, HttpStatus.SC_NOT_FOUND,
                        "Cannot find requested test stats. Please configure it.");
            }
        }
    }

    private void initializeTest(HttpServletRequest request, HttpServletResponse response,
            String pathInfo) throws IOException {
        // look for it in the collection
        MockTest parentTest = allTests.get(getServerNameFromPath(pathInfo));
        MockPathTest pathTest = searchMockPathTest(parentTest, pathInfo);
        if (pathTest != null) {
            // Configured test was found
            PathStats pathStats = sendResponse(request, response, pathTest);
            TestStats testsStats = statsMap.get(parentTest.getName());
            testsStats.addPathStats(pathTest.path, pathStats);

        } else if ((parentTest != null) && (parentTest.getRootPath() != null)) {
            createPathTest(parentTest, getRelativePathFromPath(pathInfo));
            initializeTest(request, response, pathInfo);
        } else {
            // No configured test found
            sendError(response, HttpStatus.SC_NOT_FOUND,
                    "Cannot find requested test. Please configure it.");
        }
    }

    private void addTest(HttpServletResponse response, String urlTestName, MockTest test)
            throws IOException {
        //If the test specified in the url is the same as in the xml
        if (urlTestName.equals(test.getName())) {
            //Add test and send confirmation response
            allTests.put(test.getName(), test);
            statsMap.put(test.getName(), new TestStats(test.getName()));
            response.setContentType("text/plain; charset=utf-8");
            response.setStatus(HttpStatus.SC_OK);
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(("OK for " + test.getName()).getBytes("utf-8"));
            outputStream.flush();
        }
        //If the test names in the url and xml do not match
        else {
            sendError(response, HttpStatus.SC_NOT_FOUND,
                    "The name of the test in the URL does not match the name of the test in the XML.");
        }
    }

    private void getTest(HttpServletResponse response, MockTest test) throws IOException {
        if (test != null) {
            //Stream the selected test in XML
            XStream xStream = getXStream();
            String selectedTest = xStream.toXML(test);
            response.setContentType("application/xml");
            response.setStatus(HttpStatus.SC_OK);
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(selectedTest.getBytes("utf-8"));
            outputStream.flush();
        } else {
            //If the test is not found
            sendError(response, HttpStatus.SC_NOT_FOUND,
                    "Cannot find requested test. Please configure it.");
        }
    }

    private void createPathTest(MockTest test, String relativePath) {
        allTests.get(test.getName()).addPath(new MockPathTest(relativePath));
    }

    /**
     * Responds to the user's testing request
     */
    private PathStats sendResponse(HttpServletRequest request,
            HttpServletResponse response, MockPathTest pathTest) throws IOException {
        PathStats pathStats = new PathStats(pathTest.returnCode, request.getMethod());

        response.setStatus(pathTest.returnCode);

        ServletOutputStream outputStream;

        // Wait before sending the header
        if (pathTest.timeToTakeForHeader != 0) {
            long timeout = pathTest.timeToTakeForHeader * 1000;
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return new PathStats(HttpStatus.SC_INTERNAL_SERVER_ERROR, request.getMethod());
            }
        }

        //If the return code is not 200 (O.K.)
        if (pathTest.returnCode != HttpStatus.SC_OK) {
            response.setStatus(pathTest.returnCode);
            outputStream = response.getOutputStream();
            sendError(outputStream, pathTest.reason);
            return pathStats;
        } else {

            outputStream = response.getOutputStream();
        }

        response.setDateHeader(HttpHeaders.LAST_MODIFIED, System.currentTimeMillis());
        if (pathTest.lastModified != 0) {
            response.setDateHeader(HttpHeaders.LAST_MODIFIED, pathTest.lastModified);
        }
        response.setContentType(pathTest.contentType);

        // If the return code is 200, send some data
        if ((pathTest.urlContent != null) || (pathTest.parentTest.getRootPath() != null)) {

            if ("HEAD".equalsIgnoreCase(request.getMethod())) {
                File contentFile = null;
                String errorMsg = null;
                if (pathTest.parentTest.getRootPath() != null) {
                    contentFile = new File(pathTest.parentTest.getRootPath() + pathTest.path);
                    if (!contentFile.exists()) {
                        errorMsg = "The requested file " + pathTest.path +
                                " was not found in the server root path " +
                                pathTest.parentTest.getRootPath();
                        contentFile = null;
                    }
                } else {
                    URL contentURL = MockHandler.class.getResource(pathTest.urlContent);
                    if (contentURL != null) {
                        contentFile = new File(contentURL.getFile());
                    }
                    if (contentFile == null || !contentFile.exists()) {
                        errorMsg = "The requested file " + pathTest.path +
                                " was not found in the calls path " +
                                pathTest.urlContent;
                        contentFile = null;
                    }
                }

                if (contentFile != null) {
                    if (pathTest.lastModified != 0) {
                        response.setDateHeader(HttpHeaders.LAST_MODIFIED,
                                contentFile.lastModified());
                    }
                    response.setContentLength(Integer.parseInt(contentFile.length() + ""));
                    response.setStatus(HttpStatus.SC_OK);
                    outputStream.flush();
                } else {
                    sendError(response, HttpStatus.SC_NOT_FOUND, errorMsg);
                }
            } else {
                //Get input stream and convert the content to bytes
                InputStream inputStream;
                if (pathTest.parentTest.getRootPath() != null) {
                    inputStream =
                            new FileInputStream(pathTest.parentTest.getRootPath() + pathTest.path);
                } else {
                    inputStream = MockHandler.class.getResourceAsStream(pathTest.urlContent);
                }
                boolean cutStream;
                if (inputStream != null) {
                    byte[] byteBuffer = IOUtils.toByteArray(inputStream);

                    // Set in force the content length independently of the url content size
                    if (pathTest.contentLength != 0) {
                        response.setContentLength(pathTest.contentLength);
                    } else {
                        response.setContentLength(byteBuffer.length);
                    }

                    //Set byte limt (either part or full length)
                    int byteLimit;
                    if (pathTest.breakPipeAfter != 0) {
                        byteLimit = pathTest.breakPipeAfter;
                        cutStream = true;
                    } else {
                        byteLimit = byteBuffer.length;
                        cutStream = false;
                    }

                    //Set default byte offset and check if we need to postpone
                    int offset = 0;
                    if (pathTest.timeToTakeForData != 0) {

                        //Calculate how many bytes to transfer in each iteration
                        int bytesPerThreeSecs = (byteLimit * 3) / pathTest.timeToTakeForData;

                        for (int i = 0;
                             i < pathTest.timeToTakeForData && byteLimit > bytesPerThreeSecs;
                             i += 3) {

                            //Write limited amount of bytes
                            outputStream.write(byteBuffer, offset, bytesPerThreeSecs);
                            outputStream.flush();
                            offset += bytesPerThreeSecs;
                            byteLimit -= bytesPerThreeSecs;

                            //Wait for 3 seconds
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                return new PathStats(HttpStatus.SC_INTERNAL_SERVER_ERROR,
                                        request.getMethod());
                            }
                        }
                    }

                    //Write leftover bytes and flush response
                    outputStream.write(byteBuffer, offset, byteLimit);
                    response.flushBuffer();

                } else {
                    sendError(response, HttpStatus.SC_NOT_FOUND,
                            "The requested file was not found in the supplied URL.");
                    return new PathStats(HttpStatus.SC_NOT_FOUND, request.getMethod());
                }

                // If need some cut close brutally (Broken Pipe)
                if (cutStream) {
                    outputStream.close();
                }
            }
        } else {
            // no url content
            sendError(response, HttpStatus.SC_NO_CONTENT,
                    "No urlContent configured for the path test");
            return new PathStats(HttpStatus.SC_NO_CONTENT, request.getMethod());
        }

        return pathStats;
    }

    /**
     * Searches and returns a MockPathTest via path (test.getName()/mock.path.test.getName())
     *
     * @param pathInfo
     * @return
     */
    private MockPathTest searchMockPathTest(MockTest mockTest, String pathInfo) {
        String relativePath = getRelativePathFromPath(pathInfo);
        if (mockTest != null) {
            return findByPath(mockTest, relativePath);
        }
        return null;
    }

    /**
     * Replace the mock test path at the input pathInfo with the input replacement.
     *
     * @param testName    The test name
     * @param replacement The replacement
     * @return The replaced object (null if not found)
     */
    private MockPathTest replaceMockPathTest(String testName, MockPathTest replacement) {

        MockTest mockTest = allTests.get(testName);
        MockPathTest foundPath = null;
        if (mockTest != null) {
            foundPath = findByPath(mockTest, replacement.path);
            if (foundPath != null) {
                replacement.parentTest = mockTest;
                mockTest.getPaths().remove(foundPath);
                mockTest.getPaths().add(replacement);
            }
        }
        return foundPath;
    }

    private MockPathTest findByPath(MockTest mockTest, String path) {
        List<MockPathTest> mockPathTests = mockTest.getPaths();
        for (MockPathTest pathTest : mockPathTests) {
            if ((path.equals("/" + pathTest.path)) || (path.equals(pathTest.path))) {
                // mock test was found for the request path
                return pathTest;
            }
        }
        return null;
    }

    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        sendError(response.getOutputStream(), message);
    }

    /**
     * Sends the user an error message with a customizable status and message
     *
     * @param message
     * @throws IOException
     */
    private void sendError(ServletOutputStream outputStream, String message)
            throws IOException {
        if (message != null) {
            outputStream.write((message).getBytes("utf-8"));
        }
        outputStream.flush();
    }

    private XStream getXStream() {
        if (xStream == null) {
            xStream = new XStream();
            xStream.processAnnotations(MockTest.class);
            xStream.alias("mockpathtest", MockPathTest.class);
        }
        return xStream;
    }

    private String getServerNameFromPath(String pathInfo) {
        int secondPathSeperator = getPathStartIndex(pathInfo);
        return pathInfo.substring(1, secondPathSeperator);
    }

    private String getRelativePathFromPath(String pathInfo) {
        int secondPathSeperator = getPathStartIndex(pathInfo);
        return pathInfo.substring(secondPathSeperator + 1, pathInfo.length());
    }

    private int getPathStartIndex(String pathInfo) {
        int secondPathSeperator = StringUtils.indexOf(pathInfo, "/", 1);
        if (secondPathSeperator < 0) {
            throw new IllegalArgumentException("Invalid path: " + pathInfo);
        }
        return secondPathSeperator;
    }
}
