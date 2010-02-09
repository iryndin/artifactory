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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.nodetype.PropertyDefinitionImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.value.ValueHelper;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * <code>PropertyImpl</code> implements the <code>Property</code> interface.
 */
public class PropertyImpl extends ItemImpl implements Property {

    private static Logger log = LoggerFactory.getLogger(PropertyImpl.class);

    /** property data (avoids casting <code>ItemImpl.data</code>) */
    private final PropertyData data;

    /**
     * Package private constructor.
     *
     * @param itemMgr    the <code>ItemManager</code> that created this <code>Property</code>
     * @param session    the <code>Session</code> through which this <code>Property</code> is acquired
     * @param data       the property data
     */
    PropertyImpl(ItemManager itemMgr, SessionImpl session, PropertyData data) {
        super(itemMgr, session, data);
        this.data = data;
        // value will be read on demand
    }

    /**
     * Checks that this property is valid (session not closed, property not
     * removed, etc.) and returns the underlying property state if all is OK.
     *
     * @return property state
     * @throws RepositoryException if the property is not valid
     */
    private PropertyState getPropertyState() throws RepositoryException {
        // JCR-1272: Need to get the state reference now so it
        // doesn't get invalidated after the sanity check
        ItemState state = getItemState();
        sanityCheck();
        return (PropertyState) state;
    }

    protected synchronized ItemState getOrCreateTransientItemState()
            throws RepositoryException {

        synchronized (data) {
            if (!isTransient()) {
                // make transient (copy-on-write)
                try {
                    PropertyState transientState =
                            stateMgr.createTransientPropertyState(
                                    data.getPropertyState(), ItemState.STATUS_EXISTING_MODIFIED);
                    // swap persistent with transient state
                    data.setState(transientState);
                } catch (ItemStateException ise) {
                    String msg = "failed to create transient state";
                    log.debug(msg);
                    throw new RepositoryException(msg, ise);
                }
            }
            return getItemState();
        }
    }

    protected void makePersistent() throws InvalidItemStateException {
        if (!isTransient()) {
            log.debug(this + " (" + id + "): there's no transient state to persist");
            return;
        }

        PropertyState transientState = data.getPropertyState();
        PropertyState persistentState = (PropertyState) transientState.getOverlayedState();
        if (persistentState == null) {
            // this property is 'new'
            persistentState = stateMgr.createNew(transientState);
        }

        synchronized (persistentState) {
            // check staleness of transient state first
            if (transientState.isStale()) {
                String msg =
                    this + ": the property cannot be saved because it has"
                    + " been modified externally.";
                log.debug(msg);
                throw new InvalidItemStateException(msg);
            }
            // copy state from transient state
            persistentState.setDefinitionId(transientState.getDefinitionId());
            persistentState.setType(transientState.getType());
            persistentState.setMultiValued(transientState.isMultiValued());
            persistentState.setValues(transientState.getValues());
            // make state persistent
            stateMgr.store(persistentState);
        }

        // tell state manager to disconnect item state
        stateMgr.disconnectTransientItemState(transientState);
        // swap transient state with persistent state
        data.setState(persistentState);
        // reset status
        data.setStatus(STATUS_NORMAL);
    }

    protected void restoreTransient(PropertyState transientState)
            throws RepositoryException {
        PropertyState thisState = (PropertyState) getOrCreateTransientItemState();
        if (transientState.getStatus() == ItemState.STATUS_NEW
                && thisState.getStatus() != ItemState.STATUS_NEW) {
            thisState.setStatus(ItemState.STATUS_NEW);
            stateMgr.disconnectTransientItemState(thisState);
        }
        // reapply transient changes
        thisState.setDefinitionId(transientState.getDefinitionId());
        thisState.setType(transientState.getType());
        thisState.setMultiValued(transientState.isMultiValued());
        thisState.setValues(transientState.getValues());
    }

    protected void onRedefine(PropDefId defId) throws RepositoryException {
        PropertyDefinitionImpl newDef =
                session.getNodeTypeManager().getPropertyDefinition(defId);
        // modify the state of 'this', i.e. the target property
        PropertyState thisState = (PropertyState) getOrCreateTransientItemState();
        // set id of new definition
        thisState.setDefinitionId(defId);
        data.setDefinition(newDef);
    }

