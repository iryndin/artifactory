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

import org.jfrog.maven.viewer.ui.event.ScopeFilterEvent;
import org.jfrog.maven.viewer.ui.event.NewGraphEvent;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.richclient.command.ToggleCommand;

/**
 * User: Dror Bereznitsky
 * Date: 24/03/2007
 * Time: 00:30:16
 */
public class ShowScopeCommand extends ToggleCommand implements ApplicationContextAware, ApplicationListener {
    private String scope;
    private ApplicationContext applicationContext;

    @Override
    protected boolean onSelection(boolean b) {
        if (applicationContext != null) {
            applicationContext.getParent().publishEvent(new ScopeFilterEvent(this, scope));
        }
        return super.onSelection(b);
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
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
