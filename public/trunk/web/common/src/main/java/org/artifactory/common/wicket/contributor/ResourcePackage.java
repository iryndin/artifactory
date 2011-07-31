/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.common.wicket.contributor;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.PackageResource;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.util.io.Streams;
import org.apache.wicket.util.string.CssUtils;
import org.apache.wicket.util.string.interpolator.PropertyVariableInterpolator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.wicket.util.string.JavascriptStripper.stripCommentsAndWhitespace;

/**
 * Imporved version of HeaderContributor: <ul> <li>Naming conventions for default file name.</li> <li>Supports
 * intepolated templates.</li> <li>May hold more than one resource.</li> </ul>
 *
 * @author Yoav Aharoni
 */
public class ResourcePackage extends AbstractHeaderContributor {
    private List<IHeaderContributor> tempList = new ArrayList<IHeaderContributor>();
    private Class<?> scope;
    private IHeaderContributor[] headerContributors;

    protected ResourcePackage() {
        scope = getClass();
    }

    public ResourcePackage(Class<?> scope) {
        this.scope = scope;
    }

    public ResourcePackage dependsOn(ResourcePackage resourcePackage) {
        tempList.addAll(0, asList(resourcePackage.getHeaderContributors()));
        return this;
    }

    public ResourcePackage addCss() {
        return addCss(getDefalutCssPath());
    }

    public ResourcePackage addJavaScript() {
        return addJavaScript(getDefaultJavaScriptPath());
    }

    public ResourcePackage addCssTemplate() {
        return addCssTemplate(getDefalutCssPath());
    }

    public ResourcePackage addJavaScriptTemplate() {
        return addJavaScriptTemplate(getDefaultJavaScriptPath());
    }

    public ResourcePackage addCss(final String path) {
        return addCss(path, null);
    }

    public ResourcePackage addCss(final String path, final String media) {
        addContributor(new IHeaderContributor() {
            public void renderHead(IHeaderResponse response) {
                response.renderCSSReference(new CompressedResourceReference(scope, path), media);
            }
        });
        return this;
    }

    public ResourcePackage addJavaScript(final String path) {
        addContributor(new IHeaderContributor() {
            public void renderHead(IHeaderResponse response) {
                response.renderJavascriptReference(new JavascriptResourceReference(scope, path));
            }
        });
        return this;
    }

    public ResourcePackage addCssTemplate(final String path) {
        addContributor(new IHeaderContributor() {
            public void renderHead(IHeaderResponse response) {
                response.renderString(
                        CssUtils.INLINE_OPEN_TAG + readInterpolatedString(path) + CssUtils.INLINE_CLOSE_TAG);
            }
        });
        return this;
    }

    public ResourcePackage addJavaScriptTemplate(final String path) {
        addContributor(new IHeaderContributor() {
            public void renderHead(IHeaderResponse response) {
                response.renderJavascript(stripCommentsAndWhitespace(readInterpolatedString(path)), null);
            }
        });
        return this;
    }

    public String getResourceURL(String path) {
        ResourceReference reference = new ResourceReference(scope, path);
        return RequestCycle.get().urlFor(reference).toString();
    }

    /**
     * @see AbstractHeaderContributor#getHeaderContributors()
     */
    @Override
    public final IHeaderContributor[] getHeaderContributors() {
        return createHeaderContributors();
    }

    public static ResourcePackage forCss(Class scope) {
        return new ResourcePackage(scope).addCss();
    }

    public static ResourcePackage forJavaScript(Class scope) {
        return new ResourcePackage(scope).addJavaScript();
    }

    private String getDefaultJavaScriptPath() {
        return scope.getSimpleName() + ".js";
    }

    private String getDefalutCssPath() {
        return scope.getSimpleName() + ".css";
    }

    private String readInterpolatedString(String path) {
        try {
            PackageResource resource = PackageResource.get(scope, path);
            InputStream inputStream = resource.getResourceStream().getInputStream();
            String script = Streams.readString(inputStream);
            return PropertyVariableInterpolator.interpolate(script, this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private IHeaderContributor[] createHeaderContributors() {
        if (headerContributors == null) {
            headerContributors = new IHeaderContributor[tempList.size()];
            tempList.toArray(headerContributors);
            tempList = null;
        }
        return headerContributors;
    }

    private void addContributor(IHeaderContributor contributor) {
        if (tempList == null) {
            throw new RuntimeException("Can't add contributors after render phase!");
        }
        tempList.add(contributor);
    }
}
