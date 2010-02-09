/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.page.base;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.WebApplicationAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.HasModalHandler;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.panel.blocker.Blocker;
import org.artifactory.common.wicket.component.panel.feedback.FeedbackDistributer;
import org.artifactory.common.wicket.component.panel.feedback.FeedbackMessagesPanel;
import org.artifactory.common.wicket.component.panel.feedback.aggregated.AggregateFeedbackPanel;
import org.artifactory.common.wicket.component.panel.sidemenu.MenuPanel;
import org.artifactory.common.wicket.resources.domutils.CommonJsPackage;
import org.artifactory.web.ui.skins.GreenSkin;
import org.artifactory.webapp.wicket.application.ArtifactoryWebSession;
import org.artifactory.webapp.wicket.page.search.artifact.ArtifactSearchPage;

public abstract class BasePage extends WebPage implements HasModalHandler {

    @SpringBean
    private CentralConfigService centralConfig;

    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private AddonsManager addons;

    private ModalHandler modalHandler;

    protected BasePage() {
        init();
    }

    public ModalHandler getModalHandler() {
        return modalHandler;
    }

    public String getPageTitle() {
        return addons.addonByType(WebApplicationAddon.class).getPageTitle(this);
    }

    protected void init() {
        setVersioned(false);

        add(new CommonJsPackage());
        add(new GreenSkin());

        add(new Label("pageTitle", new PropertyModel(this, "pageTitle")));
        String footer = getFooter();
        WebMarkupContainer footerWrapper = new WebMarkupContainer("footerWrapper");
        footerWrapper.setOutputMarkupId(true);
        Label footerLabel = new Label("footer", new Model(footer));
        footerWrapper.add(footerLabel);
        add(footerWrapper);
        WebMarkupContainer revisionHeader = new WebMarkupContainer("revision", new Model());
        String revisionValue = centralConfig.getVersionInfo().getRevision();
        revisionHeader.add(new AttributeModifier("content", new Model(revisionValue)));
        add(revisionHeader);

        WebApplicationAddon addon = addons.addonByType(WebApplicationAddon.class);
        WebMarkupContainer logoWrapper = new WebMarkupContainer("logoWrapper");
        logoWrapper.setOutputMarkupId(true);
        add(logoWrapper);
        WebMarkupContainer homeLink = addon.getHomeLink("artifactoryLink");
        logoWrapper.add(homeLink);

        addAjaxIndicator();
        addFeedback();
        addVersionInfo();
        addUserInfo();
        addMenu();
        addSearchForm();
        addModalHandler();
    }

    private String getFooter() {
        String footer = centralConfig.getMutableDescriptor().getFooter();
        return footer;
    }

    @Override
    protected final void configureResponse() {
        super.configureResponse();

        // add no-store for avoiding browser cache
        final WebResponse response = getWebRequestCycle().getWebResponse();
        response.setHeader("Cache-Control", "no-store, no-cache, max-age=0, must-revalidate");
    }

    public abstract String getPageName();

    protected Class<? extends BasePage> getMenuPageClass() {
        return getClass();
    }

    private void addAjaxIndicator() {
        add(new Blocker("blocker"));
    }

    private void addFeedback() {
        FeedbackMessagesPanel defaultFeedback = newFeedbackPanel("defaultFeedback");
        add(defaultFeedback);

        FeedbackDistributer feedbackDistributer = new FeedbackDistributer("feedbackDistributer");
        add(feedbackDistributer);

        feedbackDistributer.setDefaultFeedbackPanel(defaultFeedback);
    }

    protected FeedbackMessagesPanel newFeedbackPanel(String id) {
        return new AggregateFeedbackPanel(id);
    }

    private void addModalHandler() {
        modalHandler = new ModalHandler("modal");
        add(modalHandler);
    }

    private void addSearchForm() {
        Form form = new Form("searchForm") {
            @Override
            public boolean isVisible() {
                return isSignedInOrAnonymous();
            }
        };
        add(form);

        final TextField searchTextField = new TextField("query", new Model(""));
        form.add(searchTextField);

        TitledAjaxSubmitLink searchButton = new TitledAjaxSubmitLink("searchButton", "Search", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String query = searchTextField.getModelObjectAsString();
                setResponsePage(new ArtifactSearchPage(query));
            }
        };
        form.setDefaultButton(searchButton);
        form.add(searchButton);
    }

    private void addVersionInfo() {
        String versionInfo = addons.addonByType(WebApplicationAddon.class).getVersionInfo();
        add(new Label("version", versionInfo));
    }

    private void addUserInfo() {
        AbstractLink logoutLink;
        WebApplicationAddon applicationAddon;
        AbstractLink profileLink;
        // Enable only for signed in users
        applicationAddon = addons.addonByType(WebApplicationAddon.class);
        logoutLink = applicationAddon.getLogoutLink("logoutPage");
        profileLink = applicationAddon.getProfileLink("profilePage");
        add(logoutLink);
        // Enable only if signed in as anonymous
        add(new LoginLink("loginPage", "Log In"));

        // logged in or not logged in
        add(new Label("loggedInLabel", getLoggedInMessage()));

        // update profile link
        add(profileLink);
    }

    private void addMenu() {
        add(new MenuPanel("menuItem", getMenuPageClass()));
    }

    private String getLoggedInMessage() {
        if (isNotSignedInOrAnonymous()) {
            return "Not logged in";
        }
        return "Logged in as";
    }


    private boolean isSignedInOrAnonymous() {
        return (ArtifactoryWebSession.get().isSignedIn() && !authorizationService.isAnonymous()) ||
                (authorizationService.isAnonymous() && authorizationService.isAnonAccessEnabled());
    }

    private boolean isNotSignedInOrAnonymous() {
        return !ArtifactoryWebSession.get().isSignedIn() || authorizationService.isAnonymous();
    }
}
