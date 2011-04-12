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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.lucene.search.Sort;

import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * <code>MatchAllDocsQuery</code> extends the lucene <code>MatchAllDocsQuery</code>
 * and in addition implements {@link JackrabbitQuery}.
 */
public class MatchAllDocsQuery
        extends org.apache.lucene.search.MatchAllDocsQuery
        implements JackrabbitQuery {

    /**
     * {@inheritDoc}
     */
    public QueryHits execute(JackrabbitIndexSearcher searcher,
                             SessionImpl session,
                             Sort sort) throws IOException {
        if (sort.getSort().length == 0) {
            try {
                return new NodeTraversingQueryHits(
                        session.getRootNode(), true);
            } catch (RepositoryException e) {
                throw Util.createIOException(e);
            }
        } else {
            return null;
        }
    }
}
