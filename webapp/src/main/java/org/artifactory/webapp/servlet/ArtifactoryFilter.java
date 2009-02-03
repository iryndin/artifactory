/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.servlet;

import org.apache.log4j.Logger;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.request.HttpArtifactoryRequest;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ArtifactoryContextThreadBinder;
import org.artifactory.spring.ContextHelper;
import org.springframework.web.context.WebApplicationContext;

import javax.jcr.RepositoryException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ArtifactoryFilter implements Filter {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryFilter.class);

    public static final String WEBAPP_URL_PATH_PREFIX = "webapp";

    public static final Set<String> WEBAPP_URL_PATH_PREFIXES = new HashSet<String>() {
        {
            add(WEBAPP_URL_PATH_PREFIX);
            add("skins");
            add("images");
            add("js");
            add("css");
        }
    };

    private ArtifactoryContext context;

    public static String getContextPrefix(HttpServletRequest request) {
        String contextPrefix;
        String requestUri = request.getRequestURI();
        int contextPrefixEndIdx = requestUri.indexOf('/', 1);
        if (contextPrefixEndIdx > 0) {
            contextPrefix = requestUri.substring(1, contextPrefixEndIdx);
        } else {
            contextPrefix = "";
        }
        return contextPrefix;
    }

    protected static boolean isRepoRequest(String servletPath) {
        if ("/favicon.ico".equals(servletPath)) {
            return false;
        }
        String pathPrefix = HttpArtifactoryRequest.getPathPrefix(servletPath);
        return pathPrefix != null && pathPrefix.length() > 0 &&
                !WEBAPP_URL_PATH_PREFIXES.contains(pathPrefix);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        this.context =
                (ArtifactoryContext) servletContext
                        .getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    }

    public void destroy() {
    }

    protected ArtifactoryContext getContext() {
        return context;
    }

    public final void doFilter(final ServletRequest req, final ServletResponse resp,
            final FilterChain chain)
            throws IOException, ServletException {
        ArtifactoryContextThreadBinder.bind(context);
        //Explicitly bind a jcr session to the thread
        JcrWrapper jcr = ContextHelper.get().getJcr();
        jcr.setCreateSessionIfNeeded(true);
        Throwable t = jcr.doInSession(new JcrCallback<Throwable>() {
            @SuppressWarnings({"UnnecessaryLocalVariable"})
            public Throwable doInJcr(JcrSessionWrapper session)
                    throws RepositoryException {
                try {
                    doFilterInternal(req, resp, chain);
                } catch (Throwable t) {
                    return t;
                }
                return null;
            }
        });
        ArtifactoryContextThreadBinder.unbind();
        if (t != null) {
            if (t instanceof IOException) {
                throw (IOException) t;
            } else if (t instanceof ServletException) {
                throw (ServletException) t;
            } else {
                throw new ServletException(t);
            }
        }
    }

    protected abstract void doFilterInternal(
            ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException;
}
