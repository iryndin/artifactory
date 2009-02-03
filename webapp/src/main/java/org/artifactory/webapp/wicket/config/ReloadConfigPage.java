/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket.config;

import org.apache.log4j.Logger;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.AuthenticatedPage;
import org.artifactory.webapp.wicket.component.NotifingFeedbackPanel;

import java.util.List;

@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class ReloadConfigPage extends AuthenticatedPage {
    private final static Logger LOGGER = Logger.getLogger(ReloadConfigPage.class);

    @SpringBean
    private CentralConfigService centralConfig;

    @SpringBean
    private RepositoryService repoService;

    public ReloadConfigPage() {
        //Feedback
        FeedbackPanel feedback = new NotifingFeedbackPanel("feedback");
        add(feedback);
        //Reload the configuration
        try {
            centralConfig.reload();
            List<String> repoKeys = repoService.getAllRepoKeys();
            RequestUtils.addRepoPathPrefixes(repoKeys);
            info("Configuration reloaded successfully.");
        } catch (Exception e) {
            String msg = "Failed to reload configuration: " + e.getMessage();
            error(msg);
            LOGGER.error(msg, e);
        }
    }

    @Override
    protected String getPageName() {
        return "Reload Configuration";
    }
}