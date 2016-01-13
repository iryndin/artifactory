<#if addHa && addOnce>
## add HA entries when ha is configured
<Proxy balancer://${upstreamName}>
    <#list hsservers as haserver>
    ${haserver}
    </#list>
ProxySet lbmethod=byrequests
ProxySet stickysession=ROUTEID
</Proxy>
</#if>

<#if !generalOnly>
Listen ${repoPort}
</#if>
<VirtualHost *:<#if httpOnly && generalOnly>${httpPort}</#if><#if !generalOnly>${repoPort}</#if><#if httpsOnly && generalOnly>${sslPort}</#if>>
    ServerName ${serverName}
    ServerAlias *.${serverName}
    ServerAdmin server@admin

<#if generalOnly && useHttps && !httpOnly>
    SSLEngine on
    SSLCertificateFile ${sslCrtPath}
    SSLCertificateKeyFile ${sslKeyPath}
    SSLProxyEngine on
</#if>

    ## Application specific logs
    ## ErrorLog ${APACHE_LOG_DIR}/${serverName}-error.log
    ## CustomLog ${APACHE_LOG_DIR}/${serverName}-access.log combined

    RewriteEngine on

    RewriteCond %{SERVER_PORT} (.*)
    RewriteRule (.*) - [E=my_server_port:%1]

    RewriteCond %{REQUEST_SCHEME} (.*)
    RewriteRule (.*) - [E=my_scheme:%1]

    RewriteCond %{HTTP_HOST} (.*)
    RewriteRule (.*) - [E=my_custom_host:%1]

<#if subdomain>
    RewriteCond "%{REQUEST_URI}" "^/(v1|v2)/"
    RewriteCond "%{HTTP_HOST}" ${quotes}^(.*)\.${serverName}$${quotes}
    RewriteRule "^/(v1|v2)/(.*)$" ${quotes}${webPublicContext}api/docker/%1/$1/$2${quotes} [P]
</#if>

<#if (!subdomain && !generalOnly) || isSamePort>
    RewriteRule "^/(v1|v2)/(.*)$" ${quotes}${webPublicContext}api/docker/${repoKey}/$1/$2${quotes} [P]
</#if>

    RewriteRule ^/$                ${webPublicContext}webapp/ [R,L]
    RewriteRule ^/${publicContext}(/)?$      ${webPublicContext}webapp/ [R,L]
    RewriteRule ^/${publicContext}/webapp$   ${webPublicContext}webapp/ [R,L]

    RequestHeader set Host %{my_custom_host}e
    RequestHeader set X-Forwarded-Port %{my_server_port}e
    RequestHeader set X-Forwarded-Proto %{my_scheme}e
    RequestHeader set X-Artifactory-Override-Base-Url %{my_scheme}e://${serverName}${publicContextWithSlash}
    ProxyPassReverseCookiePath /${absoluteAppContext} /${publicContext}

<#if addGeneral>
    ProxyRequests off
    ProxyPreserveHost on
</#if>
<#if !addHa>
    ProxyPass ${webPublicContext} http://${localNameAndPort}/${appContext}
    ProxyPassReverse ${webPublicContext} http://${localNameAndPort}/${appContext}
</#if>
<#if addHa>
    ProxyPass ${webPublicContext} balancer://${upstreamName}/${appContext}
    ProxyPassReverse ${webPublicContext} balancer://${upstreamName}/${appContext}
    Header add Set-Cookie ${quotes}ROUTEID=.%{BALANCER_WORKER_ROUTE}e; path=/${publicContext}/${quotes} env=BALANCER_ROUTE_CHANGED
</#if>
</VirtualHost>
