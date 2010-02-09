package org.artifactory.security;

import org.acegisecurity.acl.basic.AclObjectIdentity;
import org.acegisecurity.acl.basic.NamedEntityObjectIdentity;
import org.acegisecurity.acl.basic.SimpleAclEntry;
import org.acegisecurity.acl.basic.jdbc.JdbcExtendedDaoImpl;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContextException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
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
    public static final String DELETE_ACLS_BY_IDENTITY_STATEMENT =
            "DELETE FROM acl_permission WHERE acl_object_identity in " +
                    "(SELECT acl_object_identity.id from acl_object_identity, acl_permission " +
                    "WHERE acl_object_identity.object_identity = ? " +
                    "AND acl_object_identity.id = acl_permission.acl_object_identity)";
    public static final String DELETE_ACLS_BY_RECIPIENT_STATEMENT =
            "DELETE FROM acl_permission WHERE recipient = ?";

    protected SqlUpdate deleteAclsByIdentity;
    protected SqlUpdate deleteAclsByRecipient;


    public void createAclObjectIdentity(StringObjectIdentity aclObjectIdentity,
            StringObjectIdentity aclParentObjectIdentity) {
        //Note: there is a basicAclEntryCache in the super we cannot touch but we can assume it has
        //no records for a new oi
        String aclObjectIdentityString = convertAclObjectIdentityToString(aclObjectIdentity);

        // Lookup the object's main properties from the RDBMS (guaranteed no nulls)
        List objects = objectProperties.execute(aclObjectIdentityString);

        if (objects.size() > 0) {
            throw new DataIntegrityViolationException(
                    "ObjIdentity '" + aclObjectIdentityString + "' already exits in the database.");
        }
        JdbcExtendedDaoImpl.AclObjectIdentityInsert insert = getAclObjectIdentityInsert();
        if (aclParentObjectIdentity != null) {
            AclDetailsHolder parentDetails = lookupAclDetailsHolder(aclParentObjectIdentity);
            // Must create the acl_object_identity record with a parent
            insert.update(new Object[]{aclObjectIdentityString, parentDetails.getForeignKeyId(),
                    SimpleAclEntry.class.getName()});
        } else {
            // Must create the acl_object_identity record
            insert.update(new Object[]{aclObjectIdentityString, null,
                    SimpleAclEntry.class.getName()});
        }
    }

    @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
    public List<String> getAllGroups() {
        ObjectPropertiesMapping query = new ObjectPropertiesMapping(getDataSource());
        List<AclDetailsHolder> list = query.execute();
        List<String> groups = new ArrayList<String>(list.size());
        for (AclDetailsHolder data : list) {
            NamedEntityObjectIdentity objectIdentity =
                    (NamedEntityObjectIdentity) data.getAclObjectIdentity();
            String id = objectIdentity.getId();
            groups.add(id);
        }
        return groups;
    }

    public void deleteAcls(String recipient) {
        deleteAclsByRecipient.update(recipient);
    }

    public void deleteAcls(StringObjectIdentity identity) {
        String id = convertAclObjectIdentityToString(identity);
        deleteAclsByIdentity.update(id);
    }

    @Override
    protected void initDao() throws ApplicationContextException {
        super.initDao();
        deleteAclsByIdentity = new SqlUpdate(getDataSource(), DELETE_ACLS_BY_IDENTITY_STATEMENT,
                new int[]{Types.VARCHAR});
        deleteAclsByRecipient = new SqlUpdate(getDataSource(), DELETE_ACLS_BY_RECIPIENT_STATEMENT,
                new int[]{Types.VARCHAR});
    }

    protected AclDetailsHolder lookupAclDetailsHolder(StringObjectIdentity aclObjectIdentity)
            throws DataRetrievalFailureException {
        String aclObjectIdentityString = convertAclObjectIdentityToString(aclObjectIdentity);

        // Lookup the object's main properties from the RDBMS (guaranteed no nulls)
        List objects = objectProperties.execute(aclObjectIdentityString);

        if (objects.size() == 0) {
            throw new DataRetrievalFailureException("ObjIdentity not found '" +
                    aclObjectIdentityString + "'.");
        }

        // Should only be one record
        return (AclDetailsHolder) objects.get(0);
    }

    protected class ObjectPropertiesMapping extends MappingSqlQuery {
        protected ObjectPropertiesMapping(DataSource ds) {
            super(ds, ALL_IDENTITIES_STATEMENT);
            compile();
        }

        private AclObjectIdentity buildIdentity(String identity) {
            if (identity == null) {
                // Must be an empty parent, so return null
                return null;
            }

            int delim = identity.lastIndexOf(":");
            String classname = identity.substring(0, delim);
            String id = identity.substring(delim + 1);

            return new NamedEntityObjectIdentity(classname, id);
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
}