    /**
     * Determines the length of the given value.
     *
     * @param value value whose length should be determined
     * @return the length of the given value
     * @throws RepositoryException if an error occurs
     * @see javax.jcr.Property#getLength()
     * @see javax.jcr.Property#getLengths()
     */
    protected long getLength(InternalValue value) throws RepositoryException {
        // TODO maybe move method to InternalValue
        switch (value.getType()) {
            case PropertyType.STRING:
            case PropertyType.LONG:
            case PropertyType.DOUBLE:
            case PropertyType.DATE:
            case PropertyType.REFERENCE:
            case PropertyType.BOOLEAN:
                return value.toString().length();

            case PropertyType.NAME:
                Name name = value.getQName();
                return session.getJCRName(name).length();

            case PropertyType.PATH:
                Path path = value.getPath();
                return session.getJCRPath(path).length();

            case PropertyType.BINARY:
                BLOBFileValue blob = value.getBLOBFileValue();
                return blob.getLength();

            default:
                return -1;
        }
    }

    /**
     * Checks various pre-conditions that are common to all
     * <code>setValue()</code> methods. The checks performed are:
     * <ul>
     * <li>parent node must be checked-out</li>
     * <li>property must not be protected</li>
     * <li>parent node must not be locked by somebody else</li>
     * <li>property must be multi-valued when set to an array of values
     * (and vice versa)</li>
     * </ul>
     *
     * @param multipleValues flag indicating whether the property is about to
     *                       be set to an array of values
     * @throws ValueFormatException         if a single-valued property is set to an
     *                                      array of values (and vice versa)
     * @throws VersionException             if the parent node is not checked-out
     * @throws LockException                if the parent node is locked by somebody else
     * @throws ConstraintViolationException if the property is protected
     * @throws RepositoryException          if another error occurs
     * @see javax.jcr.Property#setValue
     */
    protected void checkSetValue(boolean multipleValues)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        NodeImpl parent = (NodeImpl) getParent();
        PropertyDefinition definition = data.getPropertyDefinition();
        // check multi-value flag
        if (multipleValues != definition.isMultiple()) {
            String msg = (multipleValues) ?
                    "Single-valued property can not be set to an array of values:" :
                    "Multivalued property can not be set to a single value (an array of lenght one is OK): ";
            throw new ValueFormatException(msg + this);
        }

        // check protected flag and for retention/hold      
        int options = ItemValidator.CHECK_CONSTRAINTS;
        session.getValidator().checkModify(this, options, Permission.NONE);
        
