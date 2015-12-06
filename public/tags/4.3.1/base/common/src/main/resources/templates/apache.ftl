<#if addGeneral>
    ProxyRequests off
    ProxyPreserveHost on
    SetEnv            proxy-sendcl 1
</#if>
<#if addSsl>
    SSLEngine on
    SSLCertificateFile      ${sslCrtPath}
    SSLCertificateKeyFile   ${sslKeyPath}
    SSLProxyEngine on
</#if>

<VirtualHost *:<#if !generalOnly>${repoPort}</#if><#if generalOnly>${generalPort}</#if>>
    RewriteEngine On
    <#if generalOnly || subdomain>
        RewriteRule ^/$              ${appContext}
        RewriteRule ${publicContext}*$  ${appContext}/webapp/ [R,L]
    </#if>
    ProxyPass ${publicContext} http://${localHost}:${localPort}${appContext}
<#if !generalOnly || subdomain>
    <#if subdomain && isDocker>
        <#if v1>
            ProxyPass         /v1 http://${localHost}:${localPort}${appContext}/api/docker/${repoKey}/v1
            ProxyPassReverse  /v1 http://${localHost}:${localPort}${appContext}/api/docker/${repoKey}/v1
        </#if>
        <#if !v1>
            ProxyPass         /v2 http://${localHost}:${localPort}${appContext}/api/docker/${repoKey}/v2
            ProxyPassReverse  /v2 http://${localHost}:${localPort}${appContext}/api/docker/${repoKey}/v2
        </#if>
    </#if>
    <#if !subdomain && isDocker>
         <#if v1>
            ProxyPass         /v1 http://${localHost}:${localPort}${appContext}/api/docker/${repoKey}/v1
            ProxyPassReverse  /v1 http://${localHost}:${localPort}${appContext}/api/docker/${repoKey}/v1
        </#if>
        <#if !v1>
            ProxyPass         /v2 http://${localHost}:${localPort}${appContext}/api/docker/${repoKey}/v2
            ProxyPassReverse  /v2 http://${localHost}:${localPort}${appContext}/api/docker/${repoKey}/v2
        </#if>
    </#if>
</#if>
</VirtualHost>