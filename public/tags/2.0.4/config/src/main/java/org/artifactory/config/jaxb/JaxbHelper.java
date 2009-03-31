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
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.artifactory.util.FileUtils;
import org.artifactory.version.ArtifactoryConfigVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JaxbHelper<T> {
    private static final Logger log = LoggerFactory.getLogger(JaxbHelper.class);
    private static final String UTF_8 = "utf-8";

    public static CentralConfigDescriptorImpl readConfig(File configFile) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(configFile);
        } catch (FileNotFoundException e) {
            String msg = "Configuration file path location '"
                    + configFile + "' does not exists.";
            log.error(msg);
            throw new RuntimeException(msg, e);
        }
        //Check that the namespace is the current valid one
        byte[] bytes;
        String configXmlString;
        try {
            bytes = IOUtils.toByteArray(fis);
            configXmlString = new String(bytes, UTF_8);
        } catch (IOException e) {
            String msg = "Could not read data from configuration file path location '"
                    + configFile + "'.";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            IOUtils.closeQuietly(fis);
        }
        if (!configXmlString.contains(Descriptor.NS)) {
            String configFileAbsPath = configFile.getAbsolutePath();
            String msg = "The current Artifactory config schema namespace is '" + Descriptor.NS +
                    "' \nThe provided config file '" + configFileAbsPath +
                    "' does not seem to be compliant with it.\n";
            if (log.isDebugEnabled()) {
                log.debug(msg + "\n" + configXmlString);
            }

            ArtifactoryConfigVersion configVersion =
                    ArtifactoryConfigVersion.getConfigVersion(configXmlString);
            if (configVersion == null) {
                throw new RuntimeException(msg +
                        "The Auto discovery of Artifactory configuration version " +
                        "did not find any valid version for this config file and so cannot " +
                        "convert it automaticaly.\n" +
                        "Please fix this file manually!");
            } else if (configVersion == ArtifactoryConfigVersion.getCurrent()) {
                throw new RuntimeException(msg +
                        "The Auto discovery of Artifactory configuration version " +
                        "found that this config file is up to date but does point to the rigth schema.\n" +
                        "Please fix this file manually!");
            } else {
                String newConfigXml = configVersion.convert(configXmlString);
                // Auto convert and save if write permission to the etc folder
                File parentFile = configFile.getParentFile();
                if (parentFile.canWrite()) {
                    try {
                        log.info(msg +
                                "Automatically converting the config file, original will be saved in " +
                                parentFile.getAbsolutePath());
                        File newConfigFile =
                                new File(parentFile,
                                        "new_" + ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
                        FileOutputStream fos = new FileOutputStream(newConfigFile);
                        IOUtils.write(newConfigXml, fos);
                        fos.close();
                        FileUtils.switchFiles(configFile, newConfigFile);
                    } catch (Exception e) {
                        log.error(msg +
                                "The converted config file for '" + configFileAbsPath + "' is:\n" +
                                newConfigXml + "\nBut it failed to be saved automatically to '" +
                                parentFile.getAbsolutePath() +
                                "' due to :" + e.getMessage() + ".\n" +
                                getPleaseCopyMessage(configFileAbsPath), e);
                    }
                } else {
                    log.error(msg +
                            "The converted config file for '" + configFileAbsPath + "' is:\n" +
                            newConfigXml + "\nBut it cannot be saved automatically to '" +
                            parentFile.getAbsolutePath() +
                            "' since the folder is not writable.\n" +
                            getPleaseCopyMessage(configFileAbsPath));
                }
                try {
                    bytes = newConfigXml.getBytes(UTF_8);
                } catch (UnsupportedEncodingException e) {
                    log.error(msg, e);
                    throw new RuntimeException(msg, e);
                }
            }
        }
        URL schemaUrl = CentralConfigDescriptorImpl.class
                .getClassLoader().getResource("artifactory.xsd");
        if (schemaUrl == null) {
            throw new RuntimeException("Cannot load artifactory.xsd schema file from classpath.\n" +
                    "Please make sure the artifactory.war is full.");
        }
        CentralConfigDescriptorImpl descriptor =
                new JaxbHelper<CentralConfigDescriptorImpl>()
                        .read(new ByteArrayInputStream(bytes),
                                CentralConfigDescriptorImpl.class,
                                schemaUrl);
        return descriptor;
    }

    private static String getPleaseCopyMessage(String configFileAbsPath) {
        String pleaseCopyMsg = "Please copy the new config xml definition above to '" +
                configFileAbsPath + "'!\n" +
                "An in memory version of this configuration will be used!";
        return pleaseCopyMsg;
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
            log.error("Could not load configuration from path location '"
                    + path + "'.");
            throw new RuntimeException("Failed to read object from file '" + path + "'.", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    public T read(InputStream stream, Class clazz, URL schemaUrl) {
        T o = null;
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            if (schemaUrl != null) {
                SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = sf.newSchema(schemaUrl);
                unmarshaller.setSchema(schema);
            }
            o = (T) unmarshaller.unmarshal(stream);
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
                    return result;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to write object to stream.", e);
        }
    }
}
