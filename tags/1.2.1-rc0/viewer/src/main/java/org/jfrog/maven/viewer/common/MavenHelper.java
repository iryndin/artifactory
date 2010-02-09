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
package org.jfrog.maven.viewer.common;

import org.apache.maven.model.Repository;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Dror Bereznitsky
 * Date: 21/03/2007
 * Time: 21:59:51
 */
public class MavenHelper {
    public static List<Repository> getAllRepositories(MavenProject project) {
        List<Repository> repositories = new ArrayList<Repository>();
        repositories.addAll(project.getRepositories());
        repositories.addAll(project.getPluginRepositories());
        return repositories;
    }
}
