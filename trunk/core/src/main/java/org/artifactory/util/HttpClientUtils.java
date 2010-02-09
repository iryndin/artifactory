/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Yoav Landman
 */
public class HttpClientUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpClientUtils.class);

    public static void configureUserAgent(HttpClient client) {
        String userAgent = HttpUtils.getArtifactoryUserAgent();
        HttpClientParams clientParams = client.getParams();
        clientParams.setParameter(HttpMethodParams.USER_AGENT, userAgent);
    }

    public static void configureProxy(HttpClient client, ProxyDescriptor proxy) {
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
                } else {
                    try {
                        //TODO: [by yl] Might want to use localAddress if specified, instead of getLocalHost()
                        creds = new NTCredentials(proxy.getUsername(),
                                proxy.getPassword(), InetAddress.getLocalHost().getHostName(),
                                proxy.getDomain());
                    } catch (UnknownHostException e) {
                        log.error("Failed to determine required local hostname for NTLM credentials.", e);
                    }
                }
                if (creds != null) {
                    client.getState().setProxyCredentials(AuthScope.ANY, creds);
                    client.getParams().setAuthenticationPreemptive(true);
                }
            }
        }
    }
}