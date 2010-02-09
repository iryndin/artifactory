package org.artifactory.api.fs;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
@XStreamAlias(FolderInfo.ROOT)
public class FolderInfo extends ItemInfo {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(FileInfo.class);

    public static final String ROOT = "artifactory.folder";

    public FolderInfo() {
    }

    public FolderInfo(FolderInfo info) {
        super(info);
    }

    @Override
    public void update(ItemInfo info) {
        super.update(info);
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public String getRootName() {
        return ROOT;
    }

    public String toString() {
        return "FolderInfo{ super=" + super.toString() + "}";
    }
}