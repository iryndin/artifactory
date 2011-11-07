/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.jcr.version.v228;

import org.artifactory.io.checksum.ChecksumPathsImpl;
import org.artifactory.version.converter.ConfigurationConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

/**
 * @author Yoav Landman
 */
public class DeleteForConsistencyFixConverter implements ConfigurationConverter<Session> {
    private static final Logger log = LoggerFactory.getLogger(DeleteForConsistencyFixConverter.class);

    @Override
    public void convert(Session session) {
        log.info("Creating the no-consistency-fix marker file...");
        if (ChecksumPathsImpl.createConsistencyFixFile()) {
            log.info("The no-consistency-fix marker file has been created.");
        }
    }
}