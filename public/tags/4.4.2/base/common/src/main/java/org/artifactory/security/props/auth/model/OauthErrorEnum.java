package org.artifactory.security.props.auth.model;


/**
 * @author Chenk Keinan
 */
public enum OauthErrorEnum {

    USER_NOT_FOUND("user not found"),
    INTERNAL_SERVER_ERROR("internal server error"),
    BAD_REQUEST("bad request"),
    UNAUTHORIZED("unauthorized request"),
    BAD_CREDENTIAL("Bad Credential");


    private String value;

    private OauthErrorEnum(String value) {
        this.value = value;
    }
}
