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

import edu.uci.ics.jung.graph.filters.EdgePredicateFilter;
import org.jfrog.maven.viewer.common.Scope;

/**
 * User: Dror Bereznitsky
 * Date: 31/03/2007
 * Time: 01:27:55
 */
public class ScopeEdgePredicateFilter extends EdgePredicateFilter {
    private final Scope scope;

    public ScopeEdgePredicateFilter(String scope) {
        super(new ScopeEdgePredicate(scope));
        this.scope = Scope.valueOf(scope.toUpperCase());
    }

    public Scope getScope() {
        return scope;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScopeEdgePredicateFilter) {
            return scope.equals(((ScopeEdgePredicateFilter) obj).getScope());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
    	return scope.ordinal();
    }
}
