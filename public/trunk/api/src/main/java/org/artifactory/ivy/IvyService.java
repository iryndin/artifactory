package org.artifactory.ivy;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepoPath;

import java.io.File;

/**
 * @author Yoav Landman
 */
public interface IvyService {
    @Lock(transactional = true, readOnly = true)
    ModuleDescriptor parseIvyFile(RepoPath repoPath);

    ModuleDescriptor parseIvyFile(File file);
}
