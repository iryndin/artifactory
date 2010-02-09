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

import org.jfrog.maven.viewer.ui.model.GroupIdBean;
import org.jfrog.maven.viewer.ui.view.form.GroupIdForm;
import org.springframework.richclient.dialog.CloseAction;
import org.springframework.richclient.dialog.FormBackedDialogPage;
import org.springframework.richclient.dialog.TitledPageApplicationDialog;

/**
 * Created by IntelliJ IDEA.
 * User: Dror Bereznitsky
 * Date: May 4, 2007
 * Time: 6:40:33 PM
 */
public class GroupIdDialog extends TitledPageApplicationDialog {
    private GroupIdForm form;
    private boolean isCancled;

    public GroupIdDialog() {
        super();
        setCloseAction(CloseAction.DISPOSE);

        form = new GroupIdForm(new GroupIdBean());
        setDialogPage(new FormBackedDialogPage(form));
    }

    public String getGroupId() {
        return form.getGroupId();
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
