/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.util;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.httpclient.auth.AuthScope.*;

/**
 * Common HTTP client configuration helper
 *
 * @author Noam Y. Tenne
 */
public class HttpClientConfigurator {
    private static final Logger log = LoggerFactory.getLogger(HttpClientConfigurator.class);

    private HttpConnectionManager connectionManager;
    private HttpClient httpClient;
    private String host;

    public HttpClientConfigurator() {
        this(false);
    }

    public HttpClientConfigurator(boolean multiThreaded) {
        if (multiThreaded) {
            connectionManager = new MultiThreadedHttpConnectionManager();
        } else {
            connectionManager = new SimpleHttpConnectionManager();
        }
        httpClient = new HttpClient(connectionManager);

        configureUserAgent(httpClient);
    }

    /**
     * May throw a runtime exception when the given URL is invalid.
     */
    public HttpClientConfigurator hostFromUrl(String urlStr) {
        if (StringUtils.isNotBlank(urlStr)) {
            try {
                URL url = new URL(urlStr);
                host = url.getHost();
                httpClient.getHostConfiguration().setHost(host/*, url.getPort()*/);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Cannot parse the url " + urlStr, e);
            }
        }
        return this;
    }

    /**
     * Ignores blank values
     */
    public HttpClientConfigurator host(String host) {
        if (StringUtils.isNotBlank(host)) {
            this.host = host;
            httpClient.getHostConfiguration().setHost(host);
        }
        return this;
    }

    public HttpClientConfigurator defaultMaxConnectionsPerHost(int defaultMaxConnectionsPerHost) {
        connectionManager.getParams().setDefaultMaxConnectionsPerHost(defaultMaxConnectionsPerHost);
        return this;
    }

    public HttpClientConfigurator maxTotalConnections(int maxTotalConnections) {
        connectionManager.getParams().setMaxTotalConnections(maxTotalConnections);
        return this;
    }

    public HttpClientConfigurator connectionTimeout(int connectionTimeout) {
        connectionManager.getParams().setConnectionTimeout(connectionTimeout);
        httpClient.getParams().setConnectionManagerTimeout(connectionTimeout);
        return this;
    }

    public HttpClientConfigurator soTimeout(int soTimeout) {
        connectionManager.getParams().setSoTimeout(soTimeout);
        httpClient.getParams().setSoTimeout(soTimeout);
        return this;
    }

    /**
     * see {@link org.apache.commons.httpclient.params.HttpConnectionParams#setStaleCheckingEnabled(boolean)}
     */
    public HttpClientConfigurator staleCheckingEnabled(boolean staleCheckingEnabled) {
        connectionManager.getParams().setStaleCheckingEnabled(staleCheckingEnabled);
        return this;
    }

    /**
     * See {@link org.apache.commons.httpclient.DefaultHttpMethodRetryHandler}
     */
    public HttpClientConfigurator retry(int retryCount, boolean requestSentRetryEnabled) {
        httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(retryCount, requestSentRetryEnabled));
        return this;
    }

    /**
     * Ignores blank or invalid input
     */
    public HttpClientConfigurator localAddress(String localAddress) {
        if (StringUtils.isNotBlank(localAddress)) {
            InetAddress address;
            try {
                address = InetAddress.getByName(localAddress);
                httpClient.getHostConfiguration().setLocalAddress(address);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Invalid local address: " + localAddress, e);
            }
        }
        return this;
    }

    /**
     * Ignores null descriptor
     */
    public HttpClientConfigurator proxy(ProxyDescriptor proxyDescriptor) {
        configureProxy(httpClient, proxyDescriptor);
        return this;
    }

    /**
     * Ignores null credentials
     */
    public HttpClientConfigurator authentication(UsernamePasswordCredentials creds) {
        if (creds != null) {
            authentication(creds.getUserName(), creds.getPassword());
        }

        return this;
    }

    /**
     * Ignores blank username input
     */
    public HttpClientConfigurator authentication(String username, String password) {
        if (StringUtils.isNotBlank(username)) {
            if (StringUtils.isBlank(host)) {
                throw new IllegalStateException("Cannot configure authentication when no host is set.");
            }
            httpClient.getParams().setAuthenticationPreemptive(true);
            Credentials creds = new UsernamePasswordCredentials(username, password);
            AuthScope scope = new AuthScope(host, ANY_PORT, ANY_REALM);
            httpClient.getState().setCredentials(scope, creds);
        }
        return this;
    }

    public HttpClient getClient() {
        return httpClient;
    }


    private void configureUserAgent(HttpClient client) {
        String userAgent = HttpUtils.getArtifactoryUserAgent();
        HttpClientParams clientParams = client.getParams();
        clientParams.setParameter(HttpMethodParams.USER_AGENT, userAgent);
    }

    private void configureProxy(HttpClient client, ProxyDescriptor proxy) {
        if (proxy != null) {
            HostConfiguration hostConf = client.getHostConfiguration();
            hostConf.setProxy(proxy.getHost(), proxy.getPort());
            if (proxy.getUsername() != null) {
                Credentials creds = null;
                if (proxy.getDomain() == null) {
                    creds = new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword());
                    //This will demote the NTLM authentication scheme so that the proxy won't barf
                    //when we try to give it traditional credentials. If the proxy doesn't do NTLM
                    //then this won't hurt it (jcej at tragus dot org)
                    List<String> authPrefs = Arrays.asList(AuthPolicy.DIGEST, AuthPolicy.BASIC, AuthPolicy.NTLM);
                    client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
                    client.getParams().setAuthenticationPreemptive(true);
                } else {
                    try {
                        String ntHost =
                                StringUtils.isBlank(proxy.getNtHost()) ? InetAddress.getLocalHost().getHostName() :
                                        proxy.getNtHost();
                        creds = new NTCredentials(proxy.getUsername(), proxy.getPassword(), ntHost, proxy.getDomain());
                    } catch (UnknownHostException e) {
                        log.error("Failed to determine required local hostname for NTLM credentials.", e);
                    }
                    client.getParams().setAuthenticationPreemptive(false);
                }
                if (creds != null) {
                    client.getState().setProxyCredentials(ANY, creds);
                    if (proxy.getRedirectedToHostsList() != null) {
                        for (String hostName : proxy.getRedirectedToHostsList()) {
                            client.getState().setCredentials(new AuthScope(hostName, ANY_PORT, ANY_REALM), creds);
                        }
                    }
                }
            }
        }
    }
}
