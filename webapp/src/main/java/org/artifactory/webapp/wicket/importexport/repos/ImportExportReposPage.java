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
package org.artifactory.webapp.wicket.importexport.repos;

import org.apache.log4j.Logger;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.artifactory.security.ArtifactorySecurityManager;
import org.artifactory.webapp.wicket.AuthenticatedPage;
import org.artifactory.webapp.wicket.components.MarkupIdOutputingFeedbackPanel;

@AuthorizeInstantiation(ArtifactorySecurityManager.ROLE_ADMIN)
public class ImportExportReposPage extends AuthenticatedPage {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(ImportExportReposPage.class);
    static final String ALL_REPOS = "All Repositories";


    /**
     * Constructor.
     */
    public ImportExportReposPage() {
        FeedbackPanel feedback = new MarkupIdOutputingFeedbackPanel("feedback");
        add(feedback);
        add(new ImportRepoPanel("importPanel"));
        add(new ImportZipPanel("importZipPanel"));
        add(new ExportRepoPanel("exportPanel"));
    }

    protected String getPageName() {
        return "Repositories Export & Import";
    }
}
