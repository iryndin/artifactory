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
package org.artifactory.webapp.wicket.components;

import org.apache.log4j.Logger;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.StringAutoCompleteRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.file.Folder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class AutoCompletePath extends TextField {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(AutoCompletePath.class);

    public AutoCompletePath(String id, IModel object) {
        super(id, object);

        // this disables Firefox & IE autocomplete
        add(new SimpleAttributeModifier("autocomplete", "off"));

        addAutoCompleteBehavior();
    }

    private void addAutoCompleteBehavior() {
        add(new ImprovedAutoCompleteBehavior(StringAutoCompleteRenderer.INSTANCE) {
            private static final long serialVersionUID = 1L;

            protected Iterator getChoices(String input) {
                return AutoCompletePath.this.getChoices(input);
            }
        });
    }

    protected Iterator<String> getChoices(String input) {
        int idx = input.lastIndexOf('/');
        String folderName;
        if (idx < 0) {
            folderName = "/";
        } else {
            folderName = input.substring(0, idx + 1);
        }
        Folder folder = new Folder(folderName);
        if (folder.exists() && folder.isDirectory()) {
            Folder[] folders = folder.getFolders();
            File[] files = folder.getFiles();
            List<String> paths = new ArrayList<String>(folders.length + files.length);
            for (Folder file : folders) {
                String path = file.getPath().replace('\\', '/');
                if (path.startsWith(input)) {
                    paths.add(path);
                }
            }
            for (File file : files) {
                String path = file.getPath().replace('\\', '/');
                paths.add(path);
            }
            return paths.iterator();
        } else {
            return Collections.<String>emptyList().iterator();
        }
    }
}
