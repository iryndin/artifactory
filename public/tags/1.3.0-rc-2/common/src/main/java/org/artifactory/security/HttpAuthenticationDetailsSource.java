package org.artifactory.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.ui.WebAuthenticationDetailsSource;

/**
 * Support using the HttpAuthenticationDetails class
 *
 * @author yoavl
 */
public class HttpAuthenticationDetailsSource extends WebAuthenticationDetailsSource {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger log = LoggerFactory.getLogger(HttpAuthenticationDetailsSource.class);

    public HttpAuthenticationDetailsSource() {
        super();
        setClazz(HttpAuthenticationDetails.class);
    }
}