package org.artifactory.webapp.wicket.panel;

import org.apache.log4j.Logger;
import org.artifactory.repo.CentralConfig;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;
import wicket.markup.html.panel.Panel;
import wicket.model.IModel;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ArtifactoryPanel extends Panel {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryPanel.class);

    public ArtifactoryPanel(String id) {
        super(id);
    }

    public ArtifactoryPanel(String id, IModel model) {
        super(id, model);
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
