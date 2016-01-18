package org.artifactory.repo.reverseProxy;

/**
 * @author Chen Keinan
 */
public class ReverseProxyPorts {

    private boolean http;
    private boolean https;
    private boolean bothPorts;

    public ReverseProxyPorts() {
    }

    public ReverseProxyPorts(boolean http, boolean https, boolean bothPorts) {
        this.http = http;
        this.https = https;
        this.bothPorts = bothPorts;
    }

    public boolean isHttp() {
        return http;
    }

    public void setHttp(boolean http) {
        this.http = http;
    }

    public boolean isHttps() {
        return https;
    }

    public void setHttps(boolean https) {
        this.https = https;
    }

    public boolean isBothPorts() {
        return bothPorts;
    }

    public void setBothPorts(boolean bothPorts) {
        this.bothPorts = bothPorts;
    }
}
