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
 *
 * Based on: /org/apache/jackrabbit/jackrabbit-core/1.6.0/jackrabbit-core-1.6.0.jar!
 * /org/apache/jackrabbit/core/data/DataStoreException.class
 */

package org.artifactory.jcr.jackrabbit;

import org.apache.jackrabbit.core.data.DataStoreException;

/**
 * @author freds
 * @date Mar 30, 2009
 */
public class MissingOrInvalidDataStoreRecordException extends DataStoreException {
    public MissingOrInvalidDataStoreRecordException(String message) {
        super(message);
    }

    public MissingOrInvalidDataStoreRecordException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingOrInvalidDataStoreRecordException(Throwable rootCause) {
        super(rootCause);
    }
}
