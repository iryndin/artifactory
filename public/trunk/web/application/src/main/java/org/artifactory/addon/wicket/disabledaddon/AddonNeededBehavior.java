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

package org.artifactory.addon.wicket.disabledaddon;

import org.apache.wicket.Component;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebRequest;
import org.artifactory.addon.wicket.Addon;
import org.artifactory.common.wicket.behavior.template.TemplateBehavior;
import org.artifactory.common.wicket.behavior.tooltip.TooltipBehavior;
import org.artifactory.common.wicket.contributor.ResourcePackage;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.addon.AddonSettings;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;

import javax.servlet.http.Cookie;

/**
 * @author Yoav Aharoni
 */
public class AddonNeededBehavior extends TemplateBehavior {
    private static final String MESSAGE_KEY = "addon.disabled";

    private Addon addon;
    private boolean enabled;

    public AddonNeededBehavior(Addon addon) {
        super(AddonNeededBehavior.class);
        this.addon = addon;

        ResourcePackage resourcePackage = getResourcePackage();
        resourcePackage.dependsOn(new ResourcePackage(TooltipBehavior.class).addJavaScript());
        resourcePackage.addJavaScript();
    }

    @Override
    public void beforeRender(Component component) {
        enabled = getAddonSettings().isShowAddonsInfo()
                && !getCookie("addon-" + addon.getAddonName()).equals(getServerToken());

        if (enabled) {
            super.beforeRender(component);
        }
    }

    @Override
    public void onRendered(Component component) {
        if (enabled) {
            super.onRendered(component);
        }
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);

        if (!enabled) {
            tag.put("style", "display: none;");
        } else {
            addCssClass(tag, "disabled-addon");
        }
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        component.setOutputMarkupId(true);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        final String contextPrefix = RequestUtils.getContextPrefix(WicketUtils.getWebRequest().getHttpServletRequest());
        response.renderJavascript(String.format("var artApp = '/%s'", contextPrefix), null);
    }

    public Addon getAddon() {
        return addon;
    }

    public String getServerToken() {
        return getAddonSettings().getShowAddonsInfoCookie();
    }

    private AddonSettings getAddonSettings() {
        return ArtifactoryApplication.get().getCentralConfig().getDescriptor().getAddons();
    }

    private String getCookie(String name) {
        Request request = RequestCycle.get().getRequest();
        if (request instanceof WebRequest) {
            Cookie cookie = ((WebRequest) request).getCookie(name);
            if (cookie == null) {
                return "";
            }
            return cookie.getValue();
        }
        return "";
    }

    public String getMessage() {
        return getComponent().getString(MESSAGE_KEY, new Model(addon));
    }
}