package org.artifactory.util.bearer;

import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.protocol.HttpContext;

/**
 * {@link AuthSchemeProvider} implementation that creates and initializes {@link BearerScheme} instances
 *
 * @author Shay Yaakov
 */
public class BearerSchemeFactory implements AuthSchemeProvider {

    private String repoKey;

    public BearerSchemeFactory(String repoKey) {
        this.repoKey = repoKey;
    }

    @Override
    public AuthScheme create(HttpContext context) {
        return new BearerScheme(repoKey);
    }
}
