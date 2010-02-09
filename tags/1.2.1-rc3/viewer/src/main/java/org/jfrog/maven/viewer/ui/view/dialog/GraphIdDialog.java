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
package org.jfrog.maven.viewer.ui.view.dialog;

import org.jfrog.maven.viewer.common.ArtifactIdentifier;
import org.jfrog.maven.viewer.ui.model.ArtifactIdentifierBean;
import org.jfrog.maven.viewer.ui.view.form.ArtifactForm;
import org.springframework.richclient.dialog.CloseAction;
import org.springframework.richclient.dialog.FormBackedDialogPage;
import org.springframework.richclient.dialog.TitledPageApplicationDialog;

/**
 * User: Dror Bereznitsky
 * Date: 03/01/2007
 * Time: 00:31:46
 */
public class GraphIdDialog extends TitledPageApplicationDialog {
    private ArtifactForm form;
    private boolean isCancled = false;

    public GraphIdDialog() {
        super();
        setCloseAction(CloseAction.DISPOSE);

        form = new ArtifactForm(new ArtifactIdentifierBean());
        setDialogPage(new FormBackedDialogPage(form));
    }

    public ArtifactIdentifier getArtifactIdentifier() {
        ArtifactIdentifierBean bean = form.getArtifactIdentifierBean();
        return new ArtifactIdentifier(bean.getArtifactId(), bean.getGroupId(), bean.getVersion());
    }

    @Override
    protected boolean onFinish() {
        // commit any buffered edits to the domain
        form.getFormModel().commit();
        return true;
    }

    @Override
    protected void onCancel() {
        super.onCancel();
        isCancled = true;
    }

    public boolean isCancled() {
        return isCancled;
    }
}
