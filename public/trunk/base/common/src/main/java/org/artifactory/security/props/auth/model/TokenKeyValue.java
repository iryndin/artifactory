package org.artifactory.security.props.auth.model;

/**
 * @author Chen Keinan
 */
public class TokenKeyValue {

    private String token;

    private String key;

    public TokenKeyValue(String token) {
        this.token = token;
    }

    public TokenKeyValue(String key, String token) {
        this.key = key;
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
