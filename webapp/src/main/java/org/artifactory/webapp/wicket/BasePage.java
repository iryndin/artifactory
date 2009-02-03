package org.artifactory.webapp.wicket;

import org.artifactory.repo.CentralConfig;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;
import org.artifactory.webapp.wicket.home.HomePage;
import wicket.markup.ComponentTag;
import wicket.markup.MarkupStream;
import wicket.markup.html.WebComponent;
import wicket.markup.html.WebPage;
import wicket.markup.html.link.BookmarkablePageLink;
import wicket.markup.parser.XmlTag;
import wicket.model.Model;

public class BasePage extends WebPage {
    public BasePage() {
        //Write the dojo debug configuration based on wicket configuration
        boolean debug = getApplication().getDebugSettings().isAjaxDebugModeEnabled();
        final String configJs = "var djConfig = { isDebug: " + debug +
                ", debugAtAllCosts: false, excludeNamespace: [\"wicket\"]};";
        WebComponent djConfig = new WebComponent("djConfig", new Model(configJs)) {
            @Override
            protected void onComponentTag(final ComponentTag tag) {
                if (tag.isOpenClose()) {
                    tag.setType(XmlTag.OPEN);
                }
                super.onComponentTag(tag);
            }

            @Override
            protected void onComponentTagBody(final MarkupStream markupStream,
                                              final ComponentTag openTag) {
                getResponse().write(configJs);
            }
        };
        add(djConfig);
        add(new BookmarkablePageLink("artifactory_link", HomePage.class));
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public ArtifactoryContext getContext() {
        ArtifactoryContext context = ContextUtils.getArtifactoryContext();
        return context;
    }

    public CentralConfig getCc() {
        ArtifactoryContext context = getContext();
        return context.getCentralConfig();
    }
}
