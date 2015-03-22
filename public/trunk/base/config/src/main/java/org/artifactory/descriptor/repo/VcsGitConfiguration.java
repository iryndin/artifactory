package org.artifactory.descriptor.repo;

import org.apache.commons.lang.StringUtils;
import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.text.MessageFormat;

/**
 * @author Yoav Luft
 */
@XmlType(name = "VcsGitType", propOrder = {"provider", "downloadUrl"})
public class VcsGitConfiguration implements Descriptor {

    @XmlElement(name = "provider")
    private VcsGitProvider provider = VcsGitProvider.GITHUB;

    @XmlElement(name = "downloadUrl")
    private String downloadUrl;

    public VcsGitConfiguration() {
    }

    public VcsGitProvider getProvider() {
        return provider;
    }

    public void setProvider(VcsGitProvider provider) {
        this.provider = provider;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String buildDownloadUrl(String gitOrg, String gitRepo, String version, String fileExt) {
        String[] values = new String[] {gitOrg, gitRepo, version, fileExt};
        String url = StringUtils.isNotBlank(downloadUrl) ? downloadUrl : provider.getDownloadUrl();
        return MessageFormat.format(url, values);
    }
}
