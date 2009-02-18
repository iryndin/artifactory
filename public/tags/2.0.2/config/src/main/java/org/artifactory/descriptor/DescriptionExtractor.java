package org.artifactory.descriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Iterator;

/**
 * Utility class that extracts help messages from the artifactory.xsd file given a Descriptor
 * and a property name.
 *
 * @author Yossi Shaul
 */
public class DescriptionExtractor {
    private final static Logger log = LoggerFactory.getLogger(DescriptionExtractor.class);

    protected Document doc;

    private static DescriptionExtractor instance;

    private DescriptionExtractor() {
        try {
            doc = loadArtifactoryXsd();
        } catch (Exception e) {
            throw new RuntimeException("Error reading schema", e);
        }
    }

    public static synchronized DescriptionExtractor getInstance() {
        if (instance == null) {
            instance = new DescriptionExtractor();
        }
        return instance;
    }

    /**
     * @param descriptor    The descriptor
     * @param propertyName  The property name.
     * @throws IllegalArgumentException if the property not found.
     * @return The description of the given property. If no description for the input property
     * empty string will be returned.
     */
    public String getDescription(Descriptor descriptor, String propertyName) {
        if (descriptor == null) {
            throw new IllegalArgumentException("Descriptor must not be null");
        }

        if (propertyName == null || "".equals(propertyName)) {
            throw new IllegalArgumentException("Property name must not be null or empty");
        }

        Field field = getField(descriptor, propertyName);

        String elementName = getElementName(propertyName, field);
        String complexTypeName = getComplexTypeName(field.getDeclaringClass());

        String query = buildXPathQuery(complexTypeName, elementName);
        log.debug("Executing xpath query: {}", query);
        String description = executeQuery(query);
        return description;
    }

    private Field getField(Descriptor descriptor, String propertyName) {
        Class<?> clazz = descriptor.getClass();
        while (clazz != Object.class) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (declaredField.getName().equals(propertyName)) {
                    return declaredField;
                }
            }
            clazz = clazz.getSuperclass();
        }

        throw new IllegalArgumentException("Property " + propertyName + " not found");
    }

    private String getElementName(String propertyName, Field field) {
        // default element name is the property name
        String elementName = propertyName;

        if (field.isAnnotationPresent(XmlElementWrapper.class)) {
            XmlElementWrapper wrapper = field.getAnnotation(XmlElementWrapper.class);
            // use the element name from the annotation only if it's not the default
            if (notDefaultName(wrapper.name())) {
                elementName = wrapper.name();
            }
        } else if (field.isAnnotationPresent(XmlElement.class)) {
            XmlElement annotation = field.getAnnotation(XmlElement.class);
            // use the element name from the annotation only if it's not the default
            if (notDefaultName(annotation.name())) {
                elementName = annotation.name();
            }
        }

        return elementName;
    }

    private boolean notDefaultName(String name) {
        return !"##default".equals(name);
    }

    protected String getComplexTypeName(Class<?> declaringClass) {
        XmlType xmlType = declaringClass.getAnnotation(XmlType.class);
        return xmlType.name();
    }


    private String buildXPathQuery(String xmlType, String elementName) {
        String xpath = "/xs:schema/xs:complexType[@name='" + xmlType + "']" +
                "//xs:element[@name='" + elementName + "']" +
                "/xs:annotation/xs:documentation/text()";
        return xpath;
    }

    private Document loadArtifactoryXsd() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream schemaIn = this.getClass().getResourceAsStream("/artifactory.xsd");
        return builder.parse(schemaIn);
    }

    private String executeQuery(String query) {
        try {
            XPathFactory xFactory = XPathFactory.newInstance();
            XPath xpath = xFactory.newXPath();
            xpath.setNamespaceContext(new SchemaNamespaceContext());
            XPathExpression expr = xpath.compile(query);
            Object description = expr.evaluate(doc, XPathConstants.STRING);
            return description.toString().trim();
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Failed to execute xpath query: " + query, e);
        }
    }

    public class SchemaNamespaceContext implements NamespaceContext {
        public String getNamespaceURI(String prefix) {
            if (prefix == null) {
                throw new NullPointerException("Null prefix");
            } else if ("xs".equals(prefix)) {
                return XMLConstants.W3C_XML_SCHEMA_NS_URI;
            } else if ("xml".equals(prefix)) {
                return XMLConstants.XML_NS_URI;
            }
            return XMLConstants.NULL_NS_URI;
        }

        // This method isn't necessary for XPath processing.
        public String getPrefix(String uri) {
            throw new UnsupportedOperationException();
        }

        // This method isn't necessary for XPath processing either.
        public Iterator getPrefixes(String uri) {
            throw new UnsupportedOperationException();
        }
    }
}
