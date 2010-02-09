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
package org.jfrog.maven.viewer.ui.command;

import org.jfrog.maven.viewer.ui.event.FilterEvent;
import org.jfrog.maven.viewer.ui.event.NewGraphEvent;
import org.jfrog.maven.viewer.ui.model.jung.filter.GroupIdVertexPredicateFilter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.richclient.command.ActionCommand;

/**
 * Created by IntelliJ IDEA.
 * User: Dror Bereznitsky
 * Date: May 5, 2007
 * Time: 12:08:23 AM
 */
public class ClearGroupIdFilterCommand extends ActionCommand implements ApplicationContextAware, ApplicationListener {
    private ApplicationContext applicationContext;

    protected void doExecuteCommand() {
        if (applicationContext != null) {
            GroupIdVertexPredicateFilter gpf = new GroupIdVertexPredicateFilter("");
            applicationContext.getParent().publishEvent(new FilterEvent(this, gpf, FilterEvent.FilterActionType.REMOVE));
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof NewGraphEvent) {
            setEnabled(true);
        }
    }
}
