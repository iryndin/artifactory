package org.artifactory.addon.oauth;

/**
 * @author Gidi Shabat
 */
public class OAuthLoginUrl {
    private String name;
    private String url;
    private OAuthProvidersTypeEnum type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public OAuthProvidersTypeEnum getType() {
        return type;
    }

    public void setType(OAuthProvidersTypeEnum type) {
        this.type = type;
    }
}
