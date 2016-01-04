package org.artifactory.rest.common.model.storagesummary;

import org.artifactory.rest.common.model.BaseModel;
import org.artifactory.storage.BinariesSummary;
import org.artifactory.storage.FileStoreSummary;
import org.artifactory.storage.RepositorySummary;
import org.artifactory.storage.StorageSummary;
import org.artifactory.storage.StorageSummaryImpl;
import org.artifactory.storage.StorageSummaryInfo;

import java.util.List;

/**
 * @author Chen Keinan
 */
public class StorageSummaryModel extends BaseModel implements StorageSummary {

    StorageSummaryImpl storageSummary;

    /**
     * serialization .ctr
     */
    public StorageSummaryModel() {
    }

    public StorageSummaryModel(StorageSummaryInfo storageSummaryInfo) {
        storageSummary = new StorageSummaryImpl(storageSummaryInfo);
    }

    @Override
    public BinariesSummary getBinariesSummary() {
        return storageSummary.getBinariesSummary();
    }

    @Override
    public void setBinariesSummary(BinariesSummary binariesSummary) {
        storageSummary.setBinariesSummary(binariesSummary);
    }

    @Override
    public FileStoreSummary getFileStoreSummary() {
        return storageSummary.getFileStoreSummary();
    }

    @Override
    public void setFileStoreSummary(FileStoreSummary fileStoreSummary) {
        storageSummary.setFileStoreSummary(fileStoreSummary);
    }

    @Override
    public List<RepositorySummary> getRepositoriesSummaryList() {
        return storageSummary.getRepositoriesSummaryList();
    }

    @Override
    public void setRepositoriesSummaryList(List<RepositorySummary> repositoriesSummaryList) {
        storageSummary.setRepositoriesSummaryList(repositoriesSummaryList);
    }

    public StorageSummaryImpl getStorageSummary() {
        return storageSummary;
    }
}
