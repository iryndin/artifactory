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

package org.artifactory.rest.common.service;

import org.artifactory.api.jackson.JacksonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

/**
 * Created by michaelp on 9/30/15.
 */
public abstract class AbstractResearchService {
    /**
     * Creates a JSON parser for the given bytes to parse
     *
     * @param bytesToParse Byte[] to parse
     * @return JSON Parser
     * @throws java.io.IOException
     */
    protected JsonParser getJsonParser(byte[] bytesToParse) throws IOException {
        return JacksonFactory.createJsonParser(bytesToParse);
    }

    /**
     * Creates a Jackson object mapper
     *
     * @return Object mapper
     */
    protected ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
