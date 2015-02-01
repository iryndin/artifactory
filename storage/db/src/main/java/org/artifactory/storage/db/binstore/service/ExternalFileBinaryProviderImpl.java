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

package org.artifactory.storage.db.binstore.service;

import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.binstore.BinaryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A binary provider that provide streams from an external filestore.
 * Will throw exception on all insert and prune methods.
 *
 * @author Fred Simon
 */
class ExternalFileBinaryProviderImpl extends FileBinaryProviderReadOnlyBase {
    private static final Logger log = LoggerFactory.getLogger(ExternalFileBinaryProviderImpl.class);

    public ExternalFileBinaryProviderImpl(File externalFilestoreDir) {
        super(externalFilestoreDir);
    }

    @Override
    @Nonnull
    public BinaryInfo addStream(InputStream in) throws IOException {
        // No add to an external binary provider
        return next().addStream(in);
    }

    @Override
    public boolean delete(String sha1) {
        // No deletion of an external binary provider
        return next().delete(sha1);
    }

    @Override
    public void prune(BasicStatusHolder statusHolder) {
        throw new UnsupportedOperationException("An external binary provider cannot be pruned!");
    }
}
