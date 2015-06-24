package org.artifactory.util.bearer;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.auth.RFC2617Scheme;
import org.apache.http.message.BufferedHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.CharArrayBuffer;
import org.artifactory.api.context.ContextHelper;

/**
 * Bearer authentication scheme as defined in RFC 2617
 *
 * @author Shay Yaakov
 */
public class BearerScheme extends RFC2617Scheme {
    @Override
    public String getSchemeName() {
        return "bearer";
    }

    @Override
    public boolean isConnectionBased() {
        return false;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public Header authenticate(Credentials credentials, HttpRequest request) throws AuthenticationException {
        return authenticate(credentials, request, new BasicHttpContext());
    }

    @Override
    public Header authenticate(Credentials credentials, HttpRequest request, HttpContext context)
            throws AuthenticationException {
        String token = ContextHelper.get().beanForType(TokenProvider.class).getToken(getParameters());
        final CharArrayBuffer buffer = new CharArrayBuffer(32);
        buffer.append(AUTH.WWW_AUTH_RESP);
        buffer.append(": Bearer ");
        buffer.append(token);
        return new BufferedHeader(buffer);
    }
}
