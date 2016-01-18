package org.artifactory.security.props.auth.model;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Chen Keinan
 */
public class OauthErrorModel implements OauthModel {

    private int statusCode;
    private OauthErrorEnum internalErrorMsg;
    private String message;
    @JsonProperty("documentation_url")
    private String documentationUrl;

    public OauthErrorModel() {
    }

    public OauthErrorModel(int statusCode, OauthErrorEnum internalErrorMsg) {
        this.statusCode = statusCode;
        this.internalErrorMsg = internalErrorMsg;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public OauthErrorEnum getInternalErrorMsg() {
        return internalErrorMsg;
    }

    public void setInternalErrorMsg(OauthErrorEnum internalErrorMsg) {
        this.internalErrorMsg = internalErrorMsg;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }
}
