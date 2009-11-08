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

package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.extractor.TextExtractor;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Creates a lucene <code>Document</code> object from a {@link javax.jcr.Node}.
 */
public class NodeIndexer {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(NodeIndexer.class);

    /**
     * The default boost for a lucene field: 1.0f.
     */
    protected static final float DEFAULT_BOOST = 1.0f;

    /**
     * The <code>NodeState</code> of the node to index
     */
    protected final NodeState node;

    /**
     * The persistent item state provider
     */
    protected final ItemStateManager stateProvider;

    /**
     * Namespace mappings to use for indexing. This is the internal namespace mapping.
     */
    protected final NamespaceMappings mappings;

    /**
     * Name and Path resolver.
     */
    protected final NamePathResolver resolver;

    /**
     * Content extractor.
     */
    protected final TextExtractor extractor;

    /**
     * The indexing configuration or <code>null</code> if none is available.
     */
    protected IndexingConfiguration indexingConfig;

    /**
     * If set to <code>true</code> the fulltext field is stored and and a term vector is created with offset
     * information.
     */
    protected boolean supportHighlighting = false;

    /**
     * Indicates index format for this node indexer.
     */
    protected IndexFormatVersion indexFormatVersion = IndexFormatVersion.V1;

    /**
     * List of {@link FieldNames#FULLTEXT} fields which should not be used in an excerpt.
     */
    protected List doNotUseInExcerpt = new ArrayList();

    /**
     * Creates a new node indexer.
     *
     * @param node          the node state to index.
     * @param stateProvider the persistent item state manager to retrieve properties.
     * @param mappings      internal namespace mappings.
     * @param extractor     content extractor
     */
    public NodeIndexer(NodeState node,
            ItemStateManager stateProvider,
            NamespaceMappings mappings,
            TextExtractor extractor) {
        this.node = node;
        this.stateProvider = stateProvider;
        this.mappings = mappings;
        this.resolver = NamePathResolverImpl.create(mappings);
        this.extractor = extractor;
    }

    /**
     * Returns the <code>NodeId</code> of the indexed node.
     *
     * @return the <code>NodeId</code> of the indexed node.
     */
    public NodeId getNodeId() {
        return node.getNodeId();
    }

    /**
     * If set to <code>true</code> additional information is stored in the index to support highlighting using the
     * rep:excerpt pseudo property.
     *
     * @param b <code>true</code> to enable highlighting support.
     */
    public void setSupportHighlighting(boolean b) {
        supportHighlighting = b;
    }

    /**
     * Sets the index format version
     *
     * @param indexFormatVersion the index format version
     */
    public void setIndexFormatVersion(IndexFormatVersion indexFormatVersion) {
        this.indexFormatVersion = indexFormatVersion;
    }

    /**
     * Sets the indexing configuration for this node indexer.
     *
     * @param config the indexing configuration.
     */
    public void setIndexingConfiguration(IndexingConfiguration config) {
        this.indexingConfig = config;
    }

