package org.artifactory.webapp.spring;

import org.apache.log4j.Logger;
import org.artifactory.backup.Backup;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.maven.Maven;
import org.artifactory.repo.CentralConfig;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryWebApplicationContext extends XmlWebApplicationContext
        implements ArtifactoryContext {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER =
            Logger.getLogger(ArtifactoryWebApplicationContext.class);

    public CentralConfig getCentralConfig() {
        return beanForType(CentralConfig.class);
    }

    public SecurityHelper getSecurity() {
        return beanForType(SecurityHelper.class);
    }

    public Maven getMaven() {
        return beanForType(Maven.class);
    }

    public Backup getBackup() {
        return beanForType(Backup.class);
    }

    @SuppressWarnings({"unchecked"})
    public <T> T beanForType(Class<T> type) {
        Iterator iter = getBeansOfType(type).values().iterator();
        if (!iter.hasNext()) {
            throw new RuntimeException("Could not find bean of type '" +type.getName() + "'.");
        }
        return (T) iter.next();
    }

    public JcrHelper getJcr() {
        return getCentralConfig().getJcr();
    }

    @Override
    protected void onRefresh() {
        //Do nothing
    }
}
