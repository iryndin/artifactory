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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * An <code>EffectiveNodeType</code> represents one or more
 * <code>NodeType</code>s as one 'effective' node type where inheritance
 * is resolved.
 * <p/>
 * Instances of <code>EffectiveNodeType</code> are immutable.
 */
public class EffectiveNodeType implements Cloneable {
    private static Logger log = LoggerFactory.getLogger(EffectiveNodeType.class);

    // list of explicitly aggregated {i.e. merged) node types
    private final TreeSet mergedNodeTypes;
    // list of implicitly aggregated {through inheritance) node types
    private final TreeSet inheritedNodeTypes;
    // list of all either explicitly (through aggregation) or implicitly
    // (through inheritance) included node types.
    private final TreeSet allNodeTypes;
    // map of named item definitions (maps name to list of definitions)
    private final HashMap namedItemDefs;
    // list of unnamed item definitions (i.e. residual definitions)
    private final ArrayList unnamedItemDefs;

    /**
     * private constructor.
     */
    private EffectiveNodeType() {
        mergedNodeTypes = new TreeSet();
        inheritedNodeTypes = new TreeSet();
        allNodeTypes = new TreeSet();
        namedItemDefs = new HashMap();
        unnamedItemDefs = new ArrayList();
    }

    /**
     * Package private factory method.
     * <p/>
     * Creates an effective node type representation of a node type definition.
     * Note that the definitions of all referenced node types must be contained
     * in <code>ntdCache</code>.
     *
     * @param ntd      node type definition
     * @param entCache cache of already-built effective node types
     * @param ntdCache cache of node type definitions, used to resolve dependencies
     * @return an effective node type representation of the given node type definition.
     * @throws NodeTypeConflictException if the node type definition is invalid,
     *                                   e.g. due to ambiguous child definitions.
     * @throws NoSuchNodeTypeException if a node type reference (e.g. a supertype)
     *                                 could not be resolved.
     */
    static EffectiveNodeType create(NodeTypeDef ntd,
                                    EffectiveNodeTypeCache entCache,
                                    Map ntdCache)
            throws NodeTypeConflictException, NoSuchNodeTypeException {
        // create empty effective node type instance
        EffectiveNodeType ent = new EffectiveNodeType();
        Name ntName = ntd.getName();

        // prepare new instance
        ent.mergedNodeTypes.add(ntName);
        ent.allNodeTypes.add(ntName);

        // map of all item definitions (maps id to definition)
        // used to effectively detect ambiguous child definitions where
        // ambiguity is defined in terms of definition identity
        HashMap itemDefIds = new HashMap();

        NodeDef[] cnda = ntd.getChildNodeDefs();
        for (int i = 0; i < cnda.length; i++) {
            // check if child node definition would be ambiguous within
            // this node type definition
            if (itemDefIds.containsKey(cnda[i].getId())) {
                // conflict
                String msg;
                if (cnda[i].definesResidual()) {
                    msg = ntName + " contains ambiguous residual child node definitions";
                } else {
                    msg = ntName + " contains ambiguous definitions for child node named "
                            + cnda[i].getName();
                }
                log.debug(msg);
                throw new NodeTypeConflictException(msg);
            } else {
                itemDefIds.put(cnda[i].getId(), cnda[i]);
            }
            if (cnda[i].definesResidual()) {
                // residual node definition
                ent.unnamedItemDefs.add(cnda[i]);
            } else {
                // named node definition
                Name name = cnda[i].getName();
                List defs = (List) ent.namedItemDefs.get(name);
                if (defs == null) {
                    defs = new ArrayList();
                    ent.namedItemDefs.put(name, defs);
                }
                if (defs.size() > 0) {
                    /**
                     * there already exists at least one definition with that
                     * name; make sure none of them is auto-create
                     */
                    for (int j = 0; j < defs.size(); j++) {
                        ItemDef def = (ItemDef) defs.get(j);
                        if (cnda[i].isAutoCreated() || def.isAutoCreated()) {
                            // conflict
                            String msg = "There are more than one 'auto-create' item definitions for '"
                                    + name + "' in node type '" + ntName + "'";
                            log.debug(msg);
                            throw new NodeTypeConflictException(msg);
                        }
                    }
                }
                defs.add(cnda[i]);
            }
        }
        PropDef[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            // check if property definition would be ambiguous within
            // this node type definition
            if (itemDefIds.containsKey(pda[i].getId())) {
                // conflict
                String msg;
                if (pda[i].definesResidual()) {
                    msg = ntName + " contains ambiguous residual property definitions";
                } else {
                    msg = ntName + " contains ambiguous definitions for property named "
                            + pda[i].getName();
                }
                log.debug(msg);
                throw new NodeTypeConflictException(msg);
            } else {
                itemDefIds.put(pda[i].getId(), pda[i]);
            }
            if (pda[i].definesResidual()) {
                // residual property definition
                ent.unnamedItemDefs.add(pda[i]);
            } else {
                // named property definition
                Name name = pda[i].getName();
                List defs = (List) ent.namedItemDefs.get(name);
                if (defs == null) {
                    defs = new ArrayList();
                    ent.namedItemDefs.put(name, defs);
                }
                if (defs.size() > 0) {
                    /**
                     * there already exists at least one definition with that
                     * name; make sure none of them is auto-create
                     */
                    for (int j = 0; j < defs.size(); j++) {
                        ItemDef def = (ItemDef) defs.get(j);
                        if (pda[i].isAutoCreated() || def.isAutoCreated()) {
                            // conflict
                            String msg = "There are more than one 'auto-create' item definitions for '"
                                    + name + "' in node type '" + ntName + "'";
                            log.debug(msg);
                            throw new NodeTypeConflictException(msg);
                        }
                    }
                }
                defs.add(pda[i]);
            }
        }

        // resolve supertypes recursively
        Name[] supertypes = ntd.getSupertypes();
        if (supertypes.length > 0) {
            EffectiveNodeType base =
                    NodeTypeRegistry.getEffectiveNodeType(supertypes, entCache, ntdCache);
            ent.internalMerge(base, true);
        }

        // we're done
        return ent;
    }

