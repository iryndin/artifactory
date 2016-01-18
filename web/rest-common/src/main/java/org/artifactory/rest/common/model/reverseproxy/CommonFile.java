package org.artifactory.rest.common.model.reverseproxy;

import org.artifactory.rest.common.model.FileModel;
import org.artifactory.rest.common.model.RestModel;

/**
 * @author Chen Keinan
 */
public class CommonFile implements RestModel, FileModel {

    private String file;

    public CommonFile(String file) {
        this.file = file;
    }

    public String toString() {
        return file.toString();
    }

    @Override
    public Object getFileResource() {
        return file;
    }
}