    /**
     * Creates a lucene Document.
     *
     * @return the lucene Document with the index layout.
     * @throws RepositoryException if an error occurs while reading property values from the
     *                             <code>ItemStateProvider</code>.
     */
    protected Document createDoc() throws RepositoryException {
        doNotUseInExcerpt.clear();
        Document doc = new Document();

        doc.setBoost(getNodeBoost());

        // special fields
        // UUID
        doc.add(new Field(
                FieldNames.UUID, node.getNodeId().getUUID().toString(),
                Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
        try {
            // parent UUID
            if (node.getParentId() == null) {
                // root node
                doc.add(new Field(FieldNames.PARENT, "", Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
                addNodeName(doc, "", "");
            } else {
                doc.add(new Field(
                        FieldNames.PARENT, node.getParentId().toString(),
                        Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
                NodeState parent = (NodeState) stateProvider.getItemState(node.getParentId());
                ChildNodeEntry child = parent.getChildNodeEntry(node.getNodeId());
                if (child == null) {
                    // this can only happen when jackrabbit
                    // is running in a cluster.
                    throw new RepositoryException(
                            "Missing child node entry for node with id: "
                                    + node.getNodeId());
                }
                Name name = child.getName();
                addNodeName(doc, name.getNamespaceURI(), name.getLocalName());
            }
        } catch (NoSuchItemStateException e) {
            throwRepositoryException(e);
        } catch (ItemStateException e) {
            throwRepositoryException(e);
        } catch (NamespaceException e) {
            // will never happen, because this.mappings will dynamically add
            // unknown uri<->prefix mappings
        }

        Set props = node.getPropertyNames();
        for (Iterator it = props.iterator(); it.hasNext();) {
            Name propName = (Name) it.next();
            PropertyId id = new PropertyId(node.getNodeId(), propName);
            try {
                PropertyState propState = (PropertyState) stateProvider.getItemState(id);

                // add each property to the _PROPERTIES_SET for searching
                // beginning with V2
                if (indexFormatVersion.getVersion()
                        >= IndexFormatVersion.V2.getVersion()) {
                    addPropertyName(doc, propState.getName());
                }

                InternalValue[] values = propState.getValues();
                for (int i = 0; i < values.length; i++) {
                    addValue(doc, values[i], propState.getName());
                }
                if (values.length > 1) {
                    // real multi-valued
                    addMVPName(doc, propState.getName());
                }
            } catch (NoSuchItemStateException e) {
                throwRepositoryException(e);
            } catch (ItemStateException e) {
                throwRepositoryException(e);
            }
        }

        // now add fields that are not used in excerpt (must go at the end)
        for (Iterator it = doNotUseInExcerpt.iterator(); it.hasNext();) {
            doc.add((Fieldable) it.next());
        }
        return doc;
    }

    /**
     * Wraps the exception <code>e</code> into a <code>RepositoryException</code> and throws the created exception.
     *
     * @param e the base exception.
     */
    private void throwRepositoryException(Exception e)
            throws RepositoryException {
        String msg = "Error while indexing node: " + node.getNodeId() + " of "
                + "type: " + node.getNodeTypeName();
        throw new RepositoryException(msg, e);
    }

    /**
     * Adds a {@link FieldNames#MVP} field to <code>doc</code> with the resolved <code>name</code> using the internal
     * search index namespace mapping.
     *
     * @param doc  the lucene document.
     * @param name the name of the multi-value property.
     */
    private void addMVPName(Document doc, Name name) {
        try {
            String propName = resolver.getJCRName(name);
            doc.add(new Field(FieldNames.MVP, propName, Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
        } catch (NamespaceException e) {
            // will never happen, prefixes are created dynamically
        }
    }

    /**
     * Adds a value to the lucene Document.
     *
     * @param doc   the document.
     * @param value the internal jackrabbit value.
     * @param name  the name of the property.
     */
    private void addValue(Document doc, InternalValue value, Name name) {
        String fieldName = name.getLocalName();
        try {
            fieldName = resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // will never happen
        }
        switch (value.getType()) {
            case PropertyType.BINARY:
                if (isIndexed(name)) {
                    addBinaryValue(doc, fieldName, value.getBLOBFileValue());
                }
                break;
            case PropertyType.BOOLEAN:
                if (isIndexed(name)) {
                    addBooleanValue(doc, fieldName, Boolean.valueOf(value.getBoolean()));
                }
                break;
            case PropertyType.DATE:
                if (isIndexed(name)) {
                    addCalendarValue(doc, fieldName, value.getDate());
                }
                break;
            case PropertyType.DOUBLE:
                if (isIndexed(name)) {
                    addDoubleValue(doc, fieldName, new Double(value.getDouble()));
                }
                break;
            case PropertyType.LONG:
                if (isIndexed(name)) {
                    addLongValue(doc, fieldName, new Long(value.getLong()));
                }
                break;
            case PropertyType.REFERENCE:
                if (isIndexed(name)) {
                    addReferenceValue(doc, fieldName, value.getUUID());
                }
                break;
            case PropertyType.PATH:
                if (isIndexed(name)) {
                    addPathValue(doc, fieldName, value.getPath());
                }
                break;
            case PropertyType.STRING:
                if (isIndexed(name)) {
                    // never fulltext index jcr:uuid String
                    if (name.equals(NameConstants.JCR_UUID)) {
                        addStringValue(doc, fieldName, value.getString(),
                                false, false, DEFAULT_BOOST);
                    } else {
                        addStringValue(doc, fieldName, value.getString(),
                                true, isIncludedInNodeIndex(name),
                                getPropertyBoost(name), useInExcerpt(name));
                    }
                }
                break;
            case PropertyType.NAME:
                // jcr:primaryType and jcr:mixinTypes are required for correct
                // node type resolution in queries
                //TODO: [by yl] Get rid of this file when a JR 1.6 is released (https://issues.apache.org/jira/browse/JCR-1950)
                //yoavl: check indexing rules last, it is much faster - this is the only change in logic
                if (name.equals(NameConstants.JCR_PRIMARYTYPE)
                        || name.equals(NameConstants.JCR_MIXINTYPES)
                        || isIndexed(name)) {
                    addNameValue(doc, fieldName, value.getQName());
                }
                break;
            default:
                throw new IllegalArgumentException("illegal internal value type");
        }

        // add length
        if (indexFormatVersion.getVersion() >= IndexFormatVersion.V3.getVersion()) {
            addLength(doc, fieldName, value);
        }
    }

    /**
     * Adds the property name to the lucene _:PROPERTIES_SET field.
     *
     * @param doc  the document.
     * @param name the name of the property.
     */
    private void addPropertyName(Document doc, Name name) {
        String fieldName = name.getLocalName();
        try {
            fieldName = resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // will never happen
        }
        doc.add(new Field(FieldNames.PROPERTIES_SET, fieldName, Field.Store.NO, Field.Index.NO_NORMS));
    }

    /**
     * Adds the binary value to the document as the named field.
     * <p/>
     * This implementation checks if this {@link #node} is of type nt:resource and if that is the case, tries to extract
     * text from the binary property using the {@link #extractor}.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addBinaryValue(Document doc,
            String fieldName,
            Object internalValue) {
        // 'check' if node is of type nt:resource
        try {
            String jcrData = mappings.getPrefix(Name.NS_JCR_URI) + ":data";
            if (!jcrData.equals(fieldName)) {
                // don't know how to index
                return;
            }

            InternalValue typeValue = getValue(NameConstants.JCR_MIMETYPE);
            if (typeValue != null) {
                String type = typeValue.getString();

                // jcr:encoding is not mandatory
                String encoding = null;
                InternalValue encodingValue = getValue(NameConstants.JCR_ENCODING);
                if (encodingValue != null) {
                    encoding = encodingValue.getString();
                }

                InputStream stream =
                        ((BLOBFileValue) internalValue).getStream();
                Reader reader = extractor.extractText(stream, type, encoding);
                doc.add(createFulltextField(reader));
            }
        } catch (Throwable t) {
            // TODO: How to recover from a transient indexing failure?
            log.warn("Exception while indexing binary property", t);
        }
    }

    /**
     * Utility method that extracts the first value of the named property of the current node. Returns <code>null</code>
     * if the property does not exist or contains no values.
     *
     * @param name property name
     * @return value of the named property, or <code>null</code>
     * @throws ItemStateException if the property can not be accessed
     */
    protected InternalValue getValue(Name name) throws ItemStateException {
        try {
            PropertyId id = new PropertyId(node.getNodeId(), name);
            PropertyState property =
                    (PropertyState) stateProvider.getItemState(id);
            InternalValue[] values = property.getValues();
            if (values.length > 0) {
                return values[0];
            } else {
                return null;
            }
        } catch (NoSuchItemStateException e) {
            return null;
        }
    }

    /**
     * Adds the string representation of the boolean value to the document as the named field.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addBooleanValue(Document doc, String fieldName, Object internalValue) {
        doc.add(createFieldWithoutNorms(fieldName, internalValue.toString(),
                PropertyType.BOOLEAN));
    }

    /**
     * Creates a field of name <code>fieldName</code> with the value of <code> internalValue</code>. The created field
     * is indexed without norms.
     *
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     * @param propertyType  the property type.
     */
    protected Field createFieldWithoutNorms(String fieldName,
            String internalValue,
            int propertyType) {
        if (indexFormatVersion.getVersion()
                >= IndexFormatVersion.V3.getVersion()) {
            Field field = new Field(FieldNames.PROPERTIES,
                    new SingletonTokenStream(
                            FieldNames.createNamedValue(fieldName, internalValue),
                            propertyType)
            );
            field.setOmitNorms(true);
            return field;
        } else {
            return new Field(FieldNames.PROPERTIES,
                    FieldNames.createNamedValue(fieldName, internalValue),
                    Field.Store.NO, Field.Index.NO_NORMS,
                    Field.TermVector.NO);
        }
    }

    /**
     * Adds the calendar value to the document as the named field. The calendar value is converted to an indexable
     * string value using the {@link DateField} class.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addCalendarValue(Document doc, String fieldName, Object internalValue) {
        Calendar value = (Calendar) internalValue;
        long millis = value.getTimeInMillis();
        try {
            doc.add(createFieldWithoutNorms(fieldName, DateField.timeToString(millis),
                    PropertyType.DATE));
        } catch (IllegalArgumentException e) {
            log.warn("'{}' is outside of supported date value range.",
                    new Date(value.getTimeInMillis()));
        }
    }

    /**
     * Adds the double value to the document as the named field. The double value is converted to an indexable string
     * value using the {@link DoubleField} class.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addDoubleValue(Document doc, String fieldName, Object internalValue) {
        double doubleVal = ((Double) internalValue).doubleValue();
        doc.add(createFieldWithoutNorms(fieldName, DoubleField.doubleToString(doubleVal),
                PropertyType.DOUBLE));
    }

    /**
     * Adds the long value to the document as the named field. The long value is converted to an indexable string value
     * using the {@link LongField} class.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addLongValue(Document doc, String fieldName, Object internalValue) {
        long longVal = ((Long) internalValue).longValue();
        doc.add(createFieldWithoutNorms(fieldName, LongField.longToString(longVal),
                PropertyType.LONG));
    }

    /**
     * Adds the reference value to the document as the named field. The value's string representation is added as the
     * reference data. Additionally the reference data is stored in the index.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addReferenceValue(Document doc, String fieldName, Object internalValue) {
        UUID value = (UUID) internalValue;
        String uuid = value.toString();
        doc.add(createFieldWithoutNorms(fieldName, uuid,
                PropertyType.REFERENCE));
        doc.add(new Field(FieldNames.PROPERTIES,
                FieldNames.createNamedValue(fieldName, uuid),
                Field.Store.YES, Field.Index.NO, Field.TermVector.NO));
    }

    /**
     * Adds the path value to the document as the named field. The path value is converted to an indexable string value
     * using the name space mappings with which this class has been created.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addPathValue(Document doc, String fieldName, Object internalValue) {
        Path path = (Path) internalValue;
        String pathString = path.toString();
        try {
            pathString = resolver.getJCRPath(path);
        } catch (NamespaceException e) {
            // will never happen
        }
        doc.add(createFieldWithoutNorms(fieldName, pathString,
                PropertyType.PATH));
    }

    /**
     * Adds the string value to the document both as the named field and for full text indexing.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     * @deprecated Use {@link #addStringValue(Document, String, Object, boolean) addStringValue(Document, String,
     *             Object, boolean)} instead.
     */
    protected void addStringValue(Document doc, String fieldName, Object internalValue) {
        addStringValue(doc, fieldName, internalValue, true, true, DEFAULT_BOOST);
    }

    /**
     * Adds the string value to the document both as the named field and optionally for full text indexing if
     * <code>tokenized</code> is <code>true</code>.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     * @param tokenized     If <code>true</code> the string is also tokenized and fulltext indexed.
     */
    protected void addStringValue(Document doc, String fieldName,
            Object internalValue, boolean tokenized) {
        addStringValue(doc, fieldName, internalValue, tokenized, true, DEFAULT_BOOST);
    }

    /**
     * Adds the string value to the document both as the named field and optionally for full text indexing if
     * <code>tokenized</code> is <code>true</code>.
     *
     * @param doc                The document to which to add the field
     * @param fieldName          The name of the field to add
     * @param internalValue      The value for the field to add to the document.
     * @param tokenized          If <code>true</code> the string is also tokenized and fulltext indexed.
     * @param includeInNodeIndex If <code>true</code> the string is also tokenized and added to the node scope fulltext
     *                           index.
     * @param boost              the boost value for this string field.
     * @deprecated use {@link #addStringValue(Document, String, Object, boolean, boolean, float, boolean)} instead.
     */
    protected void addStringValue(Document doc, String fieldName,
            Object internalValue, boolean tokenized,
            boolean includeInNodeIndex, float boost) {
        addStringValue(doc, fieldName, internalValue, tokenized, includeInNodeIndex, boost, true);
    }

    /**
     * Adds the string value to the document both as the named field and optionally for full text indexing if
     * <code>tokenized</code> is <code>true</code>.
     *
     * @param doc                The document to which to add the field
     * @param fieldName          The name of the field to add
     * @param internalValue      The value for the field to add to the document.
     * @param tokenized          If <code>true</code> the string is also tokenized and fulltext indexed.
     * @param includeInNodeIndex If <code>true</code> the string is also tokenized and added to the node scope fulltext
     *                           index.
     * @param boost              the boost value for this string field.
     * @param useInExcerpt       If <code>true</code> the string may show up in an excerpt.
     */
    protected void addStringValue(Document doc, String fieldName,
            Object internalValue, boolean tokenized,
            boolean includeInNodeIndex, float boost,
            boolean useInExcerpt) {

        // simple String
        String stringValue = (String) internalValue;
        doc.add(createFieldWithoutNorms(fieldName, stringValue,
                PropertyType.STRING));
        if (tokenized) {
            if (stringValue.length() == 0) {
                return;
            }
            // create fulltext index on property
            int idx = fieldName.indexOf(':');
            fieldName = fieldName.substring(0, idx + 1)
                    + FieldNames.FULLTEXT_PREFIX + fieldName.substring(idx + 1);
            Field f = new Field(fieldName, stringValue,
                    Field.Store.NO,
                    Field.Index.TOKENIZED,
                    Field.TermVector.NO);
            f.setBoost(boost);
            doc.add(f);

            if (includeInNodeIndex) {
                // also create fulltext index of this value
                boolean store = supportHighlighting && useInExcerpt;
                f = createFulltextField(stringValue, store, supportHighlighting);
                if (useInExcerpt) {
                    doc.add(f);
                } else {
                    doNotUseInExcerpt.add(f);
                }
            }
        }
    }

    /**
     * Adds the name value to the document as the named field. The name value is converted to an indexable string
     * treating the internal value as a qualified name and mapping the name space using the name space mappings with
     * which this class has been created.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addNameValue(Document doc, String fieldName, Object internalValue) {
        try {
            Name qualiName = (Name) internalValue;
            String normValue = mappings.getPrefix(qualiName.getNamespaceURI())
                    + ":" + qualiName.getLocalName();
            doc.add(createFieldWithoutNorms(fieldName, normValue,
                    PropertyType.NAME));
        } catch (NamespaceException e) {
            // will never happen
        }
    }

    /**
     * Creates a fulltext field for the string <code>value</code>.
     *
     * @param value the string value.
     * @return a lucene field.
     * @deprecated use {@link #createFulltextField(String, boolean, boolean)} instead.
     */
    protected Field createFulltextField(String value) {
        return createFulltextField(value, supportHighlighting, supportHighlighting);
    }

    /**
     * Creates a fulltext field for the string <code>value</code>.
     *
     * @param value       the string value.
     * @param store       if the value of the field should be stored.
     * @param withOffsets if a term vector with offsets should be stored.
     * @return a lucene field.
     */
    protected Field createFulltextField(String value,
            boolean store,
            boolean withOffsets) {
        Field.TermVector tv;
        if (withOffsets) {
            tv = Field.TermVector.WITH_OFFSETS;
        } else {
            tv = Field.TermVector.NO;
        }
        if (store) {
            // store field compressed if greater than 16k
            Field.Store stored;
            if (value.length() > 0x4000) {
                stored = Field.Store.COMPRESS;
            } else {
                stored = Field.Store.YES;
            }
            return new Field(FieldNames.FULLTEXT, value, stored,
                    Field.Index.TOKENIZED, tv);
        } else {
            return new Field(FieldNames.FULLTEXT, value,
                    Field.Store.NO, Field.Index.TOKENIZED, tv);
        }
    }

    /**
     * Creates a fulltext field for the reader <code>value</code>.
     *
     * @param value the reader value.
     * @return a lucene field.
     */
    protected Fieldable createFulltextField(Reader value) {
        if (supportHighlighting) {
            return new LazyTextExtractorField(FieldNames.FULLTEXT, value, true, true);
        } else {
            return new LazyTextExtractorField(FieldNames.FULLTEXT, value, false, false);
        }
    }

    /**
     * Returns <code>true</code> if the property with the given name should be indexed.
     *
     * @param propertyName name of a property.
     * @return <code>true</code> if the property should be fulltext indexed; <code>false</code> otherwise.
     */
    protected boolean isIndexed(Name propertyName) {
        if (indexingConfig == null) {
            return true;
        } else {
            return indexingConfig.isIndexed(node, propertyName);
        }
    }

    /**
     * Returns <code>true</code> if the property with the given name should also be added to the node scope index.
     *
     * @param propertyName the name of a property.
     * @return <code>true</code> if it should be added to the node scope index; <code>false</code> otherwise.
     */
    protected boolean isIncludedInNodeIndex(Name propertyName) {
        if (indexingConfig == null) {
            return true;
        } else {
            return indexingConfig.isIncludedInNodeScopeIndex(node, propertyName);
        }
    }

    /**
     * Returns <code>true</code> if the content of the property with the given name should the used to create an
     * excerpt.
     *
     * @param propertyName the name of a property.
     * @return <code>true</code> if it should be used to create an excerpt; <code>false</code> otherwise.
     */
    protected boolean useInExcerpt(Name propertyName) {
        if (indexingConfig == null) {
            return true;
        } else {
            return indexingConfig.useInExcerpt(node, propertyName);
        }
    }

    /**
     * Returns the boost value for the given property name.
     *
     * @param propertyName the name of a property.
     * @return the boost value for the given property name.
     */
    protected float getPropertyBoost(Name propertyName) {
        if (indexingConfig == null) {
            return DEFAULT_BOOST;
        } else {
            return indexingConfig.getPropertyBoost(node, propertyName);
        }
    }

    /**
     * @return the boost value for this {@link #node} state.
     */
    protected float getNodeBoost() {
        if (indexingConfig == null) {
            return DEFAULT_BOOST;
        } else {
            return indexingConfig.getNodeBoost(node);
        }
    }

    /**
     * Adds a {@link FieldNames#PROPERTY_LENGTHS} field to <code>document</code> with a named length value.
     *
     * @param doc          the lucene document.
     * @param propertyName the property name.
     * @param value        the internal value.
     */
    protected void addLength(Document doc,
            String propertyName,
            InternalValue value) {
        long length;
        if (value.getType() == PropertyType.BINARY) {
            length = value.getBLOBFileValue().getLength();
        } else if (value.getType() == PropertyType.NAME
                || value.getType() == PropertyType.PATH) {
            // TODO https://jsr-283.dev.java.net/issues/show_bug.cgi?id=464
            return;
        } else {
            length = value.toString().length();
        }
        doc.add(new Field(FieldNames.PROPERTY_LENGTHS,
                FieldNames.createNamedLength(propertyName, length),
                Field.Store.NO, Field.Index.NO_NORMS));
    }

    /**
     * Depending on the index format version adds one or two fields to the document for the node name.
     *
     * @param doc          the lucene document.
     * @param namespaceURI the namespace URI of the node name.
     * @param localName    the local name of the node.
     */
    protected void addNodeName(Document doc,
            String namespaceURI,
            String localName) throws NamespaceException {
        String name = mappings.getPrefix(namespaceURI) + ":" + localName;
        doc.add(new Field(FieldNames.LABEL, name, Field.Store.NO, Field.Index.NO_NORMS));
        // as of version 3, also index combination of namespace URI and local name
        if (indexFormatVersion.getVersion() >= IndexFormatVersion.V3.getVersion()) {
            doc.add(new Field(FieldNames.NAMESPACE_URI, namespaceURI, Field.Store.NO, Field.Index.NO_NORMS));
            doc.add(new Field(FieldNames.LOCAL_NAME, localName, Field.Store.NO, Field.Index.NO_NORMS));
        }
    }
}
