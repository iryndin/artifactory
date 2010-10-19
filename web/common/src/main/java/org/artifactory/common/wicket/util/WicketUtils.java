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

package org.artifactory.common.wicket.util;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.request.target.component.BookmarkablePageRequestTarget;
import org.apache.wicket.util.io.Streams;
import org.apache.wicket.util.lang.Packages;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.TextContentPanel;
import org.artifactory.common.wicket.component.label.highlighter.Syntax;
import org.artifactory.common.wicket.component.label.highlighter.SyntaxHighlighter;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yoavl
 */
public abstract class WicketUtils {
    private WicketUtils() {
        // utility class
    }

    /**
     * Get the bookmarkable path of a page that has been mounted
     *
     * @param pageClass Mounted page
     * @return CharSequence - Bookmarkable path
     */
    public static CharSequence mountPathForPage(Class pageClass) {
        return mountPathForPage(pageClass, null);
    }

    /**
     * Get the bookmarkable path of a page that has been mounted
     *
     * @param pageClass      Mounted page
     * @param pageParameters Optional page parameters
     * @return CharSequence - Bookmarkable path
     */
    public static CharSequence mountPathForPage(Class pageClass, PageParameters pageParameters) {
        return getWebApplication().getRequestCycleProcessor().getRequestCodingStrategy()
                .pathForTarget(new BookmarkablePageRequestTarget(pageClass, pageParameters));
    }

    /**
     * Returns the web application object
     *
     * @return WebApplication - application object
     */
    public static WebApplication getWebApplication() {
        return (WebApplication) RequestCycle.get().getApplication();
    }

    public static WebRequest getWebRequest() {
        WebRequestCycle webRequestCycle = (WebRequestCycle) RequestCycle.get();
        if (webRequestCycle == null) {
            return null;
        }
        return webRequestCycle.getWebRequest();
    }

    public static WebResponse getWebResponse() {
        WebRequestCycle webRequestCycle = (WebRequestCycle) RequestCycle.get();
        return webRequestCycle.getWebResponse();
    }

    public static Map<String, String> getHeadersMap() {
        Map<String, String> map = new HashMap<String, String>();
        HttpServletRequest request = getWebRequest().getHttpServletRequest();
        if (request != null) {
            Enumeration headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = (String) headerNames.nextElement();
                map.put(headerName.toUpperCase(), request.getHeader(headerName));
            }
        }
        return map;
    }

    public static Page getPage() {
        return RequestCycle.get().getResponsePage();
    }

    public static String getWicketAppPath() {
        return RequestCycle.get().getRequest().getRelativePathPrefixToWicketHandler();
    }

    public static String readResource(Class scope, String file) {
        try {
            final String path = Packages.absolutePath(scope, file);
            final IResourceStream resourceStream = Application.get()
                    .getResourceSettings()
                    .getResourceStreamLocator()
                    .locate(scope, path);
            InputStream inputStream = resourceStream.getInputStream();
            return Streams.readString(inputStream, "utf-8");
        } catch (ResourceStreamNotFoundException e) {
            throw new RuntimeException(String.format("Can't find resource \"%s.%s\"", scope.getName(), file), e);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Can't read resource \"%s.%s\"", scope.getName(), file), e);
        }
    }

    /**
     * Returns a syntax highlighter. If the size of the string exceeds the size limit defined in the system properties,
     * than a simple text content panel will be returned
     *
     * @param componentId ID to assign to the returned component
     * @param toDisplay   String to display
     * @param syntaxType  Type of syntax to use
     * @return Text displaying component
     */
    public static Component getSyntaxHighlighter(String componentId, String toDisplay, Syntax syntaxType) {
        if (ConstantValues.uiSyntaxColoringMaxTextSizeBytes.getLong() >= toDisplay.getBytes().length) {
            return new SyntaxHighlighter(componentId, toDisplay, syntaxType);
        } else {
            TextContentPanel contentPanel = new TextContentPanel(componentId);
            contentPanel.add(new CssClass("lines"));
            return contentPanel.setContent(toDisplay);
        }
    }
}