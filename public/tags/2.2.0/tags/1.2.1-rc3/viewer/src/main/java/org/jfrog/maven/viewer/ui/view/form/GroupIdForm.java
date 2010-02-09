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
package org.jfrog.maven.viewer.ui.view.form;

import org.jfrog.maven.viewer.ui.model.GroupIdBean;
import org.springframework.richclient.form.AbstractForm;
import org.springframework.richclient.form.builder.TableFormBuilder;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: Dror Bereznitsky
 * Date: May 4, 2007
 * Time: 6:35:55 PM
 */
public class GroupIdForm extends AbstractForm {
    private JComponent groupId;

    public GroupIdForm(GroupIdBean formObject) {
        super(formObject);
        setId("groupIdForm");
    }

    protected JComponent createFormControl() {
        TableFormBuilder formBuilder = new TableFormBuilder(getBindingFactory());
        formBuilder.setLabelAttributes("colGrId=label colSpec=left:pref");

        formBuilder.row();
        groupId = formBuilder.add("groupId")[1];
        formBuilder.row();
        return formBuilder.getForm();
    }

    public String getGroupId() {
        return ((GroupIdBean) getFormModel().getFormObject()).getGroupId();
    }
}
