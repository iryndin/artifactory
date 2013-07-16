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

package org.artifactory.storage.db.util.blob;

import com.gc.iotools.stream.is.InputStreamFromOutputStream;
import org.artifactory.api.jackson.JacksonFactory;
import org.codehaus.jackson.JsonGenerator;

import java.io.OutputStream;

/**
 * A blob wrapper that lazily creates an input stream out of an Object using JSON serialization.
 *
 * @author freds
 */
public class JsonBlobWrapper extends BlobWrapper {
    JsonBlobWrapper(final Object jsonObject) {
        // TODO: Make sure the Global Artifactory executor is used
        super(new InputStreamFromOutputStream() {
            @Override
            protected Object produce(OutputStream outputStream) throws Exception {
                JsonGenerator jsonGenerator = JacksonFactory.createJsonGenerator(outputStream);
                jsonGenerator.writeObject(jsonObject);
                return null;
            }
        });
    }
}
