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
package org.artifactory.config.jaxb;

import org.apache.log4j.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JaxbHelper<T> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JaxbHelper.class);

    public void write(String path, T object) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write object to file '" + path + "'.", e);
        }
        write(fos, object);
    }

    public void write(OutputStream stream, T object) {
        try {
            JAXBContext context = JAXBContext.newInstance(object.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(object, stream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write object to stream.", e);
        }
    }

    public T read(String path, Class clazz) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to read object from file '" + path + "'.", e);
        }
        return read(fis, clazz);
    }

    @SuppressWarnings({"unchecked"})
    public T read(InputStream stream, Class clazz) {
        T o;
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            URL xsdUrl = clazz.getClassLoader().getResource("artifactory.xsd");
            Schema schema = sf.newSchema(xsdUrl);
            unmarshaller.setSchema(schema);
            o = (T) unmarshaller.unmarshal(stream);
            /*
            //Should clean whitespaces automatically, but doesn't work:
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setSchema(schema);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(stream);
            db.setErrorHandler(new XMLErrorHandler());
            o = (T) unmarshaller.unmarshal(doc);
            */
        } catch (Throwable t) {
            throw new RuntimeException("Failed to read object from stream.", t);
        }
        return o;
    }

    public void generateSchema(
            final OutputStream stream, final Class<T> clazz, final String namespace) {
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            context.generateSchema(new SchemaOutputResolver() {
                public Result createOutput(String namespaceUri, String suggestedFileName)
                        throws IOException {
                    StreamResult result = new StreamResult(stream);
                    result.setSystemId(namespace);
                    //result.setSystemId("http://artifactory.org/" + object.getClass().getName());
                    /*OutputFormat format = new OutputFormat();
                    XMLResult result = new XMLResult(stream, format);
                    format.setIndentSize(2);
                    format.setEncoding("utf-8");
                    format.setTrimText(false);
                    format.setNewlines(true);*/
                    return result;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to write object to stream.", e);
        }
    }
}
