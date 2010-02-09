package org.artifactory.scheduling;

import org.apache.log4j.Logger;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.utils.SqlUtils;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactorySchedulerFactoryBean extends SchedulerFactoryBean implements
        ApplicationContextAware {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER =
            Logger.getLogger(ArtifactorySchedulerFactoryBean.class);

    /**
     * Ugly signleton for use by quartz only
     */
    private static ArtifactoryContext signleton;

    @Override
    public void setDataSource(DataSource source) {
        super.setDataSource(source);
        //Check if the users table exists - create it if not
        boolean tableExists = SqlUtils.tableExists("qrtz_job_details", source);
        if (!tableExists) {
            SqlUtils.executeResourceScript("sql/quartz.sql", source);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        super.setApplicationContext(applicationContext);
        //Initi the static singleton
        ArtifactorySchedulerFactoryBean.signleton = (ArtifactoryContext) applicationContext;
    }

    @Override
    public void destroy() throws SchedulerException {
        //Clean up the static singleton
        ArtifactorySchedulerFactoryBean.signleton = null;
        super.destroy();
    }

    static ArtifactoryContext getSingleton() {
        return signleton;
    }
}
