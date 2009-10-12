package org.artifactory.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.ui.WebAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;
import java.util.StringTokenizer;

/**
 * Support getting the client's ip address in reverse-proxied environments
 *
 * @author yoavl
 */
public class HttpAuthenticationDetails extends WebAuthenticationDetails {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger log = LoggerFactory.getLogger(HttpAuthenticationDetails.class);

    private String forwardedRemoteAddress;

    /**
     * Records the remote address and will also set the session Id if a session already exists (it won't create one).
     *
     * @param request that the authentication request was received from
     */
    public HttpAuthenticationDetails(HttpServletRequest request) {
        super(request);
        //Check if there is a remote address coming from a prxied request
        //(http://httpd.apache.org/docs/2.2/mod/mod_proxy.html#proxypreservehost)
        String header = request.getHeader("X-Forwarded-For");
        if (header != null) {
            //Might contain multiple entries - take the first
            forwardedRemoteAddress = new StringTokenizer(header, ",").nextToken();
        }


    }

    @Override
    public String getRemoteAddress() {
        return forwardedRemoteAddress != null ? forwardedRemoteAddress : super.getRemoteAddress();
    }
}
