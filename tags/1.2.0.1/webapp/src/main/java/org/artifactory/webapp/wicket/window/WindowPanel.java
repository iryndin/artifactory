package org.artifactory.webapp.wicket.window;

import org.acegisecurity.Authentication;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;
import wicket.behavior.AttributeAppender;
import wicket.markup.html.basic.Label;
import wicket.markup.html.panel.Panel;
import wicket.model.IModel;
import wicket.model.Model;
import wicket.model.PropertyModel;

/**
 * @author Yoav Aharoni
 */
public class WindowPanel extends Panel {
    private static final String WINDOW_TITLE_KEY = "window.title";

    {
        final Label titleLabel = new Label("title", new PropertyModel(this, "title"));
        final AttributeAppender classAttributeAppender =
                new AttributeAppender("class", new Model("win_wrapper"), " ");
        add(titleLabel);
        add(classAttributeAppender);
    }

    public WindowPanel(String id) {
        super(id);
    }

    public WindowPanel(String id, IModel iModel) {
        super(id, iModel);
    }

    public String getTitle() {
        return getLocalizer().getString(WINDOW_TITLE_KEY, this);
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

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public Authentication getAuthentication() {
        Authentication authentication = SecurityHelper.getAuthentication();
        return authentication;
    }

    protected String getArtifactMetadataContent(ArtifactResource pa) {
        String repositoryKey = pa.getRepoKey();
        LocalRepo repo = getCc().localOrCachedRepositoryByKey(repositoryKey);
        String pom = repo.getPomContent(pa);
        if (pom == null) {
            pom = "No POM file found for '" + pa.getName() + "'.";
        }
        String artifactMetadata = pa.getActualArtifactXml();
        StringBuilder result = new StringBuilder();
        if (artifactMetadata != null && artifactMetadata.trim().length() > 0) {
            result.append("------ ARTIFACT EFFECTIVE METADATA BEGIN ------\n")
                    .append(artifactMetadata)
                    .append("------- ARTIFACT EFFECTIVE METADATA END -------\n\n");
        }
        result.append(pom);
        return result.toString();
    }

}
