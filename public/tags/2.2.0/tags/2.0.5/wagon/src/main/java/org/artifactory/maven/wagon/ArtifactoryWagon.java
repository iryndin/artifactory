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
package org.artifactory.maven.wagon;

import org.apache.maven.wagon.providers.http.HttpWagon;
import org.apache.maven.wagon.repository.Repository;

/**
 * A wagon that enables using "extensions" for commons http deployment without having to remove
 * maven's own lightweight http.<br/>
 * In distribution mangement use:<br/>
 * &lt;url&gt;repo://localhost:8080/artifactory/local-repo@repo&lt;/url&gt;<br/>
 * And -
 * <pre>
 * &lt;build&gt;
 *     &lt;extensions&gt;
 *          &lt;extension&gt;
 *              &lt;groupId&gt;org.artifactory.maven.wagon&lt;/groupId&gt;
 *              &lt;artifactId&gt;artifactory-wagon&lt;/artifactId&gt;
 *              &lt;version&gt;1.0&lt;/version&gt;
 *          &lt;/extension&gt;
 *      &lt;/extensions&gt;
 * &lt;/build&gt;
 * </pre>
 */
public class ArtifactoryWagon extends HttpWagon {
    public static final String URL_SCHEME = "repo://";

    private Repository repository;

    @Override
    public Repository getRepository() {
        if (repository == null) {
            repository = super.getRepository();
            String origUrl = repository.getUrl();
            String newUrl = origUrl.replace(URL_SCHEME, "http://");
            repository.setUrl(newUrl);
        }
        return repository;
    }
}
