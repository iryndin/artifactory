package org.artifactory.security;

import org.acegisecurity.acl.basic.BasicAclExtendedDao;

import javax.sql.DataSource;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface ExtendedJdbcAclDao extends BasicAclExtendedDao {

    DataSource getDataSource();

    void createAclObjectIdentity(StringObjectIdentity aclObjectIdentity,
            StringObjectIdentity aclParentObjectIdentity);

    List<String> getAllGroups();

    void deleteAcls(StringObjectIdentity identity);

    void deleteAcls(String recipient);
}
