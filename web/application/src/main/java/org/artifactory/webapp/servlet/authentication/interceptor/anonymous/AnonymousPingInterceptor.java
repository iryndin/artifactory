package org.artifactory.webapp.servlet.authentication.interceptor.anonymous;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.rest.constant.SystemRestConstants;
import org.artifactory.common.ConstantValues;
import org.artifactory.webapp.servlet.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Allows anonymous to ping this instance even if anonymous access is disabled  when the 'ping.allowUnauthenticated'
 * flag is set (RTFACT-8239).
 *
 * @author Dan Feldman
 */
public class AnonymousPingInterceptor implements AnonymousAuthenticationInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AnonymousPingInterceptor.class);

    @Override
    public boolean accept(HttpServletRequest request) {
        return unauthenticatedPingAllowed(request);
    }

    private boolean unauthenticatedPingAllowed(HttpServletRequest request) {
        try {
            return request.getMethod().equals("GET")
                    && StringUtils.startsWith(RequestUtils.getServletPathFromRequest(request),
                        "/api/" + SystemRestConstants.PATH_ROOT + "/" + SystemRestConstants.PATH_PING)
                    && (!RequestUtils.isAuthHeaderPresent(request)
                        || RequestUtils.extractUsernameFromRequest(request).equalsIgnoreCase("anonymous"))
                    && ConstantValues.allowUnauthenticatedPing.getBoolean();
        } catch (Exception e) {
            log.debug("Caught exception: ", e);
            return false;
        }
    }
}
