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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                if (proxy.getDomain() == null) {
                    Credentials creds = new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword());
                    //This will exclude the NTLM authentication scheme so that the proxy won't barf
                    //when we try to give it traditional credentials. If the proxy doesn't do NTLM
                    //then this won't hurt it (jcej at tragus dot org)
                    List<String> authPrefs = Arrays.asList(AuthPolicy.DIGEST, AuthPolicy.BASIC);
                    client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
                    client.getState().setProxyCredentials(AuthScope.ANY, creds);
                } else {
                    try {
                        Credentials creds = new NTCredentials(proxy.getUsername(),
                                proxy.getPassword(), InetAddress.getLocalHost().getHostName(),
                                proxy.getDomain());
                        client.getState().setProxyCredentials(AuthScope.ANY, creds);
                    } catch (UnknownHostException e) {
                        log.error("Failed to determine required local hostname for NTLM credentials.", e);
                    }
                }
            }
        }
    }

}