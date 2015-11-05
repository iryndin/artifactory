package org.artifactory.descriptor.security.oauth;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * @author Gidi Shabat
 */
@XmlType(name = "oauthSettingsType",
        propOrder = {"enableIntegration", "persistUsers","defaultNpm","oauthProvidersSettings"},
        namespace = Descriptor.NS)
public class OAuthSettings implements Descriptor {

    private Boolean enableIntegration = false;
    private Boolean persistUsers = false;
    private String defaultNpm;

    @XmlElementWrapper(name = "oauthProvidersSettings")
    private List<OAuthProviderSettings> oauthProvidersSettings = Lists.newArrayList();

    public Boolean getEnableIntegration() {
        return enableIntegration;
    }

    public void setEnableIntegration(Boolean enableIntegration) {
        this.enableIntegration = enableIntegration;
    }


    public List<OAuthProviderSettings> getOauthProvidersSettings() {
        return oauthProvidersSettings;
    }

    public void setOauthProvidersSettings(List<OAuthProviderSettings> oauthProvidersSettings) {
        this.oauthProvidersSettings = oauthProvidersSettings;
    }

    public String getDefaultNpm() {
        return defaultNpm;
    }

    public void setDefaultNpm(String defaultNpm) {
        this.defaultNpm = defaultNpm;
    }

    public Boolean getPersistUsers() {
        return persistUsers;
    }

    public void setPersistUsers(Boolean persistUsers) {
        this.persistUsers = persistUsers;
    }
}
