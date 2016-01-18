package org.artifactory.security.props.auth.model;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Chenk Keinan
 */
public class AuthenticationModel implements OauthModel {

    private String token;
    @JsonProperty("expires_in")
    private String expiresIn;
    @JsonProperty("issued_at")
    private String issuedAt;

    public AuthenticationModel() {
    }

    public AuthenticationModel(String token, String issuedAt, String expiresIn) {
        this.issuedAt = issuedAt;
        this.expiresIn = expiresIn;
        this.token = token;
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(String expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }
}
