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
package org.apache.jackrabbit.core.persistence.util;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

import javax.jcr.PropertyType;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * <code>Serializer</code> is a utility class that provides static methods
 * for serializing & deserializing <code>ItemState</code> and
 * <code>NodeReferences</code> objects using a simple binary serialization
 * format.
 */
public final class Serializer {

    private static final byte[] NULL_UUID_PLACEHOLDER_BYTES = new byte[] {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    /**
     * encoding used for serializing String values
     */
    private static final String ENCODING = "UTF-8";

    /**
     * Serializes the specified <code>NodeState</code> object to the given
     * binary <code>stream</code>.
     *
     * @param state  <code>state</code> to serialize
     * @param stream the stream where the <code>state</code> should be
     *               serialized to
     * @throws Exception if an error occurs during the serialization
     * @see #deserialize(NodeState, InputStream)
     */
    public static void serialize(NodeState state, OutputStream stream)
            throws Exception {
        DataOutputStream out = new DataOutputStream(stream);

        // primaryType
        out.writeUTF(state.getNodeTypeName().toString());
        // parentUUID
        if (state.getParentId() == null) {
            out.write(NULL_UUID_PLACEHOLDER_BYTES);
        } else {
            out.write(state.getParentId().getUUID().getRawBytes());
        }
        // definitionId
        out.writeUTF(state.getDefinitionId().toString());
        // mixin types
        Collection c = state.getMixinTypeNames();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            out.writeUTF(iter.next().toString());   // name
        }
        // modCount
        out.writeShort(state.getModCount());
        // properties (names)
        c = state.getPropertyNames();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            Name propName = (Name) iter.next();
            out.writeUTF(propName.toString());   // name
        }
        // child nodes (list of name/uuid pairs)
        c = state.getChildNodeEntries();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            ChildNodeEntry entry = (ChildNodeEntry) iter.next();
            out.writeUTF(entry.getName().toString());   // name
            out.write(entry.getId().getUUID().getRawBytes());    // uuid
        }
    }

    /**
     * Deserializes a <code>NodeState</code> object from the given binary
     * <code>stream</code>.
     *
     * @param state  <code>state</code> to deserialize
     * @param stream the stream where the <code>state</code> should be deserialized from
     * @throws Exception if an error occurs during the deserialization
     * @see #serialize(NodeState, OutputStream)
     */
    public static void deserialize(NodeState state, InputStream stream)
            throws Exception {
        DataInputStream in = new DataInputStream(stream);

        // primaryType
        String s = in.readUTF();
        state.setNodeTypeName(NameFactoryImpl.getInstance().create(s));
        // parentUUID (may be null)
        byte[] uuidBytes = new byte[UUID.UUID_BYTE_LENGTH];
        in.readFully(uuidBytes);
        if (!Arrays.equals(uuidBytes, NULL_UUID_PLACEHOLDER_BYTES)) {
            state.setParentId(new NodeId(new UUID(uuidBytes)));
        }
        // definitionId
        s = in.readUTF();
        state.setDefinitionId(NodeDefId.valueOf(s));
        // mixin types
        int count = in.readInt();   // count
        Set set = new HashSet(count);
        for (int i = 0; i < count; i++) {
            set.add(NameFactoryImpl.getInstance().create(in.readUTF())); // name
        }
        if (set.size() > 0) {
            state.setMixinTypeNames(set);
        }
        // modCount
        short modCount = in.readShort();
        state.setModCount(modCount);
        // properties (names)
        count = in.readInt();   // count
        for (int i = 0; i < count; i++) {
            state.addPropertyName(NameFactoryImpl.getInstance().create(in.readUTF())); // name
        }
        // child nodes (list of name/uuid pairs)
        count = in.readInt();   // count
        for (int i = 0; i < count; i++) {
            Name name = NameFactoryImpl.getInstance().create(in.readUTF());    // name
            // uuid
            in.readFully(uuidBytes);
            state.addChildNodeEntry(name, new NodeId(new UUID(uuidBytes)));
        }
    }

    /**
     * Serializes the specified <code>PropertyState</code> object to the given
     * binary <code>stream</code>. Binary values are stored in the specified
     * <code>BLOBStore</code>.
     *
     * @param state     <code>state</code> to serialize
     * @param stream    the stream where the <code>state</code> should be
     *                  serialized to
     * @param blobStore handler for BLOB data
     * @throws Exception if an error occurs during the serialization
     * @see #deserialize(PropertyState, InputStream,BLOBStore)
     */
    public static void serialize(PropertyState state,
                                 OutputStream stream,
                                 BLOBStore blobStore)
            throws Exception {
        DataOutputStream out = new DataOutputStream(stream);

        // type
        out.writeInt(state.getType());
        // multiValued
        out.writeBoolean(state.isMultiValued());
        // definitionId
        out.writeUTF(state.getDefinitionId().toString());
        // modCount
        out.writeShort(state.getModCount());
        // values
        InternalValue[] values = state.getValues();
        out.writeInt(values.length); // count
        for (int i = 0; i < values.length; i++) {
            InternalValue val = values[i];
            if (state.getType() == PropertyType.BINARY) {
                // special handling required for binary value:
                // put binary value in BLOB store
                BLOBFileValue blobVal = val.getBLOBFileValue();
                InputStream in = blobVal.getStream();
                String blobId = blobStore.createId(state.getPropertyId(), i);
                try {
                    blobStore.put(blobId, in, blobVal.getLength());
                } finally {
                    IOUtils.closeQuietly(in);
                }
                // store id of BLOB as property value
                out.writeUTF(blobId);   // value
                // replace value instance with value backed by resource
                // in BLOB store and discard old value instance (e.g. temp file)
                if (blobStore instanceof ResourceBasedBLOBStore) {
                    // optimization: if the BLOB store is resource-based
                    // retrieve the resource directly rather than having
                    // to read the BLOB from an input stream
                    FileSystemResource fsRes =
                            ((ResourceBasedBLOBStore) blobStore).getResource(blobId);
                    values[i] = InternalValue.create(fsRes);
                } else {
                    in = blobStore.get(blobId);
                    try {
                        values[i] = InternalValue.create(in);
                    } finally {
                        IOUtils.closeQuietly(in);
                    }
                }
                blobVal.discard();
            } else {
                /**
                 * because writeUTF(String) has a size limit of 65k,
                 * Strings are serialized as <length><byte[]>
                 */
                //out.writeUTF(val.toString());   // value
                byte[] bytes = val.toString().getBytes(ENCODING);
                out.writeInt(bytes.length); // lenght of byte[]
                out.write(bytes);   // byte[]
            }
        }
    }

    /**
     * Deserializes a <code>PropertyState</code> object from the given binary
     * <code>stream</code>. Binary values are retrieved from the specified
     * <code>BLOBStore</code>.
     *
     * @param state     <code>state</code> to deserialize
     * @param stream    the stream where the <code>state</code> should be
     *                  deserialized from
     * @param blobStore handler for BLOB data
     * @throws Exception if an error occurs during the deserialization
     * @see #serialize(PropertyState, OutputStream, BLOBStore)
     */
    public static void deserialize(PropertyState state,
                                   InputStream stream,
                                   BLOBStore blobStore)
            throws Exception {
        DataInputStream in = new DataInputStream(stream);

        // type
        int type = in.readInt();
        state.setType(type);
        // multiValued
        boolean multiValued = in.readBoolean();
        state.setMultiValued(multiValued);
        // definitionId
        String s = in.readUTF();
        state.setDefinitionId(PropDefId.valueOf(s));
        // modCount
        short modCount = in.readShort();
        state.setModCount(modCount);
        // values
        int count = in.readInt();   // count
        InternalValue[] values = new InternalValue[count];
        for (int i = 0; i < count; i++) {
            InternalValue val;
            if (type == PropertyType.BINARY) {
                s = in.readUTF();   // value (i.e. blobId)
                // special handling required for binary value:
                // the value stores the id of the BLOB data
                // in the BLOB store
                if (blobStore instanceof ResourceBasedBLOBStore) {
                    // optimization: if the BLOB store is resource-based
                    // retrieve the resource directly rather than having
                    // to read the BLOB from an input stream
                    FileSystemResource fsRes =
                            ((ResourceBasedBLOBStore) blobStore).getResource(s);
                    val = InternalValue.create(fsRes);
                } else {
                    InputStream is = blobStore.get(s);
                    try {
                        val = InternalValue.create(is);
                    } finally {
                        try {
                            is.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            } else {
                /**
                 * because writeUTF(String) has a size limit of 65k,
                 * Strings are serialized as <length><byte[]>
                 */
                //s = in.readUTF();   // value
                int len = in.readInt(); // lenght of byte[]
                byte[] bytes = new byte[len];
                in.readFully(bytes); // byte[]
                s = new String(bytes, ENCODING);
                val = InternalValue.valueOf(s, type);
            }
            values[i] = val;
        }
        state.setValues(values);
    }

    /**
     * Serializes the specified <code>NodeReferences</code> object to the given
     * binary <code>stream</code>.
     *
     * @param refs   object to serialize
     * @param stream the stream where the object should be serialized to
     * @throws Exception if an error occurs during the serialization
     * @see #deserialize(NodeReferences, InputStream)
     */
    public static void serialize(NodeReferences refs, OutputStream stream)
            throws Exception {
        DataOutputStream out = new DataOutputStream(stream);

        // references
        Collection c = refs.getReferences();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            PropertyId propId = (PropertyId) iter.next();
            out.writeUTF(propId.toString());   // propertyId
        }
    }

    /**
     * Deserializes a <code>NodeReferences</code> object from the given
     * binary <code>stream</code>.
     *
     * @param refs   object to deserialize
     * @param stream the stream where the object should be deserialized from
     * @throws Exception if an error occurs during the deserialization
     * @see #serialize(NodeReferences, OutputStream)
     */
    public static void deserialize(NodeReferences refs, InputStream stream)
            throws Exception {
        DataInputStream in = new DataInputStream(stream);

        refs.clearAllReferences();

        // references
        int count = in.readInt();   // count
        for (int i = 0; i < count; i++) {
            refs.addReference(PropertyId.valueOf(in.readUTF()));    // propertyId
        }
    }
}
