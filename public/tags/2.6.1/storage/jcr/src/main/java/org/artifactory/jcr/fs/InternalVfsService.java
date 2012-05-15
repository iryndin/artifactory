package org.artifactory.jcr.fs;

import org.artifactory.sapi.fs.VfsService;
import org.artifactory.sapi.search.VfsQueryService;
import org.artifactory.spring.ReloadableBean;

/**
 * Date: 8/5/11
 * Time: 11:39 AM
 *
 * @author Fred Simon
 */
public interface InternalVfsService extends VfsService, VfsQueryService, ReloadableBean {
}
