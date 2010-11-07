/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.webapp.servlet.webdav;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.servlet.RequestUtils;

import javax.jcr.Repository;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author yoavl
 */
public class WebdavAdminServlet extends SimpleWebdavServlet {

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
        ArtifactoryContext applicationContext = RequestUtils.getArtifactoryContext(sc);
        AuthorizationService authService = applicationContext.getAuthorizationService();
        if (!authService.isAdmin()) {
            return newEmptyRepository();
        }
        RepositoryService repoService = applicationContext.beanForType(RepositoryService.class);
        Repository repository = (Repository) repoService.getJcrHandle();
        return repository;
    }

    private static Repository newEmptyRepository() {
        return new TransientRepository();
    }
}
