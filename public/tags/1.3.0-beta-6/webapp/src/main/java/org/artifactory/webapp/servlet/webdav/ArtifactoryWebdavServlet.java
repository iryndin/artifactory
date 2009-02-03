package org.artifactory.webapp.servlet.webdav;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.springframework.web.context.WebApplicationContext;

import javax.jcr.Repository;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryWebdavServlet extends SimpleWebdavServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //Reset the session provider so that we always get the repository according to the logged in
        //user, and not a cached one
        setDavSessionProvider(null);
        super.service(request, response);
    }

    @Override
    public Repository getRepository() {
        ServletContext sc = getServletContext();
        ArtifactoryContext applicationContext =
                (ArtifactoryContext) sc.getAttribute(
                        WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        AuthorizationService authService = applicationContext.getAuthorizationService();
        if (!authService.isAdmin()) {
            return newEmptyRepository();
        }
        RepositoryService repoService = applicationContext.getRepositoryService();
        Repository repository = repoService.getRepository();
        return repository;
    }

    private static Repository newEmptyRepository() {
        try {
            return new TransientRepository();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create transient repository.", e);
        }
    }
}
