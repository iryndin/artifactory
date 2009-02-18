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

import org.apache.wicket.ajax.AjaxRequestTarget;
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
import org.apache.wicket.model.AbstractReadOnlyModel;
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
import org.artifactory.webapp.wicket.application.sitemap.MenuNode;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.modal.HasModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackDistributer;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackMessagesPanel;
import org.artifactory.webapp.wicket.common.component.panel.feedback.aggregated.AggregateFeedbackPanel;
import org.artifactory.webapp.wicket.page.home.HomePage;
import org.artifactory.webapp.wicket.page.search.ArtifactSearchPage;

import java.util.List;

public abstract class BasePage extends WebPage implements HasModalHandler {

    @SpringBean
    private CentralConfigService centralConfig;

    @SpringBean
    private AuthorizationService authorizationService;

    private ModalHandler modalHandler;

    protected BasePage() {
        init();
    }

    protected void init() {
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
        Form form = new Form("searchForm") {
            @Override
            public boolean isVisible() {
                return isSignedInOrAnonymous();
            }
        };
        add(form);

        final TextField searchTextField = new TextField("search", new Model("")) {
        };
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

    protected abstract String getPageName();

    protected Class<? extends BasePage> getMenuPageClass() {
        return getClass();
    }

    public ModalHandler getModalHandler() {
        return modalHandler;
    }

    public String getPageTitle() {
        String serverName = centralConfig.getServerName();
        String pageName = getPageName();
        return "Artifactory@" + serverName + " :: " + pageName;
    }

    private void addVersionInfo() {
        VersionInfo versionInfo = centralConfig.getVersionInfo();
        String versionText = "Artifactory "
                + versionInfo.getVersion()
                + " (rev. " + versionInfo.getRevision() + ")";
        add(new Label("version", versionText));
    }

    private void addUserInfo() {
        // Enable only for signed in users
        add(new LogoutLink("logoutPage", "Log Out"));

        // Enable only if signed in as anonymous
        add(new LoginLink("loginPage", "Log In"));

        // logged in or not logged in
        add(new Label("loggedInLabel", getLoggedInMessage()));

        // update profile link
        add(new EditProfileLink("profilePage"));
    }

    private void addMenu() {
        RepeatingView menu = new RepeatingView("menuItem");
        List<MenuNode> menuPages = ArtifactoryApplication.get().getSiteMap().getRoot().getChildren();
        for (MenuNode pageNode : menuPages) {
            menu.add(new MenuItem(menu.newChildId(), pageNode, getMenuPageClass()));
        }
        add(menu);
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

        djConfig.add(new AttributeAppender("src", new AbstractReadOnlyModel() {
            public Object getObject() {
                WebRequestCycle webRequestCycle = (WebRequestCycle) getRequestCycle();
                WebRequest request = webRequestCycle.getWebRequest();
                return request.getRelativePathPrefixToContextRoot() + "v200/js/dojo/dojo.js";
            }
        }, " "));
        add(djConfig);
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
