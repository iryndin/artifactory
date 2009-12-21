/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * User: freds
 * Date: Mar 14, 2007
 * Time: 8:43:49 PM
 */

package org.artifactory.common.wicket.panel.upload;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.UploadProgressBar;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;

import java.io.File;

import static org.artifactory.util.FileUtils.removeFile;


public class FileUploadForm extends Form {
    protected FileUploadField fileUploadField;
    private File uploadedFile;
    private UploadListener listener;
    private File tempUploadsDir;

    /**
     * @param id            The wicket component id
     * @param tempUploadDir Path to a temp upload directory
     * @param listener      Parent component holding this upload form which will receive events
     */
    public FileUploadForm(String id, String tempUploadDir, UploadListener listener) {
        super(id);
        this.listener = listener;
        tempUploadsDir = new File(tempUploadDir);

        //Set this form to multipart mode (always needed for uploads!)
        setMultiPart(true);
        //Add one file input field
        add(fileUploadField = new FileUploadField("fileInput"));
        // Add the progress bar
        add(new UploadProgressBar("progress", this));
    }

    @Override
    protected void onSubmit() {
        final FileUpload upload = fileUploadField.getFileUpload();
        if (upload != null) {
            //Create a new file
            uploadedFile = new File(tempUploadsDir, upload.getClientFileName());
            //Check new file, delete if it already existed
            if (!removeUploadedFile()) {
                error("File " + uploadedFile + " already exists and cannot be deleted !!");
                uploadedFile = null;
                return;
            }
            try {
                //Save to a new file
                FileUtils.forceMkdir(tempUploadsDir);
                uploadedFile.createNewFile();
                upload.writeTo(uploadedFile);
                listener.onFileSaved(uploadedFile);
            } catch (Exception e) {
                listener.onException();
                removeUploadedFile();
                throw new IllegalStateException(
                        "Unable to write file to '" + tempUploadsDir.getAbsolutePath() + "'.", e);
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
     *
     * @return boolean
     */
    public boolean removeUploadedFile() {
        return removeFile(uploadedFile);
    }

    @Override
    protected void onDetach() {
        cleanupOnDetach();
        super.onDetach();
    }

    protected void cleanupOnDetach() {
        //Cleanup resources if we are not staying on the same page
        Page targetPage = RequestCycle.get().getResponsePage();
        Page page = getPage();
        //Target page can be null on ajax requests - in this case we do not wish to clean up
        if (targetPage != null && !page.equals(targetPage)) {
            removeUploadedFile();
        }
    }
}
