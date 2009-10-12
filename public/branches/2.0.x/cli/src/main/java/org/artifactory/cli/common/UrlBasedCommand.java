package org.artifactory.cli.common;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.lang.ArrayUtils;
import org.artifactory.cli.main.CliOption;
import org.artifactory.cli.main.CommandDefinition;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Extends the BaseCommand class to act as a super class for commands that need URL handling (import, export, etc')
 *
 * @author Noam Tenne
 */
public abstract class UrlBasedCommand extends BaseCommand {
    private static final Logger log = LoggerFactory.getLogger(UrlBasedCommand.class);

    /**
     * Default server host
     */
    private static final String SERVER_HOST = "localhost:8081/";
    /**
     * URL for Rest API
     */
    private static final String API_URI = "artifactory/api/";

    /**
     * URL String Builder
     */
    private StringBuilder url = new StringBuilder();

    /**
     * Default constructor
     *
     * @param commandDefinition The command defintion
     * @param extraOptions      Extra CLI option (if needed, as well as the global ones)
     */
    public UrlBasedCommand(CommandDefinition commandDefinition, CliOption... extraOptions) {
        super(commandDefinition, addExtra(extraOptions));
    }

    /**
     * Recieves the extra options from the command class and adds it to the existing global ones
     *
     * @param extraOptions Any needed extra CLI options
     * @return CliOption[] Options needed for the command
     */
    protected static CliOption[] addExtra(CliOption... extraOptions) {
        CliOption[] baseOptions = {
                CliOption.username,
                CliOption.password,
                CliOption.host,
                CliOption.ssl,
                CliOption.timeout,
                CliOption.url
        };
        CliOption[] allOptions = (CliOption[]) ArrayUtils.addAll(baseOptions, extraOptions);
        return allOptions;
    }

    /**
     * Handles the URL
     *
     * @param url
     * @param passedUrl
     */
    private static void addUrl(StringBuilder url, String passedUrl) {
        url.append(passedUrl);
        if (passedUrl.charAt(passedUrl.length() - 1) != '/') {
            url.append("/");
        }
    }

    /**
     * Returns the URL
     *
     * @return String Host URL
     */
    public String getURL() {
        if (CliOption.url.isSet()) {
            String passedUrl = CliOption.url.getValue().trim();
            addUrl(url, passedUrl);
        } else {
            if (CliOption.ssl.isSet()) {
                url.append("https://");
            } else {
                url.append("http://");
            }
            if (CliOption.host.isSet()) {
                String passedUrl = CliOption.host.getValue().trim();
                addUrl(url, passedUrl);
            } else {
                url.append(SERVER_HOST);
            }
            url.append(API_URI);
        }
        return url.toString();
    }

    @SuppressWarnings({"unchecked"})
    protected static <I, O> O post(String uri, I inObj, Class<O> outObjClass)
            throws Exception {
        XStream xStream = new XStream();
        xStream.processAnnotations(inObj.getClass());
        if (outObjClass != null) {
            xStream.processAnnotations(outObjClass);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        xStream.toXML(inObj, bos);
        if (outObjClass != null) {
            byte[] bytes = post(uri, bos.toByteArray(), "application/xml", 200, "application/xml",
                    false);
            return (O) xStream.fromXML(new ByteArrayInputStream(bytes));
        } else {
            post(uri, bos.toByteArray(), "application/xml", 200, null, true);
            return null;
        }
    }

    @SuppressWarnings({"unchecked"})
    protected static <T> T get(String uri, Class<T> xstreamObjClass)
            throws Exception {
        if (xstreamObjClass != null) {
            byte[] bytes = get(uri, 200, "application/xml", false);
            XStream xStream = new XStream();
            xStream.processAnnotations(xstreamObjClass);
            return (T) xStream.fromXML(new ByteArrayInputStream(bytes));
        } else {
            get(uri, 200, null, true);
            return null;
        }
    }

    protected static byte[] get(String uri, int expectedStatus,
            String expectedMediaType, boolean printStream) throws Exception {
        GetMethod method;
        try {
            method = new GetMethod(uri);
        } catch (IllegalStateException ise) {
            throw new RemoteCommandException("\nAn error has occurred while trying to resolve the given url: " +
                    uri + "\n" + ise.getMessage());
        }

        executeMethod(uri, method, expectedStatus, expectedMediaType);

        return analyzeResponse(method, printStream);
    }

    private static void checkContentType(String uri, String expectedMediaType, String contentType) {
        if (!PathUtils.hasText(expectedMediaType)) {
            return;
        }
        if (!contentType.contains(expectedMediaType)) {
            throw new RuntimeException("HTTP content type was " + contentType + " and should be " +
                    expectedMediaType + " for request on " + uri);
        }
    }

    private static void checkStatus(String uri, int expectedStatus, int status) {
        if (status != expectedStatus) {
            throw new RemoteCommandException("\nUnexpected response status for request: " + uri + "\n" +
                    "Expected status: " + expectedStatus + " (" + HttpStatus.getStatusText(expectedStatus) + ")" +
                    "\n" +
                    "Received status: " + status + " (" + HttpStatus.getStatusText(status) + ")" + "\n");
        }
    }

    protected static byte[] post(String uri, final byte[] data,
            final String inputDataType, int expectedStatus,
            String expectedMediaType, boolean printStream) throws Exception {
        PostMethod method = new PostMethod(uri);
        method.setRequestEntity(new RequestEntity() {
            public boolean isRepeatable() {
                return true;
            }

            public void writeRequest(OutputStream out) throws IOException {
                out.write(data);
            }

            public long getContentLength() {
                return data.length;
            }

            public String getContentType() {
                return inputDataType;
            }
        });
        executeMethod(uri, method, expectedStatus, expectedMediaType);

        return analyzeResponse(method, printStream);
    }

    private static byte[] analyzeResponse(HttpMethod method, boolean printStream)
            throws IOException {
        InputStream is = method.getResponseBodyAsStream();
        byte[] buffer = new byte[1024];
        int r;
        try {
            if (printStream) {
                while ((r = is.read(buffer)) != -1) {
                    System.out.print(new String(buffer, 0, r, "utf-8"));
                }
                System.out.println("");
                return null;
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while ((r = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, r);
                }

                return baos.toByteArray();
            }
        } catch (SocketTimeoutException e) {
            log.error("Communication with the server has timed out: " + e.getMessage());
            log.error("ATTENTION: The command on the server may still be running!");
            String url = method.getURI().toString();
            int apiPos = url.indexOf("/api");
            String logsUrl;
            if (apiPos != -1) {
                logsUrl = url.substring(0, apiPos) + "/webapp/systemlogs.html";
            } else {
                logsUrl = "http://" + method.getURI().getHost() + "/artifactory/webapp/systemlogs.html";
            }
            log.error("Please check the server logs " + logsUrl + " before re-running the command.");
            return null;
        }
    }

    private static void executeMethod(String uri, HttpMethod method, int expectedStatus,
            String expectedMediaType) throws Exception {
        int status;
        try {
            status = getHttpClient(uri).executeMethod(method);
        } catch (SSLException ssle) {
            throw new RemoteCommandException("\nThe host you are trying to reach does not support SSL.");
        } catch (ConnectTimeoutException cte) {
            throw new RemoteCommandException("\n" + cte.getMessage());
        } catch (UnknownHostException uhe) {
            throw new RemoteCommandException("\nThe host of the specified URL: " + uri + " could not be found.\n" +
                    "Please make sure you have specified the correct path. The default should be:\n" +
                    "http://myhost:8081/artifactory/api/system");
        } catch (ConnectException ce) {
            throw new RemoteCommandException("\nThe specified host paramater is invalid. " +
                    "The format should be - host:port.\n" + "For example: localhost:8081");
        } catch (NoRouteToHostException nrthe) {
            throw new RemoteCommandException("\nThe host could not be reached.\n" +
                    "Please make sure that the address is valid and that the port is open (firewall, router, etc').");
        }

        checkStatus(uri, expectedStatus, status);
        Header mediaType = method.getResponseHeader("content-type");
        String contentType = mediaType.getValue();
        checkContentType(uri, expectedMediaType, contentType);
    }

    protected static HttpClient getHttpClient(String url) throws Exception {
        HttpConnectionManager connectionManager = new SimpleHttpConnectionManager();
        HttpConnectionManagerParams connectionManagerParams = connectionManager.getParams();
        //Set the socket connection timeout
        connectionManagerParams.setConnectionTimeout(3000);
        HttpClient client = new HttpClient(connectionManager);
        HttpClientParams clientParams = client.getParams();
        //Set the socket data timeout
        int to = 900000;
        if (CliOption.timeout.isSet()) {
            String timeout = CliOption.timeout.getValue();
            try {
                to = Integer.parseInt(timeout) * 1000;
            } catch (NumberFormatException nfe) {
                throw new RemoteCommandException(
                        "\nThe timeout length you have entered: " + timeout + " - is invalid.\n" +
                                "Please enter a positive integer for a timeout value in seconds.");
            }
        }
        clientParams.setSoTimeout(to);
        if (CliOption.username.isSet()) {
            String host;
            try {
                host = new URL(url).getHost();
            } catch (MalformedURLException mue) {
                throw new RemoteCommandException("\nAn error has occurred while trying to resolve the given url: " +
                        url + "\n" + mue.getMessage());
            }
            clientParams.setAuthenticationPreemptive(true);
            Credentials creds = new UsernamePasswordCredentials(
                    CliOption.username.getValue(),
                    CliOption.password.getValue());
            AuthScope scope = new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
            client.getState().setCredentials(scope, creds);
        }
        return client;
    }
}