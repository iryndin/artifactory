package org.artifactory.test.internal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ArtifactoryProperties;
import org.artifactory.common.ConstantsValue;
import org.artifactory.jcr.schedule.WorkingCopyCommitter;
import org.artifactory.repo.index.IndexerJob;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.ReloadableBean;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashSet;
import java.util.List;

/**
 * Tests to make sure that all the reloadable beans are initialized and destroyed in the proper order. Also tests the
 * proper deletion of indexer temp files
 *
 * @author Noam Tenne
 */
public class TestInitOrderAndIndexer extends ArtifactoryTestBase {

    /**
     * Map of beans already checked
     */
    HashSet<ReloadableBean> checkedBeanMap = new HashSet<ReloadableBean>();

    @BeforeMethod
    public void setUp() {
        // 30 secs default timeout, to shorten the tests
        ArtifactoryProperties.get()
                .setProperty(ConstantsValue.lockTimeoutSecs.getPropertyName(), "10");
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger(QuartzCommand.class).setLevel(Level.DEBUG);
    }

    @Test
    public void testInitOrder() {
        List<ReloadableBean> orderedBeans = context.getBeans();
        System.out.println("Init beans order");
        int index = 1;
        for (ReloadableBean bean : orderedBeans) {
            System.out.println("Bean " + (index++) + " : " + getInterfaceName(bean));
        }
        for (ReloadableBean currentBean : orderedBeans) {
            checkedBeanMap.add(currentBean);
            Class<? extends ReloadableBean>[] beansToInitFirst = currentBean.initAfter();
            for (Class<? extends ReloadableBean> beanToInitFirst : beansToInitFirst) {
                ReloadableBean bean = context.beanForType(beanToInitFirst);
                Assert.assertTrue(checkedBeanMap.contains(bean));
            }
        }
    }

    @Test
    public void testIndexer() {
        TaskService taskService = context.beanForType(TaskService.class);
        taskService.stopTasks(WorkingCopyCommitter.class, true);
        //Need to setup against momo MockServer where .index will exists
        QuartzTask task = new QuartzTask(IndexerJob.class, "TestIndexer");
        taskService.startTask(task);
        taskService.waitForTaskCompletion(task.getToken());

        File[] tempChildren = ArtifactoryHome.getTmpDir().listFiles();
        boolean foundIndexDirs = indexDirsExists(tempChildren);

        Assert.assertTrue(!foundIndexDirs);
    }

    private boolean indexDirsExists(File[] dirContent) {
        for (File child : dirContent) {
            /**
             * We check that it's a dir because there are other *.tmp files in the the temp dir
             * which we do not handle
             */
            if (child.isDirectory()) {
                String childName = child.getName();
                if (childName.contains("artifactory.index")) {
                    return true;
                }
            }
        }

        return false;
    }

    private String getInterfaceName(ReloadableBean bean) {
        return bean.getClass().getInterfaces()[0].getName();
    }
}
