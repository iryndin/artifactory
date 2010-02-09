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
package org.artifactory.webapp.wicket.importexport.system;

import org.apache.log4j.Logger;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.artifactory.webapp.wicket.AuthenticatedPage;
import org.artifactory.webapp.wicket.components.MarkupIdOutputingFeedbackPanel;
import org.artifactory.webapp.wicket.importexport.repos.ImportExportReposPage;

@AuthorizeInstantiation("ADMIN")
public class ImportExportSystemPage extends AuthenticatedPage {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(ImportExportReposPage.class);


    /**
     * Constructor.
     */
    public ImportExportSystemPage() {
        add(new MarkupIdOutputingFeedbackPanel("feedback"));
        add(new ExportSystemPanel("exportPanel"));
        add(new ImportSystemPanel("importPanel"));
    }

    protected String getPageName() {
        return "Entire System Export & Import";
    }
}