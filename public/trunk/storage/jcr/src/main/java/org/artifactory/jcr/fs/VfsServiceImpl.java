package org.artifactory.jcr.fs;

import org.artifactory.api.config.ImportSettingsImpl;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrServiceImpl;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.data.InternalVirtualFsDataService;
import org.artifactory.jcr.search.VfsQueryJcrImpl;
import org.artifactory.jcr.search.VfsRepoQueryJcrImpl;
import org.artifactory.jcr.utils.JcrHelper;
import org.artifactory.sapi.common.ArtifactorySession;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.data.VfsDataService;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.fs.MetadataReader;
import org.artifactory.sapi.search.VfsQuery;
import org.artifactory.sapi.search.VfsRepoQuery;
import org.artifactory.spring.Reloadable;
import org.artifactory.update.md.MetadataVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 * Date: 8/5/11
 * Time: 11:38 AM
 *
 * @author Fred Simon
 */
@Service
@Reloadable(beanClass = InternalVfsService.class, initAfter = {InternalVirtualFsDataService.class})
public class VfsServiceImpl implements InternalVfsService {
    @Autowired
    private JcrService jcrService;

    @Autowired
    private VfsDataService vfsDataService;

    @Override
    public void init() {
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    @Override
    public boolean nodeExists(String absolutePath) {
        return JcrHelper.itemNodeExists(absolutePath, jcrService.getManagedSession());
    }

    @Override
    public boolean delete(String absolutePath) {
        return jcrService.delete(absolutePath) > 0;
    }

    @Override
    public InputStream getStream(String absolutePath) {
        if (nodeExists(absolutePath)) {
            VfsNode node = vfsDataService.findByPath(absolutePath);
            if (node != null && node.hasContent()) {
                return node.content().getStream();
            }
        }
        return null;
    }

    @Override
    public String getContentAsString(String absolutePath) {
        if (nodeExists(absolutePath)) {
            VfsNode node = vfsDataService.findByPath(absolutePath);
            if (node != null && node.hasContent()) {
                return node.content().getContentAsString();
            }
        }
        return null;
    }


    @Override
    public MetadataReader fillBestMatchMetadataReader(ImportSettings importSettings, File metadataFolder) {
        ImportSettingsImpl settings = (ImportSettingsImpl) importSettings;
        MetadataReader metadataReader = settings.getMetadataReader();
        if (metadataReader == null) {
            if (settings.getExportVersion() != null) {
                metadataReader = MetadataVersion.findVersion(settings.getExportVersion());
            } else {
                //try to find the version from the format of the metadata folder
                metadataReader = MetadataVersion.findVersion(metadataFolder);
            }
            settings.setMetadataReader(metadataReader);
        }
        return metadataReader;
    }

    @Override
    public void createIfNeeded(String absolutePath) {
        vfsDataService.getOrCreate(absolutePath);
    }

    @Override
    public ArtifactorySession getUnmanagedSession() {
        JcrSession unmanagedSession = jcrService.getUnmanagedSession();
        JcrServiceImpl.keepUnmanagedSession(unmanagedSession);
        unmanagedSession.addLogoutListener(new Callable() {
            @Override
            public Object call() throws Exception {
                JcrServiceImpl.removeUnmanagedSession();
                return null;
            }
        });
        return unmanagedSession;
    }

    @Override
    public VfsQuery createQuery() {
        return new VfsQueryJcrImpl();
    }

    @Override
    public VfsRepoQuery createRepoQuery() {
        return new VfsRepoQueryJcrImpl();
    }
}
