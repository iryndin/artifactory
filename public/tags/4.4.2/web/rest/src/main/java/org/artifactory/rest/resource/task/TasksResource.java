/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2015 JFrog Ltd.
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

package org.artifactory.rest.resource.task;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.HaAddon;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.rest.constant.HaRestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.schedule.TaskBase;
import org.artifactory.schedule.TaskService;
import org.artifactory.storage.db.servers.service.ArtifactoryServersCommonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resource to provide REST API for background tasks (Binaries GC, artifacts cleanup etc.).
 *
 * @author Yossi Shaul
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path("tasks")
@RolesAllowed({AuthorizationService.ROLE_ADMIN, HaRestConstants.ROLE_HA})
public class TasksResource {
    private static final Logger log = LoggerFactory.getLogger(TasksResource.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private ArtifactoryServersCommonService serversService;

    /**
     * @return A list of active tasks - tasks that have active triggers and either are running or scheduled to run
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public BackgroundTasks getActiveTasks() {
        List<TaskBase> internalActiveTasks = taskService.getActiveTasks(t -> true);
        List<BackgroundTask> activeTasks = internalActiveTasks.stream()/*.filter(TaskBase::isRunning)*/
                .map(this::createBackgroundTask)
                .collect(Collectors.toList());

        BackgroundTasks backgroundTasks = new BackgroundTasks(activeTasks);
        addBackgroundTasksFromClusterNodes(backgroundTasks);

        return backgroundTasks;
    }

    private void addBackgroundTasksFromClusterNodes(BackgroundTasks tasks) {
        HaAddon haAddon = ContextHelper.get().beanForType(AddonsManager.class).addonByType(HaAddon.class);
        // collect from other HA nodes only if HA enabled and current request is not from another HA node
        if (haAddon.isHaEnabled() && !haAddon.isHaAuthentication()) {
            List<BackgroundTasks> nodesTasks = haAddon.propagateTasksList(
                    serversService.getOtherRunningHaMembers(), BackgroundTasks.class);
            for (BackgroundTasks nodeTasks : nodesTasks) {
                log.debug("Node tasks: {}", nodesTasks);
                tasks.addTasks(nodeTasks.getTasks());
            }
        }
    }

    private BackgroundTask createBackgroundTask(TaskBase t) {
        BackgroundTask bt = new BackgroundTask(t.getToken(), t.getType().getName(),
                t.getCurrentState().name().toLowerCase(), t.getDescription(),
                t.isRunning() ? t.getLastStarted() : 0);

        HaCommonAddon haAddon = ContextHelper.get().beanForType(AddonsManager.class).addonByType(HaCommonAddon.class);
        if (haAddon.isHaEnabled()) {
            String nodeId = haAddon.getCurrentMemberServerId();
            bt.setNodeId(nodeId);
        }
        return bt;
    }
}