    /**
     * Package private factory method for creating a new 'empty' effective
     * node type instance.
     *
     * @return an 'empty' effective node type instance.
     */
    static EffectiveNodeType create() {
        return new EffectiveNodeType();
    }

    public Name[] getMergedNodeTypes() {
        return (Name[]) mergedNodeTypes.toArray(new Name[mergedNodeTypes.size()]);
    }

    public Name[] getInheritedNodeTypes() {
        return (Name[]) inheritedNodeTypes.toArray(new Name[inheritedNodeTypes.size()]);
    }

    public Name[] getAllNodeTypes() {
        return (Name[]) allNodeTypes.toArray(new Name[allNodeTypes.size()]);
    }

    public ItemDef[] getAllItemDefs() {
        if (namedItemDefs.size() == 0 && unnamedItemDefs.size() == 0) {
            return ItemDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size() + unnamedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            defs.addAll((List) iter.next());
        }
        defs.addAll(unnamedItemDefs);
        if (defs.size() == 0) {
            return ItemDef.EMPTY_ARRAY;
        }
        return (ItemDef[]) defs.toArray(new ItemDef[defs.size()]);
    }

    public ItemDef[] getNamedItemDefs() {
        if (namedItemDefs.size() == 0) {
            return ItemDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            defs.addAll((List) iter.next());
        }
        if (defs.size() == 0) {
            return ItemDef.EMPTY_ARRAY;
        }
        return (ItemDef[]) defs.toArray(new ItemDef[defs.size()]);
    }

    public ItemDef[] getUnnamedItemDefs() {
        if (unnamedItemDefs.size() == 0) {
            return ItemDef.EMPTY_ARRAY;
        }
        return (ItemDef[]) unnamedItemDefs.toArray(new ItemDef[unnamedItemDefs.size()]);
    }

    public boolean hasNamedItemDef(Name name) {
        return namedItemDefs.containsKey(name);
    }

