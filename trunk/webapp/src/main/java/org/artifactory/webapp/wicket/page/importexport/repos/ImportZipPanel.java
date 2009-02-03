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
package org.artifactory.webapp.wicket.page.importexport.repos;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.webapp.wicket.common.component.FileUploadForm;
import org.artifactory.webapp.wicket.common.component.FileUploadParentPanel;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(ImportZipPanel.class);

    private FileUploadForm uploadForm;
    private static final int DEFAULT_BUFF_SIZE = 8192;

    public ImportZipPanel(String string) {
        super(string);

        //Add upload form with ajax progress bar
        uploadForm = new FileUploadForm("uploadForm", this);
        uploadForm.add(new SchemaHelpBubble("uploadHelp", "", getUploadHelpText()));
        add(uploadForm);

        getImportForm().setVisible(false);
        getImportForm().add(new SchemaHelpBubble("repoSelectHelp", "", getRepoSelectHelpText()));
    }

    private String getUploadHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Uploads the archive you select.\n");
        sb.append(
                "When importing a single repository, the file structure within the archive should be similar to:\n");
        sb.append("ARCHIVE.ZIP\n");
        sb.append(" |\n");
        sb.append(" |--LIB_DIR_1\n");
        sb.append("\n");
        sb.append(
                "But when importing all repositories, the file structure within the archive should be similar to:\n");
        sb.append("ARCHIVE.ZIP\n");
        sb.append(" |\n");
        sb.append(" |--REPOSITORY_NAME_DIR_1\n");
        sb.append(" |    |\n");
        sb.append(" |    |--LIB_DIR_1\n");
        sb.append("\n");
        sb.append(
                "When importing all repositories, make sure that the names of the directories that represent\n");
        sb.append(
                "The repositories in the archive, match the names of the targeted repositories in the application.\n");
        sb.append("Please note that uploading the archive, does not import it's content.\n");
        sb.append(
                "To import, choose a repository (or all of them) and press import (will appear after upload).\n");
        return sb.toString();
    }

    private String getRepoSelectHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Selects where to import the uploaded content.\n");
        sb.append(
                "If the archive contains only artficat libraries, select the repository you would like to import them to.\n");
        sb.append(
                "In a case where the archive contains a structure similar to that of a collection of repositories,\n");
        sb.append("Select \"All Repositories\".");
        return sb.toString();
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
            log.error("Error during import of " + uploadedFile, e);
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
        log.debug("Cleaning up zip import resources.");
        if (getImportFromPath() != null) {
            try {
                FileUtils.deleteDirectory(getImportFromPath());
            } catch (IOException e) {
                log.warn("Cannot clean extract directory " + getImportFromPath());
            }
            setImportFromPath(null);
        }
        uploadForm.removeUploadedFile();
    }
}