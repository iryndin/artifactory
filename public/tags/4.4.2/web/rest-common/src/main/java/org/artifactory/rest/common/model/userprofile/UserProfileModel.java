package org.artifactory.rest.common.model.userprofile;

import org.artifactory.rest.common.model.BaseModel;

/**
 * @author Chen Keinan
 */
public class UserProfileModel extends BaseModel {

    private String userName;
    private String apiKey;

    public UserProfileModel() {
        // for jackson
    }

    public UserProfileModel(String apiKey) {
        this.apiKey = apiKey;
    }

    public UserProfileModel(String apiKey, String userName) {
        this.apiKey = apiKey;
        this.userName = userName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
