/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.jcr.md;

import org.artifactory.cache.InternalCacheService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrServiceImpl;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.jcr.JcrHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jcr.Node;

import static org.artifactory.jcr.JcrTypes.NODE_ARTIFACTORY_METADATA;

/**
 * TODO: This class should be removed and all code move to JcrService and PersistenceHandler User: freds Date: Aug 10,
 * 2008 Time: 3:27:38 PM
 */
@Service
@Reloadable(beanClass = MetadataService.class,
        initAfter = {MetadataDefinitionService.class, InternalCacheService.class})
public class MetadataServiceImpl implements InternalMetadataService {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(MetadataServiceImpl.class);

    @Autowired
    private JcrService jcr;

    public void init() {
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{MetadataDefinitionService.class, InternalCacheService.class};
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public void destroy() {
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    public void saveChecksums(JcrFsItem fsItem, String metadataName, Checksum[] checksums) {
        // TODO: Maybe write lock the fsItem?
        Node metadataNode = JcrHelper.safeGetNode(fsItem.getNode(), NODE_ARTIFACTORY_METADATA, metadataName);
        if (metadataNode != null) {
            JcrServiceImpl.setChecksums(metadataNode, checksums, false);
        }
    }
}
