/*
 * This file is part of Artifactory.
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

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * Jackson generator factory class
 *
 * @author Noam Tenne
 */
public abstract class JacksonFactory {

    /**
     * Creates a JsonGenerator using the given output stream as a writer
     *
     * @param outputStream Stream to write to
     * @return Json Generator
     * @throws IOException
     */
    public static JsonGenerator create(OutputStream outputStream) throws IOException {
        JsonFactory jsonFactory = getFactory();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(outputStream, JsonEncoding.UTF8);
        updateGenerator(jsonFactory, jsonGenerator);
        return jsonGenerator;
    }

    /**
     * Creates a JsonGenerator using the given writer
     *
     * @param writer Stream to write to
     * @return Json Generator
     * @throws IOException
     */
    public static JsonGenerator create(Writer writer) throws IOException {
        JsonFactory jsonFactory = getFactory();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
        updateGenerator(jsonFactory, jsonGenerator);
        return jsonGenerator;
    }

    private static JsonFactory getFactory() {
        JsonFactory jsonFactory = new JsonFactory();
        //Do not auto-close target output when writing completes
        jsonFactory.disableGeneratorFeature(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        return jsonFactory;
    }

    private static void updateGenerator(JsonFactory jsonFactory, JsonGenerator jsonGenerator) {
        jsonGenerator.setCodec(new ObjectMapper(jsonFactory));
        jsonGenerator.useDefaultPrettyPrinter();
    }
}
