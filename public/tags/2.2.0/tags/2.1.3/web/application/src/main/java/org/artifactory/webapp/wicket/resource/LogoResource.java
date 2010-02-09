package org.artifactory.webapp.wicket.resource;

import org.apache.wicket.markup.html.WebResource;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.artifactory.common.ArtifactoryHome;

import java.io.File;

/**
 * Resource to get the uploaded user logo
 *
 * @author Tomer Cohen
 */
public class LogoResource extends WebResource {

    private ArtifactoryHome artifactoryHome;

    public LogoResource(ArtifactoryHome artifactoryHome) {
        this.artifactoryHome = artifactoryHome;
    }

    @Override
    public IResourceStream getResourceStream() {
        File logoFile = new File(artifactoryHome.getLogoDir(), "logo");
        return new FileResourceStream(logoFile);
    }
}
