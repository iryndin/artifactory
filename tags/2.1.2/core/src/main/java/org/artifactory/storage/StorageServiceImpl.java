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

package org.artifactory.storage;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.storage.GarbageCollectorInfo;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.jackrabbit.GenericDbDataStore;
import org.artifactory.jcr.schedule.JcrGarbageCollectorJob;
import org.artifactory.jcr.utils.DerbyUtils;
import org.artifactory.jcr.utils.JcrUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.index.IndexerJob;
import org.artifactory.schedule.TaskService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.storage.mbean.Storage;
import org.artifactory.storage.mbean.StorageMBean;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * @author yoavl
 */
@Service
public class StorageServiceImpl implements InternalStorageService {
    private static final Logger log = LoggerFactory.getLogger(StorageServiceImpl.class);

    @Autowired
    private JcrService jcrService;

    @Autowired
    private TaskService taskService;

    private boolean derbyUsed;

    public void compress(MultiStatusHolder statusHolder) {
        if (!derbyUsed) {
            statusHolder.setError("Compress command is not supported on current database type.", log);
            return;
        }

        logStorageSizes();
        DerbyUtils.compress(statusHolder);
        logStorageSizes();
    }

    public void logStorageSizes() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n-----Storage sizes (in bytes)-----\n");
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File dataDir = artifactoryHome.getDataDir();
        File[] dirs = {new File(dataDir, "db"), new File(dataDir, "store"), new File(dataDir, "index")};
        for (File dir : dirs) {
            if (dir.exists()) {
                sb.append(dir.getName()).append("=").append(FileUtils.sizeOfDirectory(dir)).append("\n");
            }
        }
        sb.append("datastore table=").append(getStorageSize()).append("\n");
        sb.append("-----------------------");
        log.info(sb.toString());
    }

    public long getStorageSize() {
        JcrSession session = jcrService.getUnmanagedSession();
        try {
            RepositoryImpl repository = (RepositoryImpl) session.getRepository();
            GenericDbDataStore dataStore = JcrUtils.getDataStore(repository);
            return dataStore.getStorageSize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate storage size.", e);
        } finally {
            session.logout();
        }
    }

    public long getLuceneIndexSize() {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File dataDir = artifactoryHome.getDataDir();
        File indexDir = new File(dataDir, "index");
        return FileUtils.sizeOfDirectory(indexDir);
    }

    public GarbageCollectorInfo manualGarbageCollect(MultiStatusHolder statusHolder) {
        taskService.stopTasks(IndexerJob.class, true);
        taskService.stopTasks(JcrGarbageCollectorJob.class, true);
        try {
            //GC in-use-records weak references used by the file datastore
            System.gc();
            return jcrService.garbageCollect();
        } catch (Exception e) {
            statusHolder.setError(e.getMessage(), log);
        } finally {
            taskService.resumeTasks(JcrGarbageCollectorJob.class);
            taskService.resumeTasks(IndexerJob.class);
        }
        return new GarbageCollectorInfo();  // null object
    }

    public boolean isDerbyUsed() {
        return derbyUsed;
    }

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addReloadableBean(InternalStorageService.class);
    }

    public void init() {
        derbyUsed = DerbyUtils.isDerbyUsed();
        InternalContextHelper.get().registerArtifactoryMBean(new Storage(this), StorageMBean.class, null);
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        derbyUsed = DerbyUtils.isDerbyUsed();
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{JcrService.class};
    }

    public void destroy() {
        //nop
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        //nop
    }

}
