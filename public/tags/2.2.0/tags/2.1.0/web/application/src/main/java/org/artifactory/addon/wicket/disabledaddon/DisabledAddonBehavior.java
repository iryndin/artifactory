package org.artifactory.addon.wicket.disabledaddon;

import org.apache.wicket.Component;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.util.value.IValueMap;
import org.artifactory.addon.wicket.Addon;
import org.artifactory.common.wicket.behavior.template.TemplateBehavior;
import org.artifactory.common.wicket.behavior.tooltip.TooltipBehavior;
import org.artifactory.common.wicket.contributor.ResourcePackage;
import org.artifactory.descriptor.addon.AddonSettings;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;

import javax.servlet.http.Cookie;

/**
 * @author Yoav Aharoni
 */
public class DisabledAddonBehavior extends TemplateBehavior {
    private static final String MESSAGE_KEY = "addon.disabled";

    private Addon addon;
    private boolean enabled;

    public DisabledAddonBehavior(Addon addon) {
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
        IValueMap attributes = tag.getAttributes();
        attributes.remove("href");
        attributes.remove("onclick");

        if (!enabled) {
            attributes.put("style", "display: none;");
        } else {
            addCssClass(tag, "disabled-addon");
        }
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        component.setOutputMarkupId(true);
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
