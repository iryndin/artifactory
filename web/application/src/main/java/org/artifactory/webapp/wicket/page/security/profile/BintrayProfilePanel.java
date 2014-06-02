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

package org.artifactory.webapp.wicket.page.security.profile;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.bintray.BintrayService;
import org.artifactory.api.bintray.BintrayUser;
import org.artifactory.api.bintray.exception.BintrayException;
import org.artifactory.common.wicket.ajax.ImmediateAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.WicketUtils;

import java.io.IOException;
import java.util.Map;

/**
 * @author Shay Yaakov
 */
public class BintrayProfilePanel extends TitledPanel {

    @SpringBean
    private BintrayService bintrayService;

    @SpringBean
    private AddonsManager addonsManager;

    private static final String HIDDEN_PASSWORD = "************";

    private final TextField<String> bintrayUsername;
    private final TextField<String> bintrayApiKey;
    private final TitledAjaxSubmitLink testButton;

    public BintrayProfilePanel(String id, final ProfileModel profile) {
        super(id);
        setOutputMarkupId(true);
        setDefaultModel(new CompoundPropertyModel<>(profile));
        add(new CssClass("display:block"));
        bintrayUsername = new TextField<>("bintrayUsername", new Model<>(HIDDEN_PASSWORD));
        bintrayUsername.setEnabled(false);
        add(bintrayUsername);
        add(new HelpBubble("bintrayUsername.help", new ResourceModel("bintrayUsername.help")));

        bintrayApiKey = new TextField<>("bintrayApiKey", new Model<>(HIDDEN_PASSWORD));
        bintrayApiKey.setEnabled(false);
        add(bintrayApiKey);
        add(new HelpBubble("bintrayApiKey.help", new ResourceModel("bintrayApiKey.help")));

        Component bintrayLink;
        if (StringUtils.isBlank(profile.getBintrayAuth())) {
            bintrayLink = new ExternalLink("bintrayLink", bintrayService.getBintrayRegistrationUrl(),
                    "Register to Bintray...");
            bintrayLink.add(new CssClass("bintray-link"));
        } else {
            bintrayLink = new WebMarkupContainer("bintrayLink");
            bintrayLink.setVisible(false);
        }
        add(bintrayLink);

        testButton = new TitledAjaxSubmitLink("test", "Test") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    Map<String, String> headersMap = WicketUtils.getHeadersMap();
                    BintrayUser bintrayUser = bintrayService.getBintrayUser(profile.getBintrayUsername(),
                            profile.getBintrayApiKey(), headersMap);
                    info("Successfully authenticated '" + bintrayUser.getFullName() + "' with Bintray.");
                } catch (IOException e) {
                    error("Connection failed with exception: " + e.getMessage());
                } catch (BintrayException e) {
                    error("Could not authenticate user: " + e.getStatus() + " " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    error(e.getMessage());
                }
                AjaxUtils.refreshFeedback(target);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new ImmediateAjaxIndicatorDecorator();
            }
        };
        testButton.setEnabled(false);
        add(testButton);
    }

    @Override
    public String getTitle() {
        return "Bintray Settings";
    }

    @Override
    protected Component newToolbar(String id) {
        return new HelpBubble(id, new ResourceModel("bintray.help"));
    }

    public void updateDefaultModel(ProfileModel profileModel, boolean enableFields) {
        bintrayUsername.setDefaultModel(new PropertyModel<String>(profileModel, "bintrayUsername"));
        bintrayUsername.setEnabled(enableFields);

        bintrayApiKey.setDefaultModel(new PropertyModel<String>(profileModel, "bintrayApiKey"));
        bintrayApiKey.setEnabled(enableFields);

        testButton.setEnabled(enableFields);
    }
}
