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
package org.artifactory.webapp.wicket.page.base;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.VersionInfo;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;
import org.artifactory.webapp.wicket.application.ArtifactoryWebSession;
import org.artifactory.webapp.wicket.application.sitemap.PageNode;
import org.artifactory.webapp.wicket.application.test.TestRequestCycle;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.SimplePageLink;
import org.artifactory.webapp.wicket.common.component.modal.HasModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackDistributer;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackMessagesPanel;
import org.artifactory.webapp.wicket.common.component.panel.feedback.aggregated.AggregateFeedbackPanel;
import org.artifactory.webapp.wicket.page.home.HomePage;
import org.artifactory.webapp.wicket.page.search.ArtifactSearchPage;
import org.artifactory.webapp.wicket.page.security.login.LoginPage;
import org.artifactory.webapp.wicket.page.security.login.LogoutPage;
import org.artifactory.webapp.wicket.page.security.profile.ProfilePage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BasePage extends WebPage implements IAjaxIndicatorAware, HasModalHandler {

    @SpringBean
    private CentralConfigService centralConfig;

    @SpringBean
    private AuthorizationService authorizationService;

    private ModalHandler modalHandler;
    private Map<String, Integer> ids;

    protected BasePage() {
        //TODO: [by yl] Use a better way to test for logout before adding the components, like supporting menu-state
        //refresh calls after construction
        testLogout();
        setVersioned(false);

        add(new Label("pageTitle", new PropertyModel(this, "pageTitle")));
        add(new BookmarkablePageLink("artifactoryLink", HomePage.class));

        addFeedback();
        addDojo();
        addVersionInfo();
        addUserInfo();
        addMenu();
        addSearchForm();
        addModalHandler();
    }

    protected void testLogout() {
        //Do nothing
    }

    private void addFeedback() {
        FeedbackMessagesPanel defaultFeedback = new AggregateFeedbackPanel("defaultFeedback");
        add(defaultFeedback);

        FeedbackDistributer feedbackDistributer = new FeedbackDistributer("feedbackDistributer");
        add(feedbackDistributer);

        feedbackDistributer.setDefaultFeedbackPanel(defaultFeedback);
    }

    private void addModalHandler() {
        modalHandler = new ModalHandler("modal");
        add(modalHandler);
    }

    private void addSearchForm() {
        Form form = new Form("searchForm");
        add(form);

        final TextField searchTextField = new TextField("search", new Model(""));
        form.add(searchTextField);

        SimpleButton searchButton = new SimpleButton("searchButton", form, "Search") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String query = searchTextField.getModelObjectAsString();
                setResponsePage(new ArtifactSearchPage(query));
            }
        };
        form.setDefaultButton(searchButton);
        form.add(searchButton);
    }

    private void addUserInfo() {
        // Only allow users with updatable profile to change their profile

        // Enable only for signed in users
        add(new SimplePageLink("logoutPage", "Log Out", LogoutPage.class) {
            @Override
            public boolean isVisible() {
                return ArtifactoryWebSession.get().isSignedIn() && !authorizationService.isAnonymous();
            }
        });

        // Enable only if signed in as anonymous
        add(new SimplePageLink("loginPage", "Log In", LoginPage.class) {
            @Override
            public boolean isVisible() {
                return !ArtifactoryWebSession.get().isSignedIn() ||
                        authorizationService.isAnonymous();
            }
        });

        // logged in or not logged in
        add(new Label("loggedInLabel", getLoggedInMessage()));

        // update profile link
        SimplePageLink profileLink = new SimplePageLink("profilePage", getUserName(), ProfilePage.class) {
            @Override
            public boolean isEnabled() {
                return authorizationService.isUpdatableProfile();
            }

            @Override
            public boolean isVisible() {
                return ArtifactoryWebSession.get().isSignedIn() && !authorizationService.isAnonymous();
            }
        };
        profileLink.setBeforeDisabledLink(getUserName());
        add(profileLink);
    }

    public String getUserName() {
        return getAuthorizationService().currentUsername();
    }

    public String getLoggedInMessage() {
        if (!ArtifactoryWebSession.get().isSignedIn() || authorizationService.isAnonymous()) {
            return "Not logged in";
        }
        return "Logged in as";
    }

    private void addMenu() {
        RepeatingView menu = new RepeatingView("menuItem");
        List<PageNode> menuPages = ArtifactoryApplication.get().getSiteMap().getRoot().getChildren();
        for (PageNode pageNode : menuPages) {
            menu.add(new MenuItem(menu.newChildId(), pageNode, getMenuPageClass()));
        }
        add(menu);
    }

    protected Class<? extends BasePage> getMenuPageClass() {
        return getClass();
    }

    private void addVersionInfo() {
        // TODO: Need to cache this object
        VersionInfo versionInfo = centralConfig.getVersionInfo();
        String versionStr = "Artifactory " + versionInfo.getVersion() +
                " (rev. " + versionInfo.getRevision() + ")";
        add(new Label("version", new Model(versionStr)));
    }

    private void addDojo() {
        //Write the dojo debug configuration based on wicket configuration
        boolean debug = getApplication().getDebugSettings().isAjaxDebugModeEnabled();
        final String configJs = "var djConfig = { isDebug: " + debug +
                ", debugAtAllCosts: false, excludeNamespace: [\"wicket\"]};";
        WebComponent djConfig = new WebComponent("djConfig", new Model(configJs)) {
            @Override
            protected void onComponentTag(ComponentTag tag) {
                if (tag.isOpenClose()) {
                    tag.setType(XmlTag.OPEN);
                }
                super.onComponentTag(tag);
            }

            @Override
            protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                getResponse().write(configJs);
            }
        };
        WebRequestCycle webRequestCycle = (WebRequestCycle) getRequestCycle();
        WebRequest request = webRequestCycle.getWebRequest();
        String src = request.getRelativePathPrefixToContextRoot() + "130beta6/js/dojo/dojo.js";
        djConfig.add(new AttributeAppender("src", new Model(src), " "));
        add(djConfig);
    }

    public ModalHandler getModalHandler() {
        return modalHandler;
    }

    public final String getAjaxIndicatorMarkupId() {
        return "ajaxIndicator";
    }

    public Map<String, Integer> getIds() {
        if (ids == null) {
            ids = new HashMap<String, Integer>();
        }
        return ids;
    }

    @Override
    protected void componentChanged(Component component, MarkupContainer parent) {
        super.componentChanged(component, parent);

        // todo, i think this better be done with version manager
        RequestCycle requestCycle = RequestCycle.get();
        if (requestCycle instanceof TestRequestCycle) {
            TestRequestCycle cycle = (TestRequestCycle) requestCycle;
            if (cycle.getAjaxData() != null) {
                cycle.getAjaxData().componentChanged(component, parent);
            }
        }
    }

    public AuthorizationService getAuthorizationService() {
        return authorizationService;
    }

    public String getPageTitle() {
        String serverName = centralConfig.getServerName();
        String pageName = getPageName();
        return "Artifactory@" + serverName + " :: " + pageName;
    }

    protected abstract String getPageName();
}
