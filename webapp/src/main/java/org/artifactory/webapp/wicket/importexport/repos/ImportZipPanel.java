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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.webapp.wicket.component.FileUploadForm;
import org.artifactory.webapp.wicket.component.FileUploadParentPanel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Yoav Aharoni
 */
public class ImportZipPanel extends BasicImportPanel implements FileUploadParentPanel {
    private final static Logger LOGGER = Logger.getLogger(ImportZipPanel.class);

    private FileUploadForm uploadForm;
    private static final int DEFAULT_BUFF_SIZE = 8192;

    public ImportZipPanel(String string) {
        super(string);

        //Add upload form with ajax progress bar
        uploadForm = new FileUploadForm("uploadForm", this);
        add(uploadForm);

        getImportForm().setVisible(false);
    }

    public void onException() {
        getImportForm().setVisible(false);
    }

    public void onFileSaved() {
        ZipInputStream zipinputstream = null;
        FileOutputStream fos = null;
        File uploadedFile = null;
        try {
            uploadedFile = uploadForm.getUploadedFile();
            zipinputstream = new ZipInputStream(new FileInputStream(uploadedFile));
            File destFolder =
                    new File(ArtifactoryHome.getTmpUploadsDir(),
                            uploadedFile.getName() + "_extract");
            FileUtils.deleteDirectory(destFolder);

            byte[] buf = new byte[DEFAULT_BUFF_SIZE];
            ZipEntry zipentry;

            zipentry = zipinputstream.getNextEntry();
            while (zipentry != null) {
                //for each entry to be extracted
                String entryName = zipentry.getName();
                File destFile = new File(destFolder, entryName);

                if (zipentry.isDirectory()) {
                    if (!destFile.exists()) {
                        if (!destFile.mkdirs()) {
                            error("Cannot create directory " + destFolder);
                            return;
                        }
                    }
                } else {
                    fos = new FileOutputStream(destFile);
                    int n;
                    while ((n = zipinputstream.read(buf, 0, DEFAULT_BUFF_SIZE)) > -1) {
                        fos.write(buf, 0, n);
                    }
                    fos.close();
                }
                zipinputstream.closeEntry();
                zipentry = zipinputstream.getNextEntry();
            }//while

            setImportFromPath(destFolder);
            getImportForm().setVisible(true);
        } catch (Exception e) {
            error(e.getMessage());
            LOGGER.error("Error during import of " + uploadedFile, e);
            onException();
        } finally {
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(zipinputstream);
        }
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        //Cleanup resources if we are not staying on the same page
        Page targetPage = RequestCycle.get().getResponsePage();
        Page page = getPage();
        if (targetPage != null && !page.equals(targetPage)) {
            cleanupResources();
        }
    }

    protected void cleanupResources() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Cleaning up zip import resources.");
        }
        if (getImportFromPath() != null) {
            try {
                FileUtils.deleteDirectory(getImportFromPath());
            } catch (IOException e) {
                LOGGER.warn("Cannot clean extract directory " + getImportFromPath());
            }
            setImportFromPath(null);
        }
        uploadForm.removeUploadedFile();
    }
}