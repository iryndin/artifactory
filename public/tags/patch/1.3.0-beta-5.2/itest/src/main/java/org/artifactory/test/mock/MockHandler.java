package org.artifactory.test.mock;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.handler.DefaultHandler;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Noam
 * Date: Sep 11, 2008
 * Time: 3:07:54 PM
 */
public class MockHandler extends DefaultHandler {

    private static final Map<String, MockTest> allTests = new HashMap<String, MockTest>();
    private static final String CONFIG_PREFIX = "/config";
    private static final String EXIST_PREFIX = "/existence";
    private XStream xStream;

    public MockHandler() {
        super();
    }

    /*
     * @see org.mortbay.jetty.Handler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    @Override
    public void handle(String target, HttpServletRequest request, HttpServletResponse response,
            int dispatch)
            throws IOException, ServletException {

        //Get path info
        String pathInfo = request.getPathInfo();

        //If there is an existence check being performed
        if (pathInfo.startsWith(EXIST_PREFIX)) {
            //Return confirmation
            response.setContentType("text/plain; charset=utf-8");
            response.setStatus(HttpStatus.SC_OK);
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(("Mock Test Server is running.").getBytes("utf-8"));
            outputStream.flush();

            return;
        }

        //If the path start with the configuration prefix (means the user wants to configure a test
        if (pathInfo.startsWith(CONFIG_PREFIX)) {
            //If the path does not end with the configuration prefix (when it does, the user has either not specified a test, or named his test 'config'
            if (!pathInfo.endsWith(CONFIG_PREFIX)) {
                //Get test name from the url
                String urlTestName = pathInfo.substring(
                        (pathInfo.lastIndexOf(CONFIG_PREFIX) + CONFIG_PREFIX.length()) + 1);

                //If the config request is of PUT type
                if ("PUT".equalsIgnoreCase(request.getMethod())) {
                    //Creat mock test and xStream instance to read the test
                    MockTest test;
                    XStream xStream = getXStream();
                    test = (MockTest) xStream.fromXML(request.getReader());
                    //If the test specified in the url is the same as in the xml
                    if (urlTestName.equals(test.getName())) {
                        //Add test and send confirmation response
                        allTests.put(test.getName(), test);
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
                //If the config request is of GET type
                else if ("GET".equalsIgnoreCase(request.getMethod())) {

                    //Search for selected test
                    Collection<MockTest> testCollection = allTests.values();
                    boolean testFound = false;
                    for (MockTest test : testCollection) {
                        if (urlTestName.equalsIgnoreCase(test.getName())) {
                            testFound = true;
                            //Stream the selected test in XML
                            XStream xStream = getXStream();
                            String selectedTest = xStream.toXML(test);
                            response.setContentType("application/xml");
                            response.setStatus(HttpStatus.SC_OK);
                            ServletOutputStream outputStream = response.getOutputStream();
                            outputStream.write(selectedTest.getBytes("utf-8"));
                            outputStream.flush();
                        }
                    }
                    //If the test is not found
                    if (!testFound) {
                        sendError(response, HttpStatus.SC_NOT_FOUND,
                                "Cannot find requested test. Please configure it.");
                    }
                }
                //If the config request is of POST type
                else if ("POST".equalsIgnoreCase(request.getMethod())) {
                    //Get sent path test
                    MockPathTest mockPathTest;
                    XStream xStream = getXStream();
                    mockPathTest = (MockPathTest) xStream.fromXML(request.getReader());
                    //Look for the path test to replace
                    searchMockPathTest(pathInfo, mockPathTest);
                }
                return;
            }
            //If there is no test name specified, or if the test is named 'config'
            else {
                sendError(response, HttpStatus.SC_NOT_FOUND,
                        "Cannot configure a test named 'config'.");
                return;
            }
        }

        //In the case that the user has requested a certain test, look for it in the collection
        MockPathTest pathTest = searchMockPathTest(pathInfo, null);
        //If the test was found
        if (pathTest != null) {
            if (sendResponse(response, pathTest)) {
                return;
            }
        } else {
            //If the test wasn't found
            sendError(response, HttpStatus.SC_NOT_FOUND,
                    "Cannot find requested test. Please configure it.");
            return;
        }

        sendError(response, HttpStatus.SC_NOT_FOUND, "Refactor me.");
    }

    /**
     * Responds to the user's testing request
     */
    private boolean sendResponse(HttpServletResponse response, MockPathTest pathTest)
            throws IOException {
        response.setStatus(pathTest.returnCode);

        ServletOutputStream outputStream;

        //If the return code is not 200 (O.K.)
        if (pathTest.returnCode != HttpStatus.SC_OK) {
            sendError(response, pathTest.returnCode, pathTest.reason);
            return true;
        }

        response.setDateHeader(HttpHeaders.LAST_MODIFIED, System.currentTimeMillis());
        response.setContentType(pathTest.contentType);

        // If the return code is 200, send some data
        if (pathTest.urlContent != null) {
            // Wait before sending the header
            if (pathTest.timeToTakeForHeader != 0) {
                long timeout = pathTest.timeToTakeForHeader * 1000;
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return true;
                }
            }

            outputStream = response.getOutputStream();

            //Get input stream and convert the content to bytes
            InputStream inputStream = MockHandler.class.getResourceAsStream(pathTest.urlContent);
            boolean cutStream = false;
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

                    for (int i = 0; i < pathTest.timeToTakeForData && byteLimit > bytesPerThreeSecs;
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
                            return true;
                        }
                    }
                }

                //Write leftover bytes and flush response
                outputStream.write(byteBuffer, offset, byteLimit);
                response.flushBuffer();
            }
            else {
                sendError(response, HttpStatus.SC_NOT_FOUND, "The requested file was not found in the supplied URL.");
                return true;
            }

            // If need some cut close brutally (Broken Pipe)
            if (cutStream) {
                outputStream.close();
            }

            return true;
        }
        return false;
    }

    /**
     * Searches and returns a MockPathTest via path (test.getName()/mock.path.test.getName()) The
     * replacement param is optional. If supplied, the given object will replace An existing object
     * with an identical path.
     *
     * @param pathInfo
     * @param replacement
     * @return
     */
    private MockPathTest searchMockPathTest(String pathInfo, MockPathTest replacement) {
        Collection<MockTest> testCollection = allTests.values();
        for (MockTest test : testCollection) {
            if (pathInfo.startsWith("/" + test.getName())) {
                String pathLeft = pathInfo.substring(test.getName().length() + 1);
                List<MockPathTest> mockPathTests = test.getPaths();
                for (MockPathTest pathTest : mockPathTests) {
                    if (pathLeft.startsWith("/" + pathTest.path)) {
                        if (replacement != null) {
                            int pathIndex = mockPathTests.indexOf(pathTest);
                            mockPathTests.remove(pathIndex);
                            mockPathTests.add(replacement);
                        }

                        return pathTest;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Sends the user an error message with a customizable status and message
     *
     * @param response
     * @param status
     * @param message
     * @throws IOException
     */
    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write((message).getBytes("utf-8"));
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
}
