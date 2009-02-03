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
package org.jfrog.maven.viewer.ui.model.jung.filter;

import edu.uci.ics.jung.graph.filters.VertexPredicateFilter;

/**
 * Created by IntelliJ IDEA.
 * User: Dror Bereznitsky
 * Date: May 4, 2007
 * Time: 6:30:44 PM
 */
public class GroupIdVertexPredicateFilter extends VertexPredicateFilter {
    private final String groupId;

    public GroupIdVertexPredicateFilter(String groupId) {
        super(new GroupIdVertexPredicate(groupId));
        this.groupId = groupId;
    }

    /*
    * Currently using only one instance of this filter, so all instances are equal
    * and has the same hash code.
    * This is probably a bad coding practice !
     */

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GroupIdVertexPredicateFilter) {
            //return ((GroupIdVertexPredicateFilter)obj).groupId.equals(groupId);
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
