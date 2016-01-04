/*
* Artifactory is a binaries repository manager.
* Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.storage;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.storage.RepoStorageSummaryInfo;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.util.NumberFormatter;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class represents StorageSummary
 *
 * @author Michael Pasternak
 */
@XmlRootElement(name = "storage")
public class StorageSummaryImpl implements StorageSummary {

    private BinariesSummary binariesSummary;
    private FileStoreSummary fileStoreSummary;
    private List<RepositorySummary> repositoriesSummaryList;

    /**
     * serialization .ctr
     */
    public StorageSummaryImpl() {
    }

    /**
     * Creates {@link StorageSummaryImpl} from {@link StorageSummaryInfo}
     *
     * @param storageSummaryInfo
     */
    public StorageSummaryImpl(StorageSummaryInfo storageSummaryInfo) {
        // update binaries summary
        updateBinariesSummary(storageSummaryInfo, this);
        // update file store summary
        updateFileStoreSummary(this);
        // update repositorySummary
        updateRepositorySummary(storageSummaryInfo, this);
    }

    public BinariesSummary getBinariesSummary() {
        return binariesSummary;
    }

    public void setBinariesSummary(BinariesSummary binariesSummary) {
        this.binariesSummary = binariesSummary;
    }

    public FileStoreSummary getFileStoreSummary() {
        return fileStoreSummary;
    }

    public void setFileStoreSummary(FileStoreSummary fileStoreSummary) {
        this.fileStoreSummary = fileStoreSummary;
    }

    public List<RepositorySummary> getRepositoriesSummaryList() {
        return repositoriesSummaryList;
    }

    public void setRepositoriesSummaryList(List<RepositorySummary> repositoriesSummaryList) {
        this.repositoriesSummaryList = repositoriesSummaryList;
    }

    /**
     * update repository summary
     *
     * @param storageSummaryInfo - summary info
     */
    private void updateRepositorySummary(StorageSummaryInfo storageSummaryInfo, StorageSummaryImpl storageSummaryModel) {
        //populate repository info to list of models
        List<RepositorySummary> repositorySummaryList = new ArrayList<>();
        Set<RepoStorageSummaryInfo> repoStorageSummaries = storageSummaryInfo.getRepoStorageSummaries();
        repoStorageSummaries.forEach(repoStorageSummary -> repositorySummaryList.add(
                new RepositorySummary(repoStorageSummary, storageSummaryInfo.getTotalSize())));
        // update total data
        updateTotalRepositoryData(storageSummaryInfo, repositorySummaryList);
        storageSummaryModel.setRepositoriesSummaryList(repositorySummaryList);
    }

    /**
     * update total repository data
     *
     * @param storageSummaryInfo    - storage info data
     * @param repositorySummaryList - list of storage repository models
     */
    private void updateTotalRepositoryData(StorageSummaryInfo storageSummaryInfo,
            List<RepositorySummary> repositorySummaryList) {
        RepositorySummary repositorySummary = new RepositorySummary();
        repositorySummary.setRepoKey("TOTAL");
        repositorySummary.setRepoType(RepoStorageSummaryInfo.RepositoryType.NA);
        repositorySummary.setFoldersCount(storageSummaryInfo.getTotalFolders());
        repositorySummary.setFilesCount(storageSummaryInfo.getTotalFiles());
        repositorySummary.setUsedSpace(StorageUnit.toReadableString(storageSummaryInfo.getTotalSize()));
        repositorySummary.setItemsCount(storageSummaryInfo.getTotalItems());
        repositorySummaryList.add(repositorySummary);
    }

    /**
     * update storage summary with binaries repositories
     *
     * @param storageSummaryModel - storageSummary Model
     */
    private void updateFileStoreSummary(StorageSummaryImpl storageSummaryModel) {

        StorageService storageService = ContextHelper.get().beanForType(StorageService.class);

        FileStoreStorageSummary fileStoreSummaryInfo = storageService.getFileStoreStorageSummary();
        FileStoreSummary fileStoreSummary = new FileStoreSummary();
        fileStoreSummary.setStorageType(fileStoreSummaryInfo.getBinariesStorageType().toString());
        List<File> binariesFolders = fileStoreSummaryInfo.getBinariesFolders();
        String storageDirLabel = "Filesystem storage is not used";
        if (binariesFolders != null && !binariesFolders.isEmpty()) {
            storageDirLabel = String.join(", ",
                    binariesFolders.stream().map(File::getAbsolutePath).collect(Collectors.toList()));
        }
        fileStoreSummary.setStorageDirectory(storageDirLabel);
        fileStoreSummary.setTotalSpace(StorageUnit.toReadableString(fileStoreSummaryInfo.getTotalSpace()));
        fileStoreSummary.setUsedSpace(
                StorageUnit.toReadableString(fileStoreSummaryInfo.getUsedSpace()) + " (" +
                        NumberFormatter.formatPercentage(fileStoreSummaryInfo.getUsedSpaceFraction()) + ")");
        fileStoreSummary.setFreeSpace(
                StorageUnit.toReadableString(fileStoreSummaryInfo.getFreeSpace()) + " (" +
                        NumberFormatter.formatPercentage(fileStoreSummaryInfo.getFreeSpaceFraction()) + ")");
        storageSummaryModel.setFileStoreSummary(fileStoreSummary);
    }

    /**
     * update storage summary with binaries repositories
     *
     * @param storageSummaryInfo - storage info
     * @param storageSummaryModel     - storageSummary Model
     */
    private void updateBinariesSummary(StorageSummaryInfo storageSummaryInfo, StorageSummaryImpl storageSummaryModel) {
        BinariesSummary binariesSummary = new BinariesSummary();
        binariesSummary.setBinariesCount(NumberFormatter.formatLong(
                storageSummaryInfo.getBinariesInfo().getBinariesCount()));

        binariesSummary.setBinariesSize(StorageUnit.toReadableString(
                storageSummaryInfo.getBinariesInfo().getBinariesSize()));

        binariesSummary.setOptimization(NumberFormatter.formatPercentage(storageSummaryInfo.getOptimization()));
        binariesSummary.setArtifactsSize(StorageUnit.toReadableString(storageSummaryInfo.getTotalSize()));
        binariesSummary.setItemsCount(NumberFormatter.formatLong((storageSummaryInfo.getTotalItems())));
        binariesSummary.setArtifactsCount(NumberFormatter.formatLong((storageSummaryInfo.getTotalFiles())));

        storageSummaryModel.setBinariesSummary(binariesSummary);
    }
}
