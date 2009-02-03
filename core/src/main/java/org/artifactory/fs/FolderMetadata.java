package org.artifactory.fs;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
@XStreamAlias("folder")
public class FolderMetadata extends FsItemMetadata {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(FileMetadata.class);

    public FolderMetadata() {
    }

    public FolderMetadata(String repoKey, String relPath, long created, String modifiedBy) {
        super(repoKey, relPath, created, modifiedBy);
    }
}