/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.version;

import org.artifactory.version.converter.XmlConverter;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.List;

/**
 * @author freds
 * @date Nov 9, 2008
 */
public class ConverterUtils {
    private static final Logger log = LoggerFactory.getLogger(ConverterUtils.class);

    public static String convert(List<XmlConverter> converters, String in) {
        // If no converters nothing to do
        if (converters.isEmpty()) {
            return in;
        }
        Document doc = parse(in);
        for (XmlConverter converter : converters) {
            log.debug("Applying converter " + converter + " on " + doc.getRootElement());
            converter.convert(doc);
        }
        return outputString(doc);
    }

    public static String outputString(Document doc) {
        String convertedXml = new XMLOutputter().outputString(doc);
        return convertedXml;
    }

    public static Document parse(String xmlContent) {
        SAXBuilder sb = new SAXBuilder();
        Document doc;
        try {
            doc = sb.build(new StringReader(xmlContent));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build dom document", e);
        }
        return doc;
    }
}
