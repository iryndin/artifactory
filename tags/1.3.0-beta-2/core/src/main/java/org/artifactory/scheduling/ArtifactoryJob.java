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
package org.artifactory.scheduling;

import org.apache.log4j.Logger;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ArtifactoryContextThreadBinder;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.jcr.RepositoryException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ArtifactoryJob implements Job {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryJob.class);

    public static ArtifactoryContext getArtifactoryContext() {
        return ArtifactorySchedulerFactoryBean.getSingletonContext();
    }

    public final void execute(final JobExecutionContext context) {
        ArtifactoryContext artifactoryContext = getArtifactoryContext();
        ArtifactoryContextThreadBinder.bind(artifactoryContext);
        JcrWrapper jcr = artifactoryContext.getJcr();
        jcr.setCreateSessionIfNeeded(true);
        JobExecutionException e = jcr.doInSession(new JcrCallback<JobExecutionException>() {
            public JobExecutionException doInJcr(JcrSessionWrapper session)
                    throws RepositoryException {
                try {
                    onExecute(context, session);
                } catch (JobExecutionException e) {
                    session.setRollbackOnly();
                    return e;
                }
                return null;
            }
        });
        if (e != null) {
            String msg = "Job execution '" + context.getJobDetail() + "' failed";
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(msg + ".", e);
            } else {
                String emsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                LOGGER.warn(msg + ": " + emsg);
            }
        }
    }

    protected abstract void onExecute(JobExecutionContext context, JcrSessionWrapper jcrSession)
            throws JobExecutionException;

}
