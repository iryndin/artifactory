/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
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

package org.artifactory.rest.resource.repositories;

import org.artifactory.api.repo.CaseSensitivityRepairService;
import org.artifactory.api.rest.artifact.RepairPathConflictsResult;
import org.artifactory.api.rest.constant.RepairRepositoryConstants;
import org.artifactory.api.security.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author Yoav Luft
 */
@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Path(RepairRepositoryConstants.PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
public class RepairRepositoryResource {

    private static final Logger log = LoggerFactory.getLogger(RepairRepositoryResource.class);

    @Autowired
    CaseSensitivityRepairService repairService;

    /**
     * Returns a JSON list of files which have conflicting path information.
     *
     * @param path Root path for searching for conflicts
     * @param dryRun If true, no change to conflicting files will be done. Defaults to true.
     * @return {@link RepairPathConflictsResult} containing repair results
     * @throws IOException
     */
    @POST
    @Path("{path: .+}")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    public RepairPathConflictsResult repairPathConflicts(@PathParam("path") String path,
            @DefaultValue("true") @QueryParam("dry") String dryRun) {

        RepairPathConflictsResult result = repairService.findPathConflicts(path);
        if (!isDryRun(dryRun) && result.conflicts != null) {
            result = repairService.fixCaseConflicts(result.conflicts);
        }
        return result;
    }

    /**
     * @param s String representing the boolean value
     * @return by default returns "true" (meaning is dry run". If "0", "off" or "false" returns false.
     */
    private boolean isDryRun(String s) {
        if (s == null) {
            return true;
        }
        return !("0".equals(s) || "off".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s));
    }

}
