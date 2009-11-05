package org.artifactory.jcr.jackrabbit;

import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;

import java.io.File;

/**
 * @author freds
 */
public class ArtifactoryFileDataStoreImpl extends ArtifactoryBaseDataStore {
    private String fileStoreDir;

    private File binariesFolder;

    @Override
    public File getOrCreateFile(DataIdentifier identifier, long expectedLength) throws DataStoreException {
        File result = getFile(identifier);
        if (result.exists() && result.length() == expectedLength) {
            return result;
        }

        throw new DataStoreRecordNotFoundException(
                "Record not found: " + identifier + " as file " + result.getAbsolutePath() +
                        " does not exists or has the wrong length!");
    }

    @Override
    protected boolean validState(ArtifactoryDbDataRecord record) {
        File file = getFile(record.getIdentifier());
        return file.exists() && file.length() == record.length;
    }

    @Override
    public File getBinariesFolder() {
        return binariesFolder;
    }

    @Override
    protected boolean saveBinariesAsBlobs() {
        return false;
    }

    public String getFileStoreDir() {
        return fileStoreDir;
    }

    public void setFileStoreDir(String fileStoreDir) {
        this.fileStoreDir = fileStoreDir;
    }

    @Override
    public void init(String homeDir) throws DataStoreException {
        initRootStoreDir(homeDir);
        super.init(homeDir);
    }

    private void initRootStoreDir(String homeDir) throws DataStoreException {
        String fsDir = getFileStoreDir();
        if (fsDir != null && fsDir.length() > 0) {
            binariesFolder = new File(fsDir);
        } else {
            binariesFolder = new File(homeDir, "filestore");
        }
        if (!binariesFolder.exists()) {
            if (!binariesFolder.mkdirs()) {
                throw new DataStoreException("Could not create file store folder " + binariesFolder.getAbsolutePath());
            }
        }
    }
}
