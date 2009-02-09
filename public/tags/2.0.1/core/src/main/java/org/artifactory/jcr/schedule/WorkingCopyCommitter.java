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
package org.artifactory.jcr.schedule;

import org.artifactory.jcr.JcrService;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class WorkingCopyCommitter extends QuartzCommand {

    public WorkingCopyCommitter() {
    }

    @Override
    protected void onExecute(JobExecutionContext context) throws JobExecutionException {
        InternalArtifactoryContext artifactoryContext = InternalContextHelper.get();
        JcrService jcr = artifactoryContext.getJcrService();
        jcr.commitWorkingCopy(500, this);
    }
}