package org.artifactory.repo.index;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.artifactory.schedule.TaskBase;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.test.internal.ArtifactoryTestBase;
import org.quartz.JobDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SimpleTriggerBean;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Date;

public class IndexerTest extends ArtifactoryTestBase {
    private final static Logger log = LoggerFactory.getLogger(IndexerTest.class);

    @BeforeClass
    public void setLogLevel() {
        //TODO: [by yl] Add a dummy index to the mock server and assert we didn't reindex it
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger(IndexerTest.class).setLevel(Level.DEBUG);
        lc.getLogger(IndexerService.class.getPackage().getName()).setLevel(Level.DEBUG);
    }

    @Test
    public void index() throws Exception {
        log.debug("Starting indexing");
        TaskService taskService = context.beanForType(TaskService.class);
        SimpleTriggerBean trigger = new SimpleTriggerBean();
        trigger.setStartTime(new Date());
        trigger.setRepeatCount(0);
        //trigger.setRepeatInterval(1);
        trigger.setMisfireInstruction(SimpleTriggerBean.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT);
        JobDetail jobDetail = new JobDetail("indexerTestJobDetail", null, IndexerJob.class);
        trigger.setJobDetail(jobDetail);
        TaskBase task = new QuartzTask(trigger);
        taskService.startTask(task);
        taskService.waitForTaskCompletion(task.getToken());
        log.debug("Finished indexing");
    }
}