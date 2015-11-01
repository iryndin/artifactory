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

package org.artifactory.api.jackson;

import java.io.IOException;

/**
 * Created by michaelp on 10/14/15.
 */
public final class JacksonWriter {

    /**
     * Serializes given item to json
     *
     * @param item an object to serialize
     *
     * @return serialized json string
     *
     * @throws java.io.IOException
     */
    public static <T> String serialize(T item) throws IOException {
        return JacksonFactory.createObjectMapper().writeValueAsString(item);
    }
}
