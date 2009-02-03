package org.artifactory.rest.common;

import com.sun.jersey.impl.provider.entity.AbstractMessageReaderWriterProvider;
import org.artifactory.config.jaxb.JaxbHelper;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.annotation.XmlType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * A jersey provider to handle Jaxb objects
 *
 * @author Noam Tenne
 */
@Produces({"application/xml"})
@Consumes({"application/xml"})
@Provider
public class JaxbProvider extends AbstractMessageReaderWriterProvider<Object> {

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return type.getAnnotation(XmlType.class) != null;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return type.getAnnotation(XmlType.class) != null;
    }

    public Object readFrom(Class<Object> aClass, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> map, InputStream stream)
            throws IOException, WebApplicationException {
        JaxbHelper jaxbHelper = new JaxbHelper();
        return jaxbHelper.read(stream, aClass, null);
    }

    @SuppressWarnings({"unchecked"})
    public void writeTo(Object o, Class<?> aClass, Type type, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> map, OutputStream stream)
            throws IOException, WebApplicationException {
        JaxbHelper jaxbHelper = new JaxbHelper();
        jaxbHelper.write(stream, o);
    }
}