    public ItemDef[] getNamedItemDefs(Name name) {
        List defs = (List) namedItemDefs.get(name);
        if (defs == null || defs.size() == 0) {
            return ItemDef.EMPTY_ARRAY;
        }
        return (ItemDef[]) defs.toArray(new ItemDef[defs.size()]);
    }

    public NodeDef[] getAllNodeDefs() {
        if (namedItemDefs.size() == 0 && unnamedItemDefs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size() + unnamedItemDefs.size());
        Iterator iter = unnamedItemDefs.iterator();
        while (iter.hasNext()) {
            ItemDef def = (ItemDef) iter.next();
            if (def.definesNode()) {
                defs.add(def);
            }
        }
        iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (def.definesNode()) {
                    defs.add(def);
                }
            }
        }
        if (defs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
        }
        return (NodeDef[]) defs.toArray(new NodeDef[defs.size()]);
    }

    public NodeDef[] getNamedNodeDefs() {
        if (namedItemDefs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (def.definesNode()) {
                    defs.add(def);
                }
            }
        }
        if (defs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
        }
        return (NodeDef[]) defs.toArray(new NodeDef[defs.size()]);
    }

    public NodeDef[] getNamedNodeDefs(Name name) {
        List list = (List) namedItemDefs.get(name);
        if (list == null || list.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(list.size());
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            ItemDef def = (ItemDef) iter.next();
            if (def.definesNode()) {
                defs.add(def);
            }
        }
        if (defs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
        }
        return (NodeDef[]) defs.toArray(new NodeDef[defs.size()]);
    }

    public NodeDef[] getUnnamedNodeDefs() {
        if (unnamedItemDefs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(unnamedItemDefs.size());
        Iterator iter = unnamedItemDefs.iterator();
        while (iter.hasNext()) {
            ItemDef def = (ItemDef) iter.next();
            if (def.definesNode()) {
                defs.add(def);
            }
        }
        if (defs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
        }
        return (NodeDef[]) defs.toArray(new NodeDef[defs.size()]);
    }

    public NodeDef[] getAutoCreateNodeDefs() {
        // since auto-create items must have a name,
        // we're only searching the named item definitions
        if (namedItemDefs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (def.definesNode() && def.isAutoCreated()) {
                    defs.add(def);
                }
            }
        }
        if (defs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
        }
        return (NodeDef[]) defs.toArray(new NodeDef[defs.size()]);
    }

    public PropDef[] getAllPropDefs() {
        if (namedItemDefs.size() == 0 && unnamedItemDefs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size() + unnamedItemDefs.size());
        Iterator iter = unnamedItemDefs.iterator();
        while (iter.hasNext()) {
            ItemDef def = (ItemDef) iter.next();
            if (!def.definesNode()) {
                defs.add(def);
            }
        }
        iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (!def.definesNode()) {
                    defs.add(def);
                }
            }
        }
        if (defs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
        }
        return (PropDef[]) defs.toArray(new PropDef[defs.size()]);
    }

    public PropDef[] getNamedPropDefs() {
        if (namedItemDefs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (!def.definesNode()) {
                    defs.add(def);
                }
            }
        }
        if (defs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
        }
        return (PropDef[]) defs.toArray(new PropDef[defs.size()]);
    }

    public PropDef[] getNamedPropDefs(Name name) {
        List list = (List) namedItemDefs.get(name);
        if (list == null || list.size() == 0) {
            return PropDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(list.size());
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            ItemDef def = (ItemDef) iter.next();
            if (!def.definesNode()) {
                defs.add(def);
            }
        }
        if (defs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
        }
        return (PropDef[]) defs.toArray(new PropDef[defs.size()]);
    }

    public PropDef[] getUnnamedPropDefs() {
        if (unnamedItemDefs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(unnamedItemDefs.size());
        Iterator iter = unnamedItemDefs.iterator();
        while (iter.hasNext()) {
            ItemDef def = (ItemDef) iter.next();
            if (!def.definesNode()) {
                defs.add(def);
            }
        }
        if (defs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
        }
        return (PropDef[]) defs.toArray(new PropDef[defs.size()]);
    }

    public PropDef[] getAutoCreatePropDefs() {
        // since auto-create items must have a name,
        // we're only searching the named item definitions
        if (namedItemDefs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (!def.definesNode() && def.isAutoCreated()) {
                    defs.add(def);
                }
            }
        }
        if (defs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
        }
        return (PropDef[]) defs.toArray(new PropDef[defs.size()]);
    }

    public PropDef[] getMandatoryPropDefs() {
        // since mandatory items must have a name,
        // we're only searching the named item definitions
        if (namedItemDefs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (!def.definesNode() && def.isMandatory()) {
                    defs.add(def);
                }
            }
        }
        if (defs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
        }
        return (PropDef[]) defs.toArray(new PropDef[defs.size()]);
    }

    public NodeDef[] getMandatoryNodeDefs() {
        // since mandatory items must have a name,
        // we're only searching the named item definitions
        if (namedItemDefs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (def.definesNode() && def.isMandatory()) {
                    defs.add(def);
                }
            }
        }
        if (defs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
        }
        return (NodeDef[]) defs.toArray(new NodeDef[defs.size()]);
    }

    /**
     * Determines whether this effective node type representation includes
     * (either through inheritance or aggregation) the given node type.
     *
     * @param nodeTypeName name of node type
     * @return <code>true</code> if the given node type is included, otherwise
     *         <code>false</code>
     */
    public boolean includesNodeType(Name nodeTypeName) {
        return allNodeTypes.contains(nodeTypeName);
    }

    /**
     * Determines whether this effective node type representation includes
     * (either through inheritance or aggregation) all of the given node types.
     *
     * @param nodeTypeNames array of node type names
     * @return <code>true</code> if all of the given node types are included,
     *         otherwise <code>false</code>
     */
    public boolean includesNodeTypes(Name[] nodeTypeNames) {
        return allNodeTypes.containsAll(Arrays.asList(nodeTypeNames));
    }

    /**
     * Tests if the value constraints defined in the property definition
     * <code>pd</code> are satisfied by the the specified <code>values</code>.
     * <p/>
     * Note that the <i>protected</i> flag is not checked. Also note that no
     * type conversions are attempted if the type of the given values does not
     * match the required type as specified in the given definition.
     *
     * @param pd     The definiton of the property
     * @param values An array of <code>InternalValue</code> objects.
     * @throws ConstraintViolationException if the value constraints defined in
     *                                      the property definition are satisfied
     *                                      by the the specified values
     * @throws RepositoryException          if another error occurs
     */
    public static void checkSetPropertyValueConstraints(PropDef pd,
                                                        InternalValue[] values)
            throws ConstraintViolationException, RepositoryException {
        // check multi-value flag
        if (!pd.isMultiple() && values != null && values.length > 1) {
            throw new ConstraintViolationException("the property is not multi-valued");
        }

        ValueConstraint[] constraints = pd.getValueConstraints();
        if (constraints == null || constraints.length == 0) {
            // no constraints to check
            return;
        }
        if (values != null && values.length > 0) {
            // check value constraints on every value
            for (int i = 0; i < values.length; i++) {
                // constraints are OR-ed together
                boolean satisfied = false;
                ConstraintViolationException cve = null;
                for (int j = 0; j < constraints.length; j++) {
                    try {
                        constraints[j].check(values[i]);
                        satisfied = true;
                        break;
                    } catch (ConstraintViolationException e) {
                        cve = e;
                    }
                }
                if (!satisfied) {
                    // re-throw last exception we encountered
                    throw cve;
                }
            }
        }
    }

    /**
     * @param name
     * @throws ConstraintViolationException
     */
    public void checkAddNodeConstraints(Name name)
            throws ConstraintViolationException {
        try {
            getApplicableChildNodeDef(name, null, null);
        } catch (NoSuchNodeTypeException nsnte) {
            String msg = "internal eror: inconsistent node type";
            log.debug(msg);
            throw new ConstraintViolationException(msg, nsnte);
        }
    }

    /**
     * @param name
     * @param nodeTypeName
     * @param ntReg
     * @throws ConstraintViolationException
     * @throws NoSuchNodeTypeException
     */
    public void checkAddNodeConstraints(Name name, Name nodeTypeName,
                                        NodeTypeRegistry ntReg)
            throws ConstraintViolationException, NoSuchNodeTypeException {
        NodeDef nd = getApplicableChildNodeDef(name, nodeTypeName, ntReg);
        if (nd.isProtected()) {
            throw new ConstraintViolationException(name + " is protected");
        }
        if (nd.isAutoCreated()) {
            throw new ConstraintViolationException(name + " is auto-created and can not be manually added");
        }
    }

    /**
     * Returns the applicable child node definition for a child node with the
     * specified name and node type. If there are multiple applicable definitions
     * named definitions will take precedence over residual definitions.
     *
     * @param name
     * @param nodeTypeName
     * @param ntReg
     * @return
     * @throws NoSuchNodeTypeException
     * @throws ConstraintViolationException if no applicable child node definition
     *                                      could be found
     */
    public NodeDef getApplicableChildNodeDef(Name name, Name nodeTypeName,
                                             NodeTypeRegistry ntReg)
            throws NoSuchNodeTypeException, ConstraintViolationException {
        EffectiveNodeType entTarget;
        if (nodeTypeName != null) {
            entTarget = ntReg.getEffectiveNodeType(nodeTypeName);
        } else {
            entTarget = null;
        }

        // try named node definitions first
        ItemDef[] defs = getNamedItemDefs(name);
        for (int i = 0; i < defs.length; i++) {
            if (defs[i].definesNode()) {
                NodeDef nd = (NodeDef) defs[i];
                Name[] types = nd.getRequiredPrimaryTypes();
                // node definition with that name exists
                if (entTarget != null && types != null) {
                    // check 'required primary types' constraint
                    if (entTarget.includesNodeTypes(types)) {
                        // found named node definition
                        return nd;
                    }
                } else if (nd.getDefaultPrimaryType() != null) {
                    // found node definition with default node type
                    return nd;
                }
            }
        }

        // no item with that name defined;
        // try residual node definitions
        NodeDef[] nda = getUnnamedNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            NodeDef nd = nda[i];
            if (entTarget != null && nd.getRequiredPrimaryTypes() != null) {
                // check 'required primary types' constraint
                if (!entTarget.includesNodeTypes(nd.getRequiredPrimaryTypes())) {
                    continue;
                }
                // found residual node definition
                return nd;
            } else {
                // since no node type has been specified for the new node,
                // it must be determined from the default node type;
                if (nd.getDefaultPrimaryType() != null) {
                    // found residual node definition with default node type
                    return nd;
                }
            }
        }

        // no applicable definition found
        throw new ConstraintViolationException("no matching child node definition found for " + name);
    }

    /**
     * Returns the applicable property definition for a property with the
     * specified name, type and multiValued characteristic. If there are
     * multiple applicable definitions the following rules will be applied:
     * <ul>
     * <li>named definitions are preferred to residual definitions</li>
     * <li>definitions with specific required type are preferred to definitions
     * with required type UNDEFINED</li>
     * </ul>
     *
     * @param name
     * @param type
     * @param multiValued
     * @return
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     */
    public PropDef getApplicablePropertyDef(Name name, int type,
                                            boolean multiValued)
            throws ConstraintViolationException {
        // try named property definitions first
        PropDef match =
                getMatchingPropDef(getNamedPropDefs(name), type, multiValued);
        if (match != null) {
            return match;
        }

        // no item with that name defined;
        // try residual property definitions
        match = getMatchingPropDef(getUnnamedPropDefs(), type, multiValued);
        if (match != null) {
            return match;
        }

        // no applicable definition found
        throw new ConstraintViolationException("no matching property definition found for " + name);
    }

    /**
     * Returns the applicable property definition for a property with the
     * specified name and type. The multiValued flag is not taken into account
     * in the selection algorithm. Other than
     * <code>{@link #getApplicablePropertyDef(Name, int, boolean)}</code>
     * this method does not take the multiValued flag into account in the
     * selection algorithm. If there more than one applicable definitions then
     * the following rules are applied:
     * <ul>
     * <li>named definitions are preferred to residual definitions</li>
     * <li>definitions with specific required type are preferred to definitions
     * with required type UNDEFINED</li>
     * <li>single-value definitions are preferred to multiple-value definitions</li>
     * </ul>
     *
     * @param name
     * @param type
     * @return
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     */
    public PropDef getApplicablePropertyDef(Name name, int type)
            throws ConstraintViolationException {
        // try named property definitions first
        PropDef match = getMatchingPropDef(getNamedPropDefs(name), type);
        if (match != null) {
            return match;
        }

        // no item with that name defined;
        // try residual property definitions
        match = getMatchingPropDef(getUnnamedPropDefs(), type);
        if (match != null) {
            return match;
        }

        // no applicable definition found
        throw new ConstraintViolationException("no matching property definition found for " + name);
    }

    private PropDef getMatchingPropDef(PropDef[] defs, int type) {
        PropDef match = null;
        for (int i = 0; i < defs.length; i++) {
            ItemDef def = defs[i];
            if (!def.definesNode()) {
                PropDef pd = (PropDef) def;
                int reqType = pd.getRequiredType();
                // match type
                if (reqType == PropertyType.UNDEFINED
                        || type == PropertyType.UNDEFINED
                        || reqType == type) {
                    if (match == null) {
                        match = pd;
                    } else {
                        // check if this definition is a better match than
                        // the one we've already got
                        if (match.getRequiredType() != pd.getRequiredType()) {
                            if (match.getRequiredType() == PropertyType.UNDEFINED) {
                                // found better match
                                match = pd;
                            }
                        } else {
                            if (match.isMultiple() && !pd.isMultiple()) {
                                // found better match
                                match = pd;
                            }
                        }
                    }
                    if (match.getRequiredType() != PropertyType.UNDEFINED
                            && !match.isMultiple()) {
                        // found best possible match, get outta here
                        return match;
                    }
                }
            }
        }
        return match;
    }

    private PropDef getMatchingPropDef(PropDef[] defs, int type,
                                       boolean multiValued) {
        PropDef match = null;
        for (int i = 0; i < defs.length; i++) {
            ItemDef def = defs[i];
            if (!def.definesNode()) {
                PropDef pd = (PropDef) def;
                int reqType = pd.getRequiredType();
                // match type
                if (reqType == PropertyType.UNDEFINED
                        || type == PropertyType.UNDEFINED
                        || reqType == type) {
                    // match multiValued flag
                    if (multiValued == pd.isMultiple()) {
                        // found match
                        if (pd.getRequiredType() != PropertyType.UNDEFINED) {
                            // found best possible match, get outta here
                            return pd;
                        } else {
                            if (match == null) {
                                match = pd;
                            }
                        }
                    }
                }
            }
        }
        return match;
    }

    /**
     * @param name
     * @throws ConstraintViolationException
     */
    public void checkRemoveItemConstraints(Name name) throws ConstraintViolationException {
        /**
         * as there might be multiple definitions with the same name and we
         * don't know which one is applicable, we check all of them
         */
        ItemDef[] defs = getNamedItemDefs(name);
        if (defs != null) {
            for (int i = 0; i < defs.length; i++) {
                if (defs[i].isMandatory()) {
                    throw new ConstraintViolationException("can't remove mandatory item");
                }
                if (defs[i].isProtected()) {
                    throw new ConstraintViolationException("can't remove protected item");
                }
            }
        }
    }

    /**
     * @param name
     * @throws ConstraintViolationException
     */
    public void checkRemoveNodeConstraints(Name name) throws ConstraintViolationException {
        /**
         * as there might be multiple definitions with the same name and we
         * don't know which one is applicable, we check all of them
         */
        ItemDef[] defs = getNamedNodeDefs(name);
        if (defs != null) {
            for (int i = 0; i < defs.length; i++) {
                if (defs[i].isMandatory()) {
                    throw new ConstraintViolationException("can't remove mandatory node");
                }
                if (defs[i].isProtected()) {
                    throw new ConstraintViolationException("can't remove protected node");
                }
            }
        }
    }

    /**
     * @param name
     * @throws ConstraintViolationException
     */
    public void checkRemovePropertyConstraints(Name name) throws ConstraintViolationException {
        /**
         * as there might be multiple definitions with the same name and we
         * don't know which one is applicable, we check all of them
         */
        ItemDef[] defs = getNamedPropDefs(name);
        if (defs != null) {
            for (int i = 0; i < defs.length; i++) {
                if (defs[i].isMandatory()) {
                    throw new ConstraintViolationException("can't remove mandatory property");
                }
                if (defs[i].isProtected()) {
                    throw new ConstraintViolationException("can't remove protected property");
                }
            }
        }
    }

    /**
     * Merges another <code>EffectiveNodeType</code> with this one.
     * Checks for merge conflicts.
     *
     * @param other
     * @return
     * @throws NodeTypeConflictException
     */
    EffectiveNodeType merge(EffectiveNodeType other)
            throws NodeTypeConflictException {
        // create a clone of this instance and perform the merge on
        // the 'clone' to avoid a potentially inconsistant state
        // of this instance if an exception is thrown during
        // the merge.
        EffectiveNodeType copy = (EffectiveNodeType) clone();
        copy.internalMerge(other, false);
        return copy;
    }

    /**
     * Internal helper method which merges another <code>EffectiveNodeType</code>
     * instance with <i>this</i> instance.
     * <p/>
     * Warning: This instance might be in an inconsistent state if an exception
     * is thrown.
     *
     * @param other
     * @param supertype true if the merge is a result of inheritance, i.e. <code>other</code>
     *                  represents one or more supertypes of this instance; otherwise false, i.e.
     *                  the merge is the result of an explicit aggregation
     * @throws NodeTypeConflictException
     */
    private synchronized void internalMerge(EffectiveNodeType other, boolean supertype)
            throws NodeTypeConflictException {
        Name[] nta = other.getAllNodeTypes();
        int includedCount = 0;
        for (int i = 0; i < nta.length; i++) {
            if (includesNodeType(nta[i])) {
                // redundant node type
                log.debug("node type '" + nta[i] + "' is already contained.");
                includedCount++;
            }
        }
        if (includedCount == nta.length) {
            // total overlap, ignore
            return;
        }

        // named item definitions
        ItemDef[] defs = other.getNamedItemDefs();
        for (int i = 0; i < defs.length; i++) {
            ItemDef def = defs[i];
            if (includesNodeType(def.getDeclaringNodeType())) {
                // ignore redundant definitions
                continue;
            }
            Name name = def.getName();
            List existingDefs = (List) namedItemDefs.get(name);
            if (existingDefs != null) {
                if (existingDefs.size() > 0) {
                    // there already exists at least one definition with that name
                    for (int j = 0; j < existingDefs.size(); j++) {
                        ItemDef existingDef = (ItemDef) existingDefs.get(j);
                        // make sure none of them is auto-create
                        if (def.isAutoCreated() || existingDef.isAutoCreated()) {
                            // conflict
                            String msg = "The item definition for '" + name
                                    + "' in node type '"
                                    + def.getDeclaringNodeType()
                                    + "' conflicts with node type '"
                                    + existingDef.getDeclaringNodeType()
                                    + "': name collision with auto-create definition";
                            log.debug(msg);
                            throw new NodeTypeConflictException(msg);
                        }
                        // check ambiguous definitions
                        if (def.definesNode() == existingDef.definesNode()) {
                            if (!def.definesNode()) {
                                // property definition
                                PropDef pd = (PropDef) def;
                                PropDef epd = (PropDef) existingDef;
                                // compare type & multiValued flag
                                if (pd.getRequiredType() == epd.getRequiredType()
                                        && pd.isMultiple() == epd.isMultiple()) {
                                    // conflict
                                    String msg = "The property definition for '"
                                            + name + "' in node type '"
                                            + def.getDeclaringNodeType()
                                            + "' conflicts with node type '"
                                            + existingDef.getDeclaringNodeType()
                                            + "': ambiguous property definition";
                                    log.debug(msg);
                                    throw new NodeTypeConflictException(msg);
                                }
                            } else {
                                // child node definition
                                // conflict
                                String msg = "The child node definition for '"
                                        + name + "' in node type '"
                                        + def.getDeclaringNodeType()
                                        + "' conflicts with node type '"
                                        + existingDef.getDeclaringNodeType()
                                        + "': ambiguous child node definition";
                                log.debug(msg);
                                throw new NodeTypeConflictException(msg);
                            }
                        }
                    }
                }
            } else {
                existingDefs = new ArrayList();
                namedItemDefs.put(name, existingDefs);
            }
            existingDefs.add(def);
        }

        // residual item definitions
        defs = other.getUnnamedItemDefs();
        for (int i = 0; i < defs.length; i++) {
            ItemDef def = defs[i];
            if (includesNodeType(def.getDeclaringNodeType())) {
                // ignore redundant definitions
                continue;
            }
            Iterator iter = unnamedItemDefs.iterator();
            while (iter.hasNext()) {
                ItemDef existing = (ItemDef) iter.next();
                // compare with existing definition
                if (def.definesNode() == existing.definesNode()) {
                    if (!def.definesNode()) {
                        // property definition
                        PropDef pd = (PropDef) def;
                        PropDef epd = (PropDef) existing;
                        // compare type & multiValued flag
                        if (pd.getRequiredType() == epd.getRequiredType()
                                && pd.isMultiple() == epd.isMultiple()) {
                            // conflict
                            String msg = "A property definition in node type '"
                                    + def.getDeclaringNodeType()
                                    + "' conflicts with node type '"
                                    + existing.getDeclaringNodeType()
                                    + "': ambiguous residual property definition";
                            log.debug(msg);
                            throw new NodeTypeConflictException(msg);
                        }
                    } else {
                        // child node definition
                        NodeDef nd = (NodeDef) def;
                        NodeDef end = (NodeDef) existing;
                        // compare required & default primary types
                        if (Arrays.equals(nd.getRequiredPrimaryTypes(), end.getRequiredPrimaryTypes())
                                && (nd.getDefaultPrimaryType() == null
                                ? end.getDefaultPrimaryType() == null
                                : nd.getDefaultPrimaryType().equals(end.getDefaultPrimaryType()))) {
                            // conflict
                            String msg = "A child node definition in node type '"
                                    + def.getDeclaringNodeType()
                                    + "' conflicts with node type '"
                                    + existing.getDeclaringNodeType()
                                    + "': ambiguous residual child node definition";
                            log.debug(msg);
                            throw new NodeTypeConflictException(msg);
                        }
                    }
                }
            }
            unnamedItemDefs.add(def);
        }
        for (int i = 0; i < nta.length; i++) {
            allNodeTypes.add(nta[i]);
        }

        if (supertype) {
            // implicit merge as result of inheritance

            // add other merged node types as supertypes
            nta = other.getMergedNodeTypes();
            for (int i = 0; i < nta.length; i++) {
                inheritedNodeTypes.add(nta[i]);
            }
            // add supertypes of other merged node types as supertypes
            nta = other.getInheritedNodeTypes();
            for (int i = 0; i < nta.length; i++) {
                inheritedNodeTypes.add(nta[i]);
            }
        } else {
            // explicit merge

            // merge with other merged node types
            nta = other.getMergedNodeTypes();
            for (int i = 0; i < nta.length; i++) {
                mergedNodeTypes.add(nta[i]);
            }
            // add supertypes of other merged node types as supertypes
            nta = other.getInheritedNodeTypes();
            for (int i = 0; i < nta.length; i++) {
                inheritedNodeTypes.add(nta[i]);
            }
        }
    }

    protected Object clone() {
        EffectiveNodeType clone = new EffectiveNodeType();

        clone.mergedNodeTypes.addAll(mergedNodeTypes);
        clone.inheritedNodeTypes.addAll(inheritedNodeTypes);
        clone.allNodeTypes.addAll(allNodeTypes);
        Iterator iter = namedItemDefs.keySet().iterator();
        while (iter.hasNext()) {
            Object key = iter.next();
            List list = (List) namedItemDefs.get(key);
            clone.namedItemDefs.put(key, new ArrayList(list));
        }
        clone.unnamedItemDefs.addAll(unnamedItemDefs);

        return clone;
    }
}
