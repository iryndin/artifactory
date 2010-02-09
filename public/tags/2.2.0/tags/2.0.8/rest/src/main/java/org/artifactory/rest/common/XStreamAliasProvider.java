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
package org.artifactory.rest.common;

import com.sun.jersey.impl.provider.entity.AbstractMessageReaderWriterProvider;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * User: freds Date: Aug 12, 2008 Time: 6:46:36 PM
 */
@Produces({"application/xml", "text/xml", "*/*"})
@Consumes({"application/xml", "text/xml", "*/*"})
@Provider
public class XStreamAliasProvider extends AbstractMessageReaderWriterProvider<Object> {
    private static final Set<Class> processed = new HashSet<Class>();
    private static final XStream xstream = new XStream();
    private static final String DEFAULT_ENCODING = "utf-8";

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return type.getAnnotation(XStreamAlias.class) != null;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return type.getAnnotation(XStreamAlias.class) != null;        
    }

    protected static String getCharsetAsString(MediaType m) {
        if (m == null) {
            return DEFAULT_ENCODING;
        }
        String result = m.getParameters().get("charset");
        return (result == null) ? DEFAULT_ENCODING : result;
    }

    protected XStream getXStream(Class type) {
        synchronized (processed) {
            if (!processed.contains(type)) {
                xstream.processAnnotations(type);
                processed.add(type);
            }
        }
        return xstream;
    }

    public Object readFrom(Class<Object> aClass, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> map, InputStream stream)
            throws IOException, WebApplicationException {
        String encoding = getCharsetAsString(mediaType);
        XStream xStream = getXStream(aClass);
        return xStream.fromXML(new InputStreamReader(stream, encoding));
    }

    public void writeTo(Object o, Class<?> aClass, Type type, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> map, OutputStream stream)
            throws IOException, WebApplicationException {
        String encoding = getCharsetAsString(mediaType);
        XStream xStream = getXStream(o.getClass());
        xStream.toXML(o, new OutputStreamWriter(stream, encoding));
    }
}
