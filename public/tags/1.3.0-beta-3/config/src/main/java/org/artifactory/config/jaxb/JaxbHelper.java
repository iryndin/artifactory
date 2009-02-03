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

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptor;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
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

    public static CentralConfigDescriptor readConfig(File configFile) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(configFile);
        } catch (FileNotFoundException e) {
            String msg = "Configuration file path location '"
                    + configFile + "' does not exists.";
            LOGGER.error(msg);
            throw new RuntimeException(msg, e);
        }
        //Check that the namespace is the current valid one
        byte[] bytes;
        String configXmlString;
        try {
            bytes = IOUtils.toByteArray(fis);
            configXmlString = new String(bytes, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
            String msg = "Could not read data from configuration file path location '"
                    + configFile + "'.";
            LOGGER.error(msg);
            throw new RuntimeException(msg, e);
        }
        if (!configXmlString.contains(Descriptor.NS)) {
            throw new RuntimeException(
                    "The current Artifactory config schema namespace is '" + Descriptor.NS +
                            "' \n" +
                            "The provided config file does not seem to be compliant with it.\n" +
                            "Please run artifactoryExport.sh from the artifactory-upgrade package" +
                            " if you are using an old version of Artifactory home.");
        }
        URL schemaUrl = CentralConfigDescriptor.class
                .getClassLoader().getResource("artifactory.xsd");
        if (schemaUrl == null) {
            throw new RuntimeException("Cannot load artifactory.xsd schema file from classpath.\n" +
                    "Please make sure the artifactory.war is full.");
        }
        CentralConfigDescriptor descriptor =
                new JaxbHelper<CentralConfigDescriptor>()
                        .read(new ByteArrayInputStream(bytes),
                                CentralConfigDescriptor.class,
                                schemaUrl);
        return descriptor;
    }

    public static void writeConfig(CentralConfigDescriptor config, File newConfFile) {
        new JaxbHelper<CentralConfigDescriptor>().write(newConfFile.getAbsolutePath(), config);
    }

    public void write(String path, T object) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(path);
            write(fos, object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write object to file '" + path + "'.", e);
        }
    }

    public void write(OutputStream stream, T object) {
        try {
            JAXBContext context = JAXBContext.newInstance(object.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(object, stream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write object to stream.", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public T read(File path, Class clazz, URL schemaUrl) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(path);
            return read(fis, clazz, schemaUrl);
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not load configuration from path location '"
                    + path + "'.");
            throw new RuntimeException("Failed to read object from file '" + path + "'.", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    public T read(InputStream stream, Class clazz, URL schemaUrl) {
        T o;
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(schemaUrl);
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
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return o;
    }

    public void generateSchema(
            final OutputStream stream, final Class<T> clazz, final String namespace) {
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            context.generateSchema(new SchemaOutputResolver() {
                @Override
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
