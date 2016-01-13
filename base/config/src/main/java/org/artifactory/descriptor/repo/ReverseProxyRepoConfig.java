package org.artifactory.descriptor.repo;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Shay Yaakov
 */
@XmlType(name = "ReverseProxyRepoConfigType", propOrder = {"repoRef", "serverName", "port"},
        namespace = Descriptor.NS)
public class ReverseProxyRepoConfig implements Descriptor {

    @XmlIDREF
    @XmlElement(name = "repoRef",required = false)
    private RepoBaseDescriptor repoRef;

    @XmlElement(name = "serverName",required = false)
    private String serverName;
    private int port = -1;

    public RepoBaseDescriptor getRepoRef() {
        return repoRef;
    }

    public void setRepoRef(RepoBaseDescriptor repoRef) {
        this.repoRef = repoRef;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPort(Integer port) {
        if (port != null) {
            this.port = port;
        } else {
            this.port = -1;
        }
    }
}
