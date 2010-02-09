package org.artifactory.repo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlType(name = "ProxyType", propOrder = {"key", "host", "port", "username", "password", "domain"})
public class Proxy implements Serializable {

    private String key;
    private String host;
    private int port;
    private String username;
    private String password;
    private String domain;

    @XmlElement(required = true)
    public String getHost() {
        return host;
    }

    @XmlID
    @XmlElement(required = true)
    public String getKey() {
        return key;
    }

    public String getPassword() {
        return password;
    }

    @XmlElement(required = true)
    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getDomain() {
        return domain;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
