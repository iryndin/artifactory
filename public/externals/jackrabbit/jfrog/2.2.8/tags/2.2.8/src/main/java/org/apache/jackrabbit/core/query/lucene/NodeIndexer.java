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
package org.apache.jackrabbit.core.query.lucene;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Namespace mappings to use for indexing. This is the internal
     * namespace mapping.
     */
    protected final NamespaceMappings mappings;

    /**
     * Name and Path resolver.
     */
    protected final NamePathResolver resolver;

    /**
     * Background task executor used for full text extraction.
     */
    private final Executor executor;

    /**
     * Parser used for extracting text content from binary properties
     * for full text indexing.
     */
    private final Parser parser;

    /**
     * The indexing configuration or <code>null</code> if none is available.
     */
    protected IndexingConfiguration indexingConfig;

    /**
     * If set to <code>true</code> the fulltext field is stored and and a term
     * vector is created with offset information.
     */
    protected boolean supportHighlighting = false;

    /**
     * Indicates index format for this node indexer.
     */
    protected IndexFormatVersion indexFormatVersion = IndexFormatVersion.V1;

    /**
     * List of {@link FieldNames#FULLTEXT} fields which should not be used in
     * an excerpt.
     */
    protected List<Fieldable> doNotUseInExcerpt = new ArrayList<Fieldable>();

    /**
     * The maximum number of characters to extract from binaries.
     */
    private int maxExtractLength = Integer.MAX_VALUE;

    /**
     * Creates a new node indexer.
     *
     * @param node          the node state to index.
     * @param stateProvider the persistent item state manager to retrieve properties.
     * @param mappings      internal namespace mappings.
     * @param executor      background task executor for text extraction
     * @param parser        parser for binary properties
     */
    public NodeIndexer(
            NodeState node, ItemStateManager stateProvider,
            NamespaceMappings mappings, Executor executor, Parser parser) {
        this.node = node;
        this.stateProvider = stateProvider;
        this.mappings = mappings;
        this.resolver = NamePathResolverImpl.create(mappings);
        this.executor = executor;
        this.parser = parser;
    }

    /**
     * Returns the <code>NodeId</code> of the indexed node.
     * @return the <code>NodeId</code> of the indexed node.
     */
    public NodeId getNodeId() {
        return node.getNodeId();
    }

    /**
     * If set to <code>true</code> additional information is stored in the index
     * to support highlighting using the rep:excerpt pseudo property.
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
     * Returns the maximum number of characters to extract from binaries.
     *
     * @return maximum extraction length
     */
    public int getMaxExtractLength() {
        return maxExtractLength;
    }

    /**
     * Sets the maximum number of characters to extract from binaries.
     *
     * @param length maximum extraction length
     */
    public void setMaxExtractLength(int length) {
        this.maxExtractLength = length;
    }

    /**
     * Creates a lucene Document.
     *
     * @return the lucene Document with the index layout.
     * @throws RepositoryException if an error occurs while reading property
     *                             values from the <code>ItemStateProvider</code>.
     */
    public Document createDoc() throws RepositoryException {
        doNotUseInExcerpt.clear();
        Document doc = new Document();

        doc.setBoost(getNodeBoost());

        // special fields
        // UUID
        doc.add(new IDField(node.getNodeId()));
        try {
            // parent UUID
            if (node.getParentId() == null) {
                // root node
                doc.add(new Field(FieldNames.PARENT, "", Field.Store.YES,
                        Field.Index.NOT_ANALYZED_NO_NORMS));
                addNodeName(doc, "", "");
            } else if (node.getSharedSet().isEmpty()) {
                addParentChildRelation(doc, node.getParentId());
            } else {
                // shareable node
                for (NodeId id : node.getSharedSet()) {
                    addParentChildRelation(doc, id);
                }
                // mark shareable nodes
                doc.add(new Field(FieldNames.SHAREABLE_NODE, "",
                        Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
            }
        } catch (NoSuchItemStateException e) {
            throwRepositoryException(e);
        } catch (ItemStateException e) {
            throwRepositoryException(e);
        } catch (NamespaceException e) {
            // will never happen, because this.mappings will dynamically add
            // unknown uri<->prefix mappings
        }

        Set<Name> props = node.getPropertyNames();
        for (Name propName : props) {
            PropertyId id = new PropertyId(node.getNodeId(), propName);
            try {
                PropertyState propState = (PropertyState) stateProvider.getItemState(id);

                // add each property to the _PROPERTIES_SET for searching
                // beginning with V2
                if (indexFormatVersion.getVersion() >= IndexFormatVersion.V2.getVersion()) {
                    addPropertyName(doc, propState.getName());
                }

                InternalValue[] values = propState.getValues();
                for (InternalValue value : values) {
                    addValue(doc, value, propState.getName());
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
        for (Fieldable field : doNotUseInExcerpt) {
            doc.add(field);
        }
        return doc;
    }

    /**
     * Wraps the exception <code>e</code> into a <code>RepositoryException</code>
     * and throws the created exception.
     *
     * @param e the base exception.
     */
    protected void throwRepositoryException(Exception e)
            throws RepositoryException {
        String msg = "Error while indexing node: " + node.getNodeId() + " of "
            + "type: " + node.getNodeTypeName();
        throw new RepositoryException(msg, e);
    }

    /**
     * Adds a {@link FieldNames#MVP} field to <code>doc</code> with the resolved
     * <code>name</code> using the internal search index namespace mapping.
     *
     * @param doc  the lucene document.
     * @param name the name of the multi-value property.
     */
    protected void addMVPName(Document doc, Name name) {
        try {
            String propName = resolver.getJCRName(name);
            doc.add(new Field(FieldNames.MVP, propName, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO));
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
    protected void addValue(Document doc, InternalValue value, Name name) throws RepositoryException {
        String fieldName = name.getLocalName();
        try {
            fieldName = resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // will never happen
        }
        switch (value.getType()) {
            case PropertyType.BINARY:
                if (isIndexed(name)) {
                    addBinaryValue(doc, fieldName, value);
                }
                break;
            case PropertyType.BOOLEAN:
                if (isIndexed(name)) {
                    addBooleanValue(doc, fieldName, value.getBoolean());
                }
                break;
            case PropertyType.DATE:
                if (isIndexed(name)) {
                    addCalendarValue(doc, fieldName, value.getDate());
                }
                break;
            case PropertyType.DOUBLE:
                if (isIndexed(name)) {
                    addDoubleValue(doc, fieldName, value.getDouble());
                }
                break;
            case PropertyType.LONG:
                if (isIndexed(name)) {
                    addLongValue(doc, fieldName, value.getLong());
                }
                break;
            case PropertyType.REFERENCE:
                if (isIndexed(name)) {
                    addReferenceValue(doc, fieldName, value.getNodeId(), false);
                }
                break;
            case PropertyType.WEAKREFERENCE:
                if (isIndexed(name)) {
                    addReferenceValue(doc, fieldName, value.getNodeId(), true);
                }
                break;
            case PropertyType.PATH:
                if (isIndexed(name)) {
                    addPathValue(doc, fieldName, value.getPath());
                }
                break;
            case PropertyType.URI:
                if (isIndexed(name)) {
                    addURIValue(doc, fieldName, value.getURI());
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
                if (name.equals(NameConstants.JCR_PRIMARYTYPE)
                        || name.equals(NameConstants.JCR_MIXINTYPES)
                        || isIndexed(name)) {
                    addNameValue(doc, fieldName, value.getName());
                }
                break;
            case PropertyType.DECIMAL:
                if (isIndexed(name)) {
                    addDecimalValue(doc, fieldName, value.getDecimal());
                }
                break;
            default:
                throw new IllegalArgumentException("illegal internal value type: " + value.getType());
        }
        addValueProperty(doc, value, name, fieldName);
    }

    /**
     * Adds a property related value to the lucene Document. <br>
     *
     * Like <code>length</code> for indexed fields.
     *
     * @param doc
     *            the document.
     * @param value
     *            the internal jackrabbit value.
     * @param name
     *            the name of the property.
     */
    protected void addValueProperty(Document doc, InternalValue value,
            Name name, String fieldName) throws RepositoryException {

        // skip this method if field is not indexed
        if (!isIndexed(name)) {
            return;
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
    protected void addPropertyName(Document doc, Name name) {
        String fieldName = name.getLocalName();
        try {
            fieldName = resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // will never happen
        }
        doc.add(new Field(FieldNames.PROPERTIES_SET, fieldName, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
    }

    /**
     * Adds the binary value to the document as the named field.
     * <p/>
     * This implementation checks if this {@link #node} is of type nt:resource
     * and if that is the case, tries to extract text from the binary property
     * using the {@link #extractor}.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addBinaryValue(Document doc,
                                  String fieldName,
                                  InternalValue internalValue) {
        // 'check' if node is of type nt:resource
        try {
            String jcrData = mappings.getPrefix(Name.NS_JCR_URI) + ":data";
            if (!jcrData.equals(fieldName)) {
                // don't know how to index
                return;
            }

            InternalValue type = getValue(NameConstants.JCR_MIMETYPE);
            if (type != null) {
                Metadata metadata = new Metadata();
                metadata.set(Metadata.CONTENT_TYPE, type.getString());

                // jcr:encoding is not mandatory
                InternalValue encoding = getValue(NameConstants.JCR_ENCODING);
                if (encoding != null) {
                    metadata.set(
                            Metadata.CONTENT_ENCODING, encoding.getString());
                }

                doc.add(createFulltextField(internalValue, metadata));
            }
        } catch (Throwable t) {
            // TODO: How to recover from a transient indexing failure?
            log.warn("Exception while indexing binary property", t);
        }
    }

    /**
     * Utility method that extracts the first value of the named property
     * of the current node. Returns <code>null</code> if the property does
     * not exist or contains no values.
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
     * Adds the string representation of the boolean value to the document as
     * the named field.
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
     * Creates a field of name <code>fieldName</code> with the value of <code>
     * internalValue</code>. The created field is indexed without norms.
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
                    Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS,
                    Field.TermVector.NO);
        }
    }

    /**
     * Adds the calendar value to the document as the named field. The calendar
     * value is converted to an indexable string value using the
     * {@link DateField} class.
     *
     * @param doc
     *            The document to which to add the field
     * @param fieldName
     *            The name of the field to add
     * @param internalValue
     *            The value for the field to add to the document.
     */
    protected void addCalendarValue(Document doc, String fieldName, Calendar internalValue) {
        try {
            doc.add(createFieldWithoutNorms(fieldName,
                    DateField.timeToString(internalValue.getTimeInMillis()),
                    PropertyType.DATE));
        } catch (IllegalArgumentException e) {
            log.warn("'{}' is outside of supported date value range.",
                    internalValue);
        }
    }

    /**
     * Adds the double value to the document as the named field. The double
     * value is converted to an indexable string value using the
     * {@link DoubleField} class.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addDoubleValue(Document doc, String fieldName, double internalValue) {
        doc.add(createFieldWithoutNorms(fieldName, DoubleField.doubleToString(internalValue),
                PropertyType.DOUBLE));
    }

    /**
     * Adds the long value to the document as the named field. The long
     * value is converted to an indexable string value using the {@link LongField}
     * class.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addLongValue(Document doc, String fieldName, long internalValue) {
        doc.add(createFieldWithoutNorms(fieldName, LongField.longToString(internalValue),
                PropertyType.LONG));
    }

    /**
     * Adds the long value to the document as the named field. The long
     * value is converted to an indexable string value using the {@link LongField}
     * class.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addDecimalValue(Document doc, String fieldName, BigDecimal internalValue) {
        doc.add(createFieldWithoutNorms(fieldName, DecimalField.decimalToString(internalValue),
                PropertyType.DECIMAL));
    }

    /**
     * Adds the reference value to the document as the named field. The value's
     * string representation is added as the reference data. Additionally the
     * reference data is stored in the index. As of Jackrabbit 2.0 this method
     * also adds the reference UUID as a {@link FieldNames#WEAK_REFS} field
     * to the index if it is a weak reference.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     * @param weak          Flag indicating whether it's a WEAKREFERENCE (true) or a REFERENCE (flase)
     */
    protected void addReferenceValue(Document doc, String fieldName, NodeId internalValue, boolean weak) {
        String uuid = internalValue.toString();
        doc.add(createFieldWithoutNorms(fieldName, uuid,
                weak ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE));
        doc.add(new Field(FieldNames.PROPERTIES,
                FieldNames.createNamedValue(fieldName, uuid),
                Field.Store.YES, Field.Index.NO, Field.TermVector.NO));
        if (weak) {
            doc.add(new Field(FieldNames.WEAK_REFS, uuid, Field.Store.NO,
                    Field.Index.NOT_ANALYZED_NO_NORMS));
        }
    }

    /**
     * Adds the path value to the document as the named field. The path
     * value is converted to an indexable string value using the name space
     * mappings with which this class has been created.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addPathValue(Document doc, String fieldName, Path internalValue) {
        String pathString = internalValue.toString();
        try {
            pathString = resolver.getJCRPath(internalValue);
        } catch (NamespaceException e) {
            // will never happen
        }
        doc.add(createFieldWithoutNorms(fieldName, pathString,
                PropertyType.PATH));
    }

    /**
     * Adds the uri value to the document as the named field.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addURIValue(Document doc, String fieldName, URI internalValue) {
        doc.add(createFieldWithoutNorms(fieldName, internalValue.toString(),
                PropertyType.URI));
    }

    /**
     * Adds the string value to the document both as the named field and for
     * full text indexing.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     * @deprecated Use {@link #addStringValue(Document, String, Object, boolean)
     *             addStringValue(Document, String, Object, boolean)} instead.
     */
    protected void addStringValue(Document doc, String fieldName, String internalValue) {
        addStringValue(doc, fieldName, internalValue, true, true, DEFAULT_BOOST);
    }

    /**
     * Adds the string value to the document both as the named field and
     * optionally for full text indexing if <code>tokenized</code> is
     * <code>true</code>.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     * @param tokenized     If <code>true</code> the string is also tokenized
     *                      and fulltext indexed.
     */
    protected void addStringValue(Document doc, String fieldName,
                                  String internalValue, boolean tokenized) {
        addStringValue(doc, fieldName, internalValue, tokenized, true, DEFAULT_BOOST);
    }

    /**
     * Adds the string value to the document both as the named field and
     * optionally for full text indexing if <code>tokenized</code> is
     * <code>true</code>.
     *
     * @param doc                The document to which to add the field
     * @param fieldName          The name of the field to add
     * @param internalValue      The value for the field to add to the
     *                           document.
     * @param tokenized          If <code>true</code> the string is also
     *                           tokenized and fulltext indexed.
     * @param includeInNodeIndex If <code>true</code> the string is also
     *                           tokenized and added to the node scope fulltext
     *                           index.
     * @param boost              the boost value for this string field.
     * @deprecated use {@link #addStringValue(Document, String, Object, boolean, boolean, float, boolean)} instead.
     */
    protected void addStringValue(Document doc, String fieldName,
                                  String internalValue, boolean tokenized,
                                  boolean includeInNodeIndex, float boost) {
        addStringValue(doc, fieldName, internalValue, tokenized, includeInNodeIndex, boost, true);
    }

    /**
     * Adds the string value to the document both as the named field and
     * optionally for full text indexing if <code>tokenized</code> is
     * <code>true</code>.
     *
     * @param doc                The document to which to add the field
     * @param fieldName          The name of the field to add
     * @param internalValue      The value for the field to add to the
     *                           document.
     * @param tokenized          If <code>true</code> the string is also
     *                           tokenized and fulltext indexed.
     * @param includeInNodeIndex If <code>true</code> the string is also
     *                           tokenized and added to the node scope fulltext
     *                           index.
     * @param boost              the boost value for this string field.
     * @param useInExcerpt       If <code>true</code> the string may show up in
     *                           an excerpt.
     */
    protected void addStringValue(Document doc, String fieldName,
                                  String internalValue, boolean tokenized,
                                  boolean includeInNodeIndex, float boost,
                                  boolean useInExcerpt) {

        // simple String
        doc.add(createFieldWithoutNorms(fieldName, internalValue,
                PropertyType.STRING));
        if (tokenized) {
            if (internalValue.length() == 0) {
                return;
            }
            // create fulltext index on property
            int idx = fieldName.indexOf(':');
            fieldName = fieldName.substring(0, idx + 1)
                    + FieldNames.FULLTEXT_PREFIX + fieldName.substring(idx + 1);
            Field f = new Field(fieldName, internalValue, Field.Store.NO,
                    Field.Index.ANALYZED, Field.TermVector.NO);
            f.setBoost(boost);
            doc.add(f);

            if (includeInNodeIndex) {
                // also create fulltext index of this value
                boolean store = supportHighlighting && useInExcerpt;
                f = createFulltextField(internalValue, store, supportHighlighting);
                if (useInExcerpt) {
                    doc.add(f);
                } else {
                    doNotUseInExcerpt.add(f);
                }
            }
        }
    }

    /**
     * Adds the name value to the document as the named field. The name
     * value is converted to an indexable string treating the internal value
     * as a <code>Name</code> and mapping the name space using the name space
     * mappings with which this class has been created.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addNameValue(Document doc, String fieldName, Name internalValue) {
        try {
            String normValue = mappings.getPrefix(internalValue.getNamespaceURI())
                    + ":" + internalValue.getLocalName();
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
     * @param value the string value.
     * @param store if the value of the field should be stored.
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
                    Field.Index.ANALYZED, tv);
        } else {
            return new Field(FieldNames.FULLTEXT, value,
                    Field.Store.NO, Field.Index.ANALYZED, tv);
        }
    }

    /**
     * Creates a fulltext field for the reader <code>value</code>.
     *
     * @param value the binary value
     * @param metadata document metatadata
     * @return a lucene field.
     */
    protected Fieldable createFulltextField(
            InternalValue value, Metadata metadata) {
        return new LazyTextExtractorField(
                parser, value, metadata, executor,
                supportHighlighting, getMaxExtractLength());
    }

    /**
     * Returns <code>true</code> if the property with the given name should be
     * indexed.
     *
     * @param propertyName name of a property.
     * @return <code>true</code> if the property should be fulltext indexed;
     *         <code>false</code> otherwise.
     */
    protected boolean isIndexed(Name propertyName) {
        if (indexingConfig == null) {
            return true;
        } else {
            return indexingConfig.isIndexed(node, propertyName);
        }
    }

    /**
     * Returns <code>true</code> if the property with the given name should also
     * be added to the node scope index.
     *
     * @param propertyName the name of a property.
     * @return <code>true</code> if it should be added to the node scope index;
     *         <code>false</code> otherwise.
     */
    protected boolean isIncludedInNodeIndex(Name propertyName) {
        if (indexingConfig == null) {
            return true;
        } else {
            return indexingConfig.isIncludedInNodeScopeIndex(node, propertyName);
        }
    }

    /**
     * Returns <code>true</code> if the content of the property with the given
     * name should the used to create an excerpt.
     *
     * @param propertyName the name of a property.
     * @return <code>true</code> if it should be used to create an excerpt;
     *         <code>false</code> otherwise.
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
     * Adds a {@link FieldNames#PROPERTY_LENGTHS} field to <code>document</code>
     * with a named length value.
     *
     * @param doc          the lucene document.
     * @param propertyName the property name.
     * @param value        the internal value.
     */
    protected void addLength(Document doc,
                             String propertyName,
                             InternalValue value) {
        long length = Util.getLength(value);
        if (length != -1) {
            doc.add(new Field(FieldNames.PROPERTY_LENGTHS,
                    FieldNames.createNamedLength(propertyName, length),
                    Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
        }
    }

    /**
     * Depending on the index format version adds one or two fields to the
     * document for the node name.
     *
     * @param doc the lucene document.
     * @param namespaceURI the namespace URI of the node name.
     * @param localName the local name of the node.
     */
    protected void addNodeName(Document doc,
                               String namespaceURI,
                               String localName) throws NamespaceException {
        String name = mappings.getPrefix(namespaceURI) + ":" + localName;
        doc.add(new Field(FieldNames.LABEL, name, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
        // as of version 3, also index combination of namespace URI and local name
        if (indexFormatVersion.getVersion() >= IndexFormatVersion.V3.getVersion()) {
            doc.add(new Field(FieldNames.NAMESPACE_URI, namespaceURI, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
            doc.add(new Field(FieldNames.LOCAL_NAME, localName, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
        }
    }

    /**
     * Adds a parent child relation to the given <code>doc</code>.
     *
     * @param doc      the document.
     * @param parentId the id of the parent node.
     * @throws ItemStateException  if the parent node cannot be read.
     * @throws RepositoryException if the parent node does not have a child node
     *                             entry for the current node.
     */
    protected void addParentChildRelation(Document doc,
                                          NodeId parentId)
            throws ItemStateException, RepositoryException {
        doc.add(new Field(
                FieldNames.PARENT, parentId.toString(),
                Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO));
        NodeState parent = (NodeState) stateProvider.getItemState(parentId);
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
}
