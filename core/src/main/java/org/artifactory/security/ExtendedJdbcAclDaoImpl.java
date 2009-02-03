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
package org.artifactory.security;

import org.acegisecurity.acl.basic.AclObjectIdentity;
import org.acegisecurity.acl.basic.SimpleAclEntry;
import org.acegisecurity.acl.basic.jdbc.JdbcExtendedDaoImpl;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContextException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;
import org.springframework.jdbc.object.SqlUpdate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ExtendedJdbcAclDaoImpl extends JdbcExtendedDaoImpl implements ExtendedJdbcAclDao {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ExtendedJdbcAclDaoImpl.class);

    public static final String ALL_IDENTITIES_STATEMENT =
            "SELECT CHILD.ID, CHILD.OBJECT_IDENTITY, CHILD.ACL_CLASS, PARENT.OBJECT_IDENTITY " +
                    "as PARENT_OBJECT_IDENTITY FROM acl_object_identity as CHILD " +
                    "LEFT OUTER JOIN acl_object_identity as PARENT " +
                    "ON CHILD.parent_object=PARENT.id";
    public static final String ACLS_BY_IDENTITY_AND_RECIPIENT_STATEMENT =
            "SELECT acl_object_identity.id from acl_object_identity, acl_permission " +
                    "WHERE acl_object_identity.object_identity = ? " +
                    "AND acl_object_identity.id = acl_permission.acl_object_identity";
    public static final String DELETE_ACLS_BY_IDENTITY_STATEMENT =
            "DELETE FROM acl_permission WHERE acl_object_identity in " +
                    "(SELECT acl_object_identity.id from acl_object_identity, acl_permission " +
                    "WHERE acl_object_identity.object_identity = ? " +
                    "AND acl_object_identity.id = acl_permission.acl_object_identity)";
    public static final String DELETE_ACLS_BY_RECIPIENT_STATEMENT =
            "DELETE FROM acl_permission WHERE recipient = ?";

    protected SqlUpdate deleteAclsByIdentity;
    protected SqlUpdate deleteAclsByRecipient;


    public void createAclObjectIdentity(RepoPath aclObjectIdentity,
            RepoPath aclParentObjectIdentity) {
        //Note: there is a basicAclEntryCache in the super we cannot touch but we can assume it has
        //no records for a new oi
        String aclObjectIdentityString = convertAclObjectIdentityToString(aclObjectIdentity);

        //Lookup the object's main properties from the RDBMS (guaranteed no nulls)
        List objects = objectProperties.execute(aclObjectIdentityString);

        if (objects.size() > 0) {
            throw new DataIntegrityViolationException(
                    "ObjIdentity '" + aclObjectIdentityString + "' already exits in the database.");
        }
        JdbcExtendedDaoImpl.AclObjectIdentityInsert insert = getAclObjectIdentityInsert();
        if (aclParentObjectIdentity != null) {
            AclDetailsHolder parentDetails = lookupAclDetailsHolder(aclParentObjectIdentity);
            //Must create the acl_object_identity record with a parent
            insert.update(new Object[]{aclObjectIdentityString, parentDetails.getForeignKeyId(),
                    SimpleAclEntry.class.getName()});
        } else {
            //Must create the acl_object_identity record
            insert.update(new Object[]{aclObjectIdentityString, null,
                    SimpleAclEntry.class.getName()});
        }
    }

    @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
    public List<RepoPath> getAllRepoPaths() {
        GetAllIdentitiesMapping query = new GetAllIdentitiesMapping(getDataSource());
        List<AclDetailsHolder> list = query.execute();
        List<RepoPath> repoPathLists = new ArrayList<RepoPath>(list.size());
        for (AclDetailsHolder data : list) {
            RepoPath objectIdentity = (RepoPath) data.getAclObjectIdentity();
            repoPathLists.add(objectIdentity);
        }
        return repoPathLists;
    }

    public void deleteAcls(String recipient) {
        deleteAclsByRecipient.update(recipient);
    }

    public void deleteAcls(RepoPath identity) {
        String id = convertAclObjectIdentityToString(identity);
        deleteAclsByIdentity.update(id);
    }

    public void update(SimpleAclEntry aclEntry) {
        delete(aclEntry.getAclObjectIdentity(), aclEntry.getRecipient());
        create(aclEntry);
    }

    @Override
    protected void initDao() throws ApplicationContextException {
        super.initDao();
        deleteAclsByIdentity = new SqlUpdate(getDataSource(), DELETE_ACLS_BY_IDENTITY_STATEMENT,
                new int[]{Types.VARCHAR});
        deleteAclsByRecipient = new SqlUpdate(getDataSource(), DELETE_ACLS_BY_RECIPIENT_STATEMENT,
                new int[]{Types.VARCHAR});
    }

    @Override
    protected void initMappingSqlQueries() {
        super.initMappingSqlQueries();
        //Override the objectProperties (get identities) query
        setObjectProperties(new DomainObjectPropertiesMapping(getDataSource()));
    }

    protected AclDetailsHolder lookupAclDetailsHolder(RepoPath aclObjectIdentity)
            throws DataRetrievalFailureException {
        String aclObjectIdentityString = convertAclObjectIdentityToString(aclObjectIdentity);
        List objects = objectProperties.execute(aclObjectIdentityString);
        //Lookup the object's main properties from the RDBMS (guaranteed no nulls)
        if (objects.size() == 0) {
            throw new DataRetrievalFailureException("ObjIdentity not found '" +
                    aclObjectIdentityString + "'.");
        }
        //Should only be one record
        return (AclDetailsHolder) objects.get(0);
    }

    @Override
    protected String convertAclObjectIdentityToString(AclObjectIdentity aclObjectIdentity) {
        //Ensure we can process this type of AclObjectIdentity
        Assert.isInstanceOf(RepoPath.class, aclObjectIdentity,
                "Only aclObjectIdentity of type RepoAndGroupId supported (was passed: " +
                        aclObjectIdentity + ")");
        RepoPath oi = (RepoPath) aclObjectIdentity;
        //Compose the String we expect to find in the RDBMS
        return oi.getId();
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private static AclObjectIdentity buildIdentity(String id) {
        if (id == null) {
            // Must be an empty parent, so return null
            return null;
        }
        RepoPath repoPath = new RepoPath(id);
        return repoPath;
    }

    protected class GetAllIdentitiesMapping extends MappingSqlQuery {
        protected GetAllIdentitiesMapping(DataSource ds) {
            super(ds, ALL_IDENTITIES_STATEMENT);
            compile();
        }

        protected Object mapRow(ResultSet rs, int rownum)
                throws SQLException {
            long id = rs.getLong(1);// required
            String objectIdentity = rs.getString(2);// required
            String aclClass = rs.getString(3);// required
            String parentObjectIdentity = rs.getString(4);// optional
            Assert.hasText(objectIdentity,
                    "required DEF_OBJECT_PROPERTIES_QUERY value (objectIdentity) returned null or empty");
            Assert.hasText(aclClass,
                    "required DEF_OBJECT_PROPERTIES_QUERY value (aclClass) returned null or empty");

            Class aclClazz;

            try {
                aclClazz = this.getClass().getClassLoader().loadClass(aclClass);
            } catch (ClassNotFoundException cnf) {
                throw new IllegalArgumentException(cnf.getMessage());
            }

            return new AclDetailsHolder(id, buildIdentity(objectIdentity),
                    buildIdentity(parentObjectIdentity), aclClazz);
        }
    }

    /**
     * Query object to look up properties for an object identity.<P>Returns the generic
     * <code>AclDetailsHolder</code> object.</p> <P>Guarantees to never return <code>null</code>
     * (exceptions are thrown in the event of any issues).</p> <P>The executed SQL requires the
     * following information be made available from the indicated placeholders: 1. ID, 2.
     * OBJECT_IDENTITY, 3. ACL_CLASS and 4. PARENT_OBJECT_IDENTITY.</p>
     */
    protected class DomainObjectPropertiesMapping extends MappingSqlQuery {
        protected DomainObjectPropertiesMapping(DataSource ds) {
            super(ds, getObjectPropertiesQuery());
            declareParameter(new SqlParameter(Types.VARCHAR));
            compile();
        }

        protected Object mapRow(ResultSet rs, int rownum)
                throws SQLException {
            long id = rs.getLong(1);// required
            String objectIdentity = rs.getString(2);// required
            String aclClass = rs.getString(3);// required
            String parentObjectIdentity = rs.getString(4);// optional
            Assert.hasText(objectIdentity,
                    "required DEF_OBJECT_PROPERTIES_QUERY value (objectIdentity) returned null " +
                            "or empty");
            Assert.hasText(aclClass,
                    "required DEF_OBJECT_PROPERTIES_QUERY value (aclClass) returned null or empty");
            Class aclClazz;
            try {
                aclClazz = this.getClass().getClassLoader().loadClass(aclClass);
            } catch (ClassNotFoundException cnf) {
                throw new IllegalArgumentException(cnf.getMessage());
            }
            return new AclDetailsHolder(id, buildIdentity(objectIdentity),
                    buildIdentity(parentObjectIdentity), aclClazz);
        }
    }
}
