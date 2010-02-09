package org.artifactory.webapp.wicket;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.artifactory.repo.RepoBase;
import org.artifactory.webapp.wicket.security.LoginPage;
import wicket.authentication.AuthenticatedWebApplication;
import wicket.authentication.AuthenticatedWebSession;
import wicket.extensions.ajax.markup.html.form.upload.UploadWebRequest;
import wicket.markup.html.WebPage;
import wicket.protocol.http.WebRequest;
import wicket.settings.IResourceSettings;
import wicket.util.file.Folder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryApp extends AuthenticatedWebApplication {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryApp.class);
    public static final Folder UPLOAD_FOLDER = new Folder(RepoBase.TEMP_FOLDER);

    @Override
    protected void init() {
        super.init();
        IResourceSettings resourceSettings = getResourceSettings();
        //Look for pages at the root of the web-app
        resourceSettings.addResourceFolder("");
        //Delete the upload folder (in case we were not shut down cleanly)
        deleteUploadsFolder();
        //Create the upload folder
        UPLOAD_FOLDER.mkdirs();
    }

    @Override
    protected Class<? extends AuthenticatedWebSession> getWebSessionClass() {
        return ArtifactorySession.class;
    }

    protected Class<? extends WebPage> getSignInPageClass() {
        return LoginPage.class;
    }

    @Override
    protected void destroy() {
        deleteUploadsFolder();
        super.destroy();
    }

    public Class getHomePage() {
        return HomePage.class;
    }

    /**
     * @see wicket.protocol.http.WebApplication#newWebRequest(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected WebRequest newWebRequest(HttpServletRequest servletRequest) {
        return new UploadWebRequest(servletRequest);
    }

    private void deleteUploadsFolder() {
        if (UPLOAD_FOLDER.exists()) {
            try {
                FileUtils.deleteDirectory(UPLOAD_FOLDER);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete the upload directory.");
            }
        }
    }
}
