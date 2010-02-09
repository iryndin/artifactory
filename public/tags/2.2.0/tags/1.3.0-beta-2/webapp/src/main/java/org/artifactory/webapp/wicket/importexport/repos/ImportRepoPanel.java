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

import org.apache.wicket.model.PropertyModel;
import org.artifactory.webapp.wicket.components.AutoCompletePath;

/**
 * @author Yoav Aharoni
 */
public class ImportRepoPanel extends BasicImportPanel {

    public ImportRepoPanel(String string) {
        super(string);

        AutoCompletePath importFromPathTf =
                new AutoCompletePath("importFromPath", new PropertyModel(this, "importFromPath"));
        importFromPathTf.setRequired(true);
        //--importFromPath.add(new ValidateArtifactFormBehavior("onKeyup"));
        getImportForm().add(importFromPathTf);
        //Add the import button
        /*Link importButton = new AjaxFallbackLink("import") {
            public void onClick(final AjaxRequestTarget target) {
               targetRepo.importFolder(new File(importFromPath));
            }
        };*/
    }

}
