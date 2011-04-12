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
package org.apache.jackrabbit.core.security.authorization.acl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractACLTemplate;
import org.apache.jackrabbit.core.security.authorization.AccessControlEntryImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.GlobPattern;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.core.security.principal.UnknownPrincipal;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the
 * {@link org.apache.jackrabbit.api.security.JackrabbitAccessControlList}
 * interface that is detached from the effective access control content.
 * Consequently, any modifications applied to this ACL only take effect, if
 * the policy gets
 * {@link javax.jcr.security.AccessControlManager#setPolicy(String, javax.jcr.security.AccessControlPolicy) reapplied}
 * to the <code>AccessControlManager</code> and the changes are saved.
 */
class ACLTemplate extends AbstractACLTemplate {

    private static final Logger log = LoggerFactory.getLogger(ACLTemplate.class);

    /**
     * List containing the entries of this ACL Template.
     */
    private final List<AccessControlEntry> entries = new ArrayList<AccessControlEntry>();

    /**
     * The principal manager used for validation checks
     */
    private final PrincipalManager principalMgr;

    /**
     * The privilege registry
     */
    private final PrivilegeRegistry privilegeRegistry;

    /**
     * The name resolver
     */
    private final NameResolver resolver;

    /**
     * The id of the access controlled node or <code>null</code> if this
     * ACLTemplate isn't created for an existing access controlled node.
     * Used for the Entry#isLocal(NodeId) call only in order to avoid calls
     * to {@link javax.jcr.Node#getPath()}.
     */
    private final NodeId id;

    /**
     *
     */
    private final String jcrRepGlob;

    /**
     * Construct a new empty {@link ACLTemplate}.
     *
     * @param path path
     * @param privilegeRegistry registry
     * @param valueFactory value factory
     * @param resolver
     * @param principalMgr manager
     * @throws javax.jcr.NamespaceException
     */
    ACLTemplate(String path, PrincipalManager principalMgr, 
                PrivilegeRegistry privilegeRegistry, ValueFactory valueFactory,
                NamePathResolver resolver) throws NamespaceException {
        super(path, valueFactory);
        this.principalMgr = principalMgr;
        this.privilegeRegistry = privilegeRegistry;
        this.resolver = resolver;
        this.id = null;

        jcrRepGlob = resolver.getJCRName(P_GLOB);
    }

    /**
     * Create a {@link ACLTemplate} that is used to edit an existing ACL
     * node.
     *
     * @param aclNode node
     * @param privilegeRegistry registry
     * @throws RepositoryException if an error occurs
     */
    ACLTemplate(NodeImpl aclNode, PrivilegeRegistry privilegeRegistry) throws RepositoryException {
        super((aclNode != null) ? aclNode.getParent().getPath() : null, (aclNode != null) ? aclNode.getSession().getValueFactory() : null);
        if (aclNode == null || !NT_REP_ACL.equals(((NodeTypeImpl)aclNode.getPrimaryNodeType()).getQName())) {
            throw new IllegalArgumentException("Node must be of type 'rep:ACL'");
        }
        SessionImpl sImpl = (SessionImpl) aclNode.getSession();
        principalMgr = sImpl.getPrincipalManager();

        this.privilegeRegistry = privilegeRegistry;
        this.resolver = sImpl;
        this.id = aclNode.getParentId();
        jcrRepGlob = sImpl.getJCRName(P_GLOB);

        // load the entries:
        AccessControlManager acMgr = sImpl.getAccessControlManager();
        NodeIterator itr = aclNode.getNodes();
        while (itr.hasNext()) {
            NodeImpl aceNode = (NodeImpl) itr.nextNode();
            try {
                String principalName = aceNode.getProperty(P_PRINCIPAL_NAME).getString();
                Principal princ = principalMgr.getPrincipal(principalName);
                if (princ == null) {
                    log.debug("Principal with name " + principalName + " unknown to PrincipalManager.");
                    princ = new PrincipalImpl(principalName);
                }

                Value[] privValues = aceNode.getProperty(P_PRIVILEGES).getValues();
                Privilege[] privs = new Privilege[privValues.length];
                for (int i = 0; i < privValues.length; i++) {
                    privs[i] = acMgr.privilegeFromName(privValues[i].getString());
                }

                Map<String,Value> restrictions = null;
                if (aceNode.hasProperty(P_GLOB)) {
                    restrictions = Collections.singletonMap(jcrRepGlob, aceNode.getProperty(P_GLOB).getValue());
                }
                // create a new ACEImpl (omitting validation check)
                Entry ace = createEntry(
                        princ,
                        privs,
                        NT_REP_GRANT_ACE.equals(((NodeTypeImpl) aceNode.getPrimaryNodeType()).getQName()),
                        restrictions);
                // add the entry
                internalAdd(ace);
            } catch (RepositoryException e) {
                log.debug("Failed to build ACE from content.", e.getMessage());
            }
        }
    }

    /**
     * Create a new entry omitting any validation checks.
     *
     * @param principal
     * @param privileges
     * @param isAllow
     * @param restrictions
     * @return A new entry
     */
    Entry createEntry(Principal principal, Privilege[] privileges, boolean isAllow, Map<String,Value> restrictions) throws RepositoryException {
        return new Entry(principal, privileges, isAllow, restrictions);
    }

    Entry createEntry(Entry base, Privilege[] newPrivileges, boolean isAllow) throws RepositoryException {
        return new Entry(base, newPrivileges, isAllow);
    }

    private List<Entry> internalGetEntries(Principal principal) {
        String principalName = principal.getName();
        List<Entry> entriesPerPrincipal = new ArrayList<Entry>(2);
        for (AccessControlEntry entry : entries) {
            if (principalName.equals(entry.getPrincipal().getName())) {
                entriesPerPrincipal.add((Entry) entry);
            }
        }
        return entriesPerPrincipal;
    }

    private synchronized boolean internalAdd(Entry entry) throws RepositoryException {
        Principal principal = entry.getPrincipal();
        List<Entry> entriesPerPrincipal = internalGetEntries(principal);
        if (entriesPerPrincipal.isEmpty()) {
            // simple case: just add the new entry at the end of the list.
            entries.add(entry);
            return true;
        } else {
            if (entriesPerPrincipal.contains(entry)) {
                // the same entry is already contained -> no modification
                return false;
            }
            // check if need to adjust existing entries
            int updateIndex = -1;
            Entry complementEntry = null;

            for (Entry e : entriesPerPrincipal) {
                if (equalRestriction(entry, e)) {
                    if (entry.isAllow() == e.isAllow()) {
                        // need to update an existing entry
                        int existingPrivs = e.getPrivilegeBits();
                        if ((existingPrivs | ~entry.getPrivilegeBits()) == -1) {
                            // all privileges to be granted/denied are already present
                            // in the existing entry -> not modified
                            return false;
                        }

                        // remember the index of the existing entry to be updated later on.
                        updateIndex = entries.indexOf(e);

                        // remove the existing entry and create a new that includes
                        // both the new privileges and the existing ones.
                        entries.remove(e);
                        int mergedBits = e.getPrivilegeBits() | entry.getPrivilegeBits();
                        Privilege[] mergedPrivs = privilegeRegistry.getPrivileges(mergedBits);
                        // omit validation check.
                        entry = createEntry(entry, mergedPrivs, entry.isAllow());
                    } else {
                        complementEntry = e;
                    }
                }
            }

            // make sure, that the complement entry (if existing) does not
            // grant/deny the same privileges -> remove privileges that are now
            // denied/granted.
            if (complementEntry != null) {

                int complPrivs = complementEntry.getPrivilegeBits();
                int resultPrivs = Permission.diff(complPrivs, entry.getPrivilegeBits());

                if (resultPrivs == PrivilegeRegistry.NO_PRIVILEGE) {
                    // remove the complement entry as the new entry covers
                    // all privileges granted by the existing entry.
                    entries.remove(complementEntry);
                    updateIndex--;
                    
                } else if (resultPrivs != complPrivs) {
                    // replace the existing entry having the privileges adjusted
                    int index = entries.indexOf(complementEntry);
                    entries.remove(complementEntry);
                    Entry tmpl = createEntry(entry,
                            privilegeRegistry.getPrivileges(resultPrivs),
                            !entry.isAllow());
                    entries.add(index, tmpl);
                } /* else: does not need to be modified.*/
            }

            // finally update the existing entry or add the new entry passed
            // to this method at the end.
            if (updateIndex < 0) {
                entries.add(entry);
            } else {
                entries.add(updateIndex, entry);
            }
            return true;
        }
    }

    private boolean equalRestriction(Entry entry1, Entry entry2) throws RepositoryException {
        Value v1 = entry1.getRestriction(jcrRepGlob);
        Value v2 = entry2.getRestriction(jcrRepGlob);

        return (v1 == null) ? v2 == null : v1.equals(v2);
    }

    //------------------------------------------------< AbstractACLTemplate >---
    /**
     * @see AbstractACLTemplate#checkValidEntry(java.security.Principal, javax.jcr.security.Privilege[], boolean, java.util.Map) 
     */
    @Override
    protected void checkValidEntry(Principal principal, Privilege[] privileges,
                                 boolean isAllow, Map<String, Value> restrictions)
            throws AccessControlException {
        // validate principal
        if (principal instanceof UnknownPrincipal) {
            log.debug("Consider fallback principal as valid: {}", principal.getName());
        } else if (!principalMgr.hasPrincipal(principal.getName())) {
            throw new AccessControlException("Principal " + principal.getName() + " does not exist.");
        }
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AbstractACLTemplate#getEntries()
     */
    @Override
    protected List<AccessControlEntry> getEntries() {
        return entries;
    }

    //--------------------------------------------------< AccessControlList >---
    /**
     * @see javax.jcr.security.AccessControlList#removeAccessControlEntry(AccessControlEntry)
     */
    public synchronized void removeAccessControlEntry(AccessControlEntry ace)
            throws AccessControlException, RepositoryException {
        if (!(ace instanceof Entry)) {
            throw new AccessControlException("Invalid AccessControlEntry implementation " + ace.getClass().getName() + ".");
        }
        if (entries.contains(ace)) {
            entries.remove(ace);
        } else {
            throw new AccessControlException("AccessControlEntry " + ace + " cannot be removed from ACL defined at " + getPath());
        }
    }

    //----------------------------------------< JackrabbitAccessControlList >---
    /**
     * @see JackrabbitAccessControlList#getRestrictionNames()
     */
    public String[] getRestrictionNames() {
        return new String[] {jcrRepGlob};
    }

    /**
     * @see JackrabbitAccessControlList#getRestrictionType(String)
     */
    public int getRestrictionType(String restrictionName) {
        if (jcrRepGlob.equals(restrictionName) || P_GLOB.toString().equals(restrictionName)) {
            return PropertyType.STRING;
        } else {
            return PropertyType.UNDEFINED;
        }
    }

    /**
     * The only known restriction is:
     * <pre>
     *   rep:glob (optional)  value-type: STRING
     * </pre>
     *
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlList#addEntry(Principal, Privilege[], boolean, Map)
     */
    public boolean addEntry(Principal principal, Privilege[] privileges,
                            boolean isAllow, Map<String, Value> restrictions)
            throws AccessControlException, RepositoryException {
        checkValidEntry(principal, privileges, isAllow, restrictions);
        Entry ace = createEntry(principal, privileges, isAllow, restrictions);
        return internalAdd(ace);
    }

    //-------------------------------------------------------------< Object >---
    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return 0;
    }

    /**
     * Returns true if the path and the entries are equal; false otherwise.
     *
     * @param obj Object to be tested.
     * @return true if the path and the entries are equal; false otherwise.
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof ACLTemplate) {
            ACLTemplate acl = (ACLTemplate) obj;
            return path.equals(acl.path) && entries.equals(acl.entries);
        }
        return false;
    }

    //--------------------------------------------------------------------------
    /**
     *
     */
    class Entry extends AccessControlEntryImpl {

        private final GlobPattern pattern;

        private Entry(Principal principal, Privilege[] privileges, boolean allow, Map<String,Value> restrictions)
                throws RepositoryException {
            super(principal, privileges, allow, restrictions);
            Value glob = getRestrictions().get(P_GLOB);
            if (glob != null) {
                pattern = GlobPattern.create(path, glob.getString());
            } else {
                pattern = GlobPattern.create(path);
            }
        }

        private Entry(Entry base, Privilege[] newPrivileges, boolean isAllow) throws RepositoryException {
            super(base, newPrivileges, isAllow);
            Value glob = getRestrictions().get(P_GLOB);
            if (glob != null) {
                pattern = GlobPattern.create(path, glob.getString());
            } else {
                pattern = GlobPattern.create(path);
            }
        }
        
        /**
         * @param nodeId
         * @return <code>true</code> if this entry is defined on the node
         * at <code>nodeId</code>
         */
        boolean isLocal(NodeId nodeId) {
            return id != null && id.equals(nodeId);
        }

        /**
         * 
         * @param jcrPath
         * @return
         */
        boolean matches(String jcrPath) {
            return pattern.matches(jcrPath);
        }

        @Override
        protected NameResolver getResolver() {
            return resolver;
        }

        @Override
        protected ValueFactory getValueFactory() {
            return valueFactory;
        }
    }
}
