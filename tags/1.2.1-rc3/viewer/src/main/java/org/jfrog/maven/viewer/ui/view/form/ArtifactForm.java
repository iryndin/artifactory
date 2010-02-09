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

import org.jfrog.maven.viewer.ui.model.ArtifactIdentifierBean;
import org.springframework.richclient.form.AbstractForm;
import org.springframework.richclient.form.builder.TableFormBuilder;

import javax.swing.*;

/**
 * User: Dror Bereznitsky
 * Date: 16/03/2007
 * Time: 14:55:06
 */
public class ArtifactForm extends AbstractForm {
    private JComponent versionField;
    private JComponent artifactIdField;
    private JComponent groupId;

    public ArtifactForm(ArtifactIdentifierBean bean) {
        super(bean);
        setId("artifact");
    }

    protected JComponent createFormControl() {
        TableFormBuilder formBuilder = new TableFormBuilder(getBindingFactory());
        formBuilder.setLabelAttributes("colGrId=label colSpec=left:pref");

        formBuilder.row();
        groupId = formBuilder.add("groupId")[1];
        formBuilder.row();
        artifactIdField = formBuilder.add("artifactId")[1];
        formBuilder.row();
        versionField = formBuilder.add("version")[1];
        return formBuilder.getForm();
    }

    public ArtifactIdentifierBean getArtifactIdentifierBean() {
        return (ArtifactIdentifierBean) getFormModel().getFormObject();
    }

    public boolean requestFocusInWindow() {
        return groupId.requestFocusInWindow();
    }
}
