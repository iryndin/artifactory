/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * User: freds
 * Date: Mar 14, 2007
 * Time: 8:43:49 PM
 */
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
package org.artifactory.webapp.wicket.common.component;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.UploadProgressBar;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.file.Files;
import org.apache.wicket.util.lang.Bytes;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.common.ArtifactoryHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


public class FileUploadForm extends Form {
    private static final Logger log = LoggerFactory.getLogger(FileUploadForm.class);

    @SpringBean
    private CentralConfigService centralConfig;

    protected FileUploadField fileUploadField;

    private File uploadedFile;

    private FileUploadParentPanel parent;

    /**
     * Construct.
     *
     * @param name   Component name
     * @param parent
     */
    public FileUploadForm(String name, FileUploadParentPanel parent) {
        super(name);
        this.parent = parent;
        //Set this form to multipart mode (always needed for uploads!)
        setMultiPart(true);
        //Add one file input field
        add(fileUploadField = new FileUploadField("fileInput"));
        //Set maximum upload size
        int uploadMaxSizeMb = centralConfig.getDescriptor().getFileUploadMaxSizeMb();
        if (uploadMaxSizeMb > 0) {
            setMaxSize(Bytes.megabytes(uploadMaxSizeMb));
        }
        // Add the progress bar
        add(new UploadProgressBar("progress", this));
    }

    @Override
    protected void onSubmit() {
        final FileUpload upload = fileUploadField.getFileUpload();
        if (upload != null) {
            //Create a new file
            File tmpUploadsDir = ArtifactoryHome.getTmpUploadsDir();
            uploadedFile = new File(tmpUploadsDir, upload.getClientFileName());
            //Check new file, delete if it allready existed
            if (!removeUploadedFile()) {
                error("File " + uploadedFile + " already exists and cannot be deleted !!");
                uploadedFile = null;
                return;
            }
            try {
                //Save to a new file
                FileUtils.forceMkdir(tmpUploadsDir);
                uploadedFile.createNewFile();
                upload.writeTo(uploadedFile);
                parent.info("Successfully uploaded file: '" + upload.getClientFileName() + ".");
                parent.onFileSaved();
            } catch (Exception e) {
                parent.onException();
                removeUploadedFile();
                throw new IllegalStateException(
                        "Unable to write file to '" + tmpUploadsDir.getAbsolutePath() + "'.", e);
            } finally {
                upload.closeStreams();
            }
        }
    }

    public File getUploadedFile() {
        return uploadedFile;
    }

    /**
     * Check whether the file allready exists, and if so, try to delete it.
     */
    public boolean removeUploadedFile() {
        if (uploadedFile != null && uploadedFile.exists()) {
            //Try to delete the file
            if (!Files.remove(uploadedFile)) {
                log.warn("Unable to remove/overwrite " + uploadedFile.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        //Cleanup resources if we are not staying on the same page
        Page targetPage = RequestCycle.get().getResponsePage();
        Page page = getPage();
        //Target page can be null on ajax requests - in this case we do not wish to clean up
        if (targetPage != null && !page.equals(targetPage)) {
            removeUploadedFile();
        }
    }
}
