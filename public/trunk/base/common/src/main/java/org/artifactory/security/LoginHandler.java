package org.artifactory.security;

import org.artifactory.security.props.auth.model.OauthModel;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.BadCredentialsException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;

/**
 * @author Chen  Keinan
 */
public interface LoginHandler {


    /**
     * do basic authentication
     *
     * @param tokens - tokens
     * @return
     * @throws IOException
     */
    OauthModel doBasicAuthWithDb(String[] tokens,
            AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) throws IOException, ParseException;


    /**
     * do basic authentication with db
     *
     * @param header   - authorization header
     * @param username - username
     * @return
     */
    OauthModel doBasicAuthWithProvider(String header, String username);

    /**
     * do user property authentication with db
     *
     * @param username - user name
     */
    OauthModel doPropsAuthWithDb(String username) throws ParseException;

    /**
     * Decodes the header into a username and password.
     *
     * @throws BadCredentialsException if the Basic header is not present or is not valid Base64
     */
    String[] extractAndDecodeHeader(String header) throws IOException;
}