        // make sure the parent is checked-out and neither locked nor under retention
        options = ItemValidator.CHECK_VERSIONING | ItemValidator.CHECK_LOCK |
                ItemValidator.CHECK_HOLD | ItemValidator.CHECK_RETENTION;
        session.getValidator().checkModify(parent, options, Permission.NONE);
    }

    /**
     * @param values
     * @param type
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    protected void internalSetValue(InternalValue[] values, int type)
            throws ConstraintViolationException, RepositoryException {
        // check for null value
        if (values == null) {
            // setting a property to null removes it automatically
            ((NodeImpl) getParent()).removeChildProperty(((PropertyId) id).getName());
            return;
        }
        ArrayList list = new ArrayList();
        // compact array (purge null entries)
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                list.add(values[i]);
            }
        }
        values = (InternalValue[]) list.toArray(new InternalValue[list.size()]);

        // modify the state of this property
        PropertyState thisState = (PropertyState) getOrCreateTransientItemState();

        // free old values as necessary
        InternalValue[] oldValues = thisState.getValues();
        if (oldValues != null) {
            for (int i = 0; i < oldValues.length; i++) {
                InternalValue old = oldValues[i];
                if (old != null && old.getType() == PropertyType.BINARY) {
                    // make sure temporarily allocated data is discarded
                    // before overwriting it
                    old.getBLOBFileValue().discard();
                }
            }
        }

        // set new values
        thisState.setValues(values);
        // set type
        if (type == PropertyType.UNDEFINED) {
            // fallback to default type
            type = PropertyType.STRING;
        }
        thisState.setType(type);
    }

    /**
     * Same as <code>{@link Property#setValue(String)}</code> except that
     * this method takes a <code>Name</code> instead of a <code>String</code>
     * value.
     *
     * @param name
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public void setValue(Name name)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        final PropertyDefinition definition = data.getPropertyDefinition();
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.NAME;
        }

        if (name == null) {
            internalSetValue(null, reqType);
            return;
        }

        InternalValue internalValue;
        if (reqType != PropertyType.NAME) {
            // type conversion required
            Value targetValue = ValueHelper.convert(
                    InternalValue.create(name).toJCRValue(session),
                    reqType, session.getValueFactory());
            internalValue = InternalValue.create(targetValue, session, rep.getDataStore());
        } else {
            // no type conversion required
            internalValue = InternalValue.create(name);
        }

        internalSetValue(new InternalValue[]{internalValue}, reqType);
    }

    /**
     * Same as <code>{@link Property#setValue(String[])}</code> except that
     * this method takes an array of <code>Name</code> instead of
     * <code>String</code> values.
     *
     * @param names
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public void setValue(Name[] names)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(true);

        // check type according to definition of this property
        final PropertyDefinition definition = data.getPropertyDefinition();
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.NAME;
        }

        InternalValue[] internalValues = null;
        // convert to internal values of correct type
        if (names != null) {
            internalValues = new InternalValue[names.length];
            for (int i = 0; i < names.length; i++) {
                Name name = names[i];
                InternalValue internalValue = null;
                if (name != null) {
                    if (reqType != PropertyType.NAME) {
                        // type conversion required
                        Value targetValue = ValueHelper.convert(
                                InternalValue.create(name).toJCRValue(session),
                                reqType, session.getValueFactory());
                        internalValue = InternalValue.create(targetValue, session, rep.getDataStore());
                    } else {
                        // no type conversion required
                        internalValue = InternalValue.create(name);
                    }
                }
                internalValues[i] = internalValue;
            }
        }

        internalSetValue(internalValues, reqType);
    }

    /**
     * {@inheritDoc}
     */
    public Name getQName() {
        return ((PropertyId) id).getName();
    }

    /**
     * Returns the internal values of a multi-valued property.
     *
     * @return array of values
     * @throws ValueFormatException if this property is not multi-valued
     * @throws RepositoryException
     */
    public InternalValue[] internalGetValues() throws RepositoryException {
        final PropertyDefinition definition = data.getPropertyDefinition();
        if (definition.isMultiple()) {
            return getPropertyState().getValues();
        } else {
            throw new ValueFormatException(
                    this + " is a single-valued property,"
                    + " so it's value can not be retrieved as an array");
        }

    }

    /**
     * Returns the internal value of a single-valued property.
     *
     * @return value
     * @throws ValueFormatException if this property is not single-valued
     * @throws RepositoryException
     */
    public InternalValue internalGetValue() throws RepositoryException {
        final PropertyDefinition definition = data.getPropertyDefinition();
        if (definition.isMultiple()) {
            throw new ValueFormatException(
                    this + " is a multi-valued property,"
                    + " so it's values can only be retrieved as an array");
        } else {
            InternalValue[] values = getPropertyState().getValues();
            if (values.length > 0) {
                return values[0];
            } else {
                // should never be the case, but being a little paranoid can't hurt...
                throw new RepositoryException(this + ": single-valued property with no value");
            }
        }
    }

    //-------------------------------------------------------------< Property >

    public Value[] getValues() throws RepositoryException {
        InternalValue[] internals = internalGetValues();
        Value[] values = new Value[internals.length];
        for (int i = 0; i < internals.length; i++) {
            values[i] = internals[i].toJCRValue(session);
        }
        return values;
    }

    public Value getValue() throws RepositoryException {
        try {
            return internalGetValue().toJCRValue(session);
        } catch (RuntimeException e) {
            String msg = "Internal error while retrieving value of " + this;
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    public String getString() throws RepositoryException {
        return getValue().getString();
    }

    public InputStream getStream() throws RepositoryException {
        return getValue().getStream();
    }

    public long getLong() throws RepositoryException {
        return getValue().getLong();
    }

    public double getDouble() throws RepositoryException {
        return getValue().getDouble();
    }

    public Calendar getDate() throws RepositoryException {
        return getValue().getDate();
    }

    public boolean getBoolean() throws RepositoryException {
        return getValue().getBoolean();
    }

    public Node getNode() throws ValueFormatException, RepositoryException {
        Value value = getValue();
        if (value.getType() == PropertyType.REFERENCE) {
            return session.getNodeByUUID(value.getString());
        } else {
            // TODO: The specification suggests using value conversion
            throw new ValueFormatException("property must be of type REFERENCE");
        }
    }

    public void setValue(Calendar value) throws RepositoryException {
        if (value != null) {
            try {
                setValue(session.getValueFactory().createValue(value));
            } catch (IllegalArgumentException e) {
                throw new ValueFormatException(e.getMessage());
            }
        } else {
            remove();
        }
    }

    public void setValue(double value) throws RepositoryException {
        setValue(session.getValueFactory().createValue(value));
    }

    public void setValue(InputStream value) throws RepositoryException {
        if (value != null) {
            setValue(session.getValueFactory().createValue(value));
        } else {
            remove();
        }
    }

    public void setValue(String value) throws RepositoryException {
        if (value != null) {
            setValue(session.getValueFactory().createValue(value));
        } else {
            remove();
        }
    }

    public void setValue(String[] strings) throws RepositoryException {
        if (strings != null) {
            ValueFactory factory = session.getValueFactory();
            Value[] values = new Value[strings.length];
            for (int i = 0; i < strings.length; i++) {
                if (strings[i] != null) {
                    values[i] = factory.createValue(strings[i]);
                }
            }
            setValue(values);
        } else {
            remove();
        }
    }

    public void setValue(boolean value) throws RepositoryException {
        setValue(session.getValueFactory().createValue(value));
    }

    public void setValue(Node target)
            throws ValueFormatException, RepositoryException {
        if (target == null) {
            remove();
        } else if (((NodeImpl) target).isNodeType(NameConstants.MIX_REFERENCEABLE)) {
            setValue(session.getValueFactory().createValue(
                    target.getUUID(), PropertyType.REFERENCE));
        } else {
            throw new ValueFormatException(
                    "target node must be of node type mix:referenceable");
        }
    }

    public void setValue(long value) throws RepositoryException {
        setValue(session.getValueFactory().createValue(value));
    }

    public synchronized void setValue(Value value)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        final PropertyDefinition definition = data.getPropertyDefinition();
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            if (value != null) {
                reqType = value.getType();
            } else {
                reqType = PropertyType.STRING;
            }
        }

        if (value == null) {
            internalSetValue(null, reqType);
            return;
        }

        InternalValue internalValue;
        if (reqType != value.getType()) {
            // type conversion required
            Value targetVal = ValueHelper.convert(
                    value, reqType, session.getValueFactory());
            internalValue = InternalValue.create(targetVal, session, rep.getDataStore());
        } else {
            // no type conversion required
            internalValue = InternalValue.create(value, session, rep.getDataStore());
        }
        internalSetValue(new InternalValue[]{internalValue}, reqType);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Value[] values)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        setValue(values, PropertyType.UNDEFINED);
    }

    /**
     * Sets the values of this property.
     *
     * @param values property values (possibly <code>null</code>)
     * @param valueType default value type if not set in the node type,
     *                  may be {@link PropertyType#UNDEFINED}
     * @throws RepositoryException if the property values could not be set
     */
    public void setValue(Value[] values, int valueType)
            throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(true);

        if (values != null) {
            // check type of values
            int firstValueType = PropertyType.UNDEFINED;
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    if (firstValueType == PropertyType.UNDEFINED) {
                        firstValueType = values[i].getType();
                    } else if (firstValueType != values[i].getType()) {
                        throw new ValueFormatException(
                                "inhomogeneous type of values");
                    }
                }
            }
        }

        final PropertyDefinition definition = data.getPropertyDefinition();
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = valueType; // use the given type as property type
        }

        InternalValue[] internalValues = null;
        // convert to internal values of correct type
        if (values != null) {
            internalValues = new InternalValue[values.length];

            // check type of values
            for (int i = 0; i < values.length; i++) {
                Value value = values[i];
                if (value != null) {
                    if (reqType == PropertyType.UNDEFINED) {
                        // Use the type of the fist value as the type
                        reqType = value.getType();
                    }
                    if (reqType != value.getType()) {
                        value = ValueHelper.convert(
                                value, reqType, session.getValueFactory());
                    }
                    internalValues[i] = InternalValue.create(
                            value, session, rep.getDataStore());
                } else {
                    internalValues[i] = null;
                }
            }
        }

        internalSetValue(internalValues, reqType);
    }

    public long getLength() throws RepositoryException {
        return getLength(internalGetValue());
    }

    public long[] getLengths() throws RepositoryException {
        InternalValue[] values = internalGetValues();
        long[] lengths = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            lengths[i] = getLength(values[i]);
        }
        return lengths;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyDefinition getDefinition() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return data.getPropertyDefinition();
    }

    public int getType() throws RepositoryException {
        return getPropertyState().getType();
    }

    //-----------------------------------------------------------------< Item >
    /**
     * {@inheritDoc}
     */
    public boolean isNode() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() throws RepositoryException {
        // check state of this instance
        sanityCheck();
        return session.getJCRName(((PropertyId) id).getName());
    }

    /**
     * {@inheritDoc}
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     */
    public Node getParent() throws RepositoryException {
        return (Node) itemMgr.getItem(getPropertyState().getParentId());
    }

    //--------------------------------------------------------------< Object >

    /**
     * Return a string representation of this property for diagnostic purposes.
     *
     * @return "property /path/to/item"
     */
    public String toString() {
        return "property " + super.toString();
    }

}
