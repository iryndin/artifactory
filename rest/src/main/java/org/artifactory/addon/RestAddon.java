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

package org.artifactory.addon;

import org.artifactory.api.rest.artifact.MoveCopyResult;
import org.artifactory.rest.resource.artifact.DownloadResource;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

/**
 * An interface that holds all the REST related operations that are available only as part of Artifactory's Add-ons.
 *
 * @author Tomer Cohen
 */
public interface RestAddon extends AddonFactory {
    /**
     * Copy an artifact from one path to another.
     *
     * @param path   The source path of the artifact.
     * @param target The target repository where to copy/move the Artifact to.
     * @param dryRun A flag to indicate whether to perform a dry run first before performing the actual action.
     * @return A JSON object of all the messages and errors that occurred during the action.
     * @throws Exception If an error occurred during the dry run or the actual action an exception is thrown.
     */
    MoveCopyResult copy(String path, String target, int dryRun) throws Exception;

    /**
     * Move an artifact from one path to another.
     *
     * @param path   The source path of the artifact.
     * @param target The target repository where to copy/move the Artifact to.
     * @param dryRun A flag to indicate whether to perform a dry run first before performing the actual action.
     * @return A JSON object of all the messages and errors that occurred during the action.
     * @throws Exception If an error occurred during the dry run or the actual action an exception is thrown.
     */
    MoveCopyResult move(String path, String target, int dryRun) throws Exception;

    Response download(String path, DownloadResource.Content content, int mark, HttpServletResponse response)
            throws Exception;
}
